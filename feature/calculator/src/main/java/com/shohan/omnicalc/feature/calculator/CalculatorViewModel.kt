package com.shohan.omnicalc.feature.calculator

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohan.omnicalc.core.common.AppSettings
import com.shohan.omnicalc.core.database.AppServices
import com.shohan.omnicalc.core.math.CalcResult
import com.shohan.omnicalc.core.math.EvalContext
import com.shohan.omnicalc.core.math.Engine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Persistent UI state for the standard/scientific calculator (spec §5). */
data class CalculatorUiState(
    val field: TextFieldValue = TextFieldValue(""),
    /** Live preview (only shown when evaluation currently succeeds). */
    val preview: String? = null,
    /** Result of the last committed "=" (shown large). */
    val committedResult: String? = null,
    val committedExact: Boolean = true,
    /** Set after "=" on a failed expression — cleared on edit (spec §4.4 UX). */
    val errorMessage: String? = null,
    /** Pinned memory value as plain string, null = memory empty (spec §4.6). */
    val memory: String? = null,
    val angleModeLabel: String = "DEG",
    /** True right after "=" — next digit starts fresh, operator continues. */
    val justEvaluated: Boolean = false,
    /** True when an expression was loaded from history (consumed once). */
    val loadedFromHistory: Boolean = false,
    /** Haptic preference mirror for key handlers (spec §2.4). */
    val hapticsEnabled: Boolean = true,
)

/**
 * Single ViewModel backing BOTH the standard and scientific calculators —
 * the scientific screen only adds keys that insert function syntax.
 *
 * Notable behaviors (spec §5):
 *  - live preview while editing (no artificial delays; the engine is local)
 *  - "=" commits to history (deduplicated against the most recent entry)
 *  - repeated "=" does not spam history; operator after "=" continues from
 *    the result; a digit after "=" starts a new expression
 *  - expression text + cursor survive rotation (VM + SavedStateHandle)
 *  - memory persists across restarts via settings
 */
class CalculatorViewModel(
    private val services: AppServices,
    private val savedState: SavedStateHandle,
    private val errorMapper: (com.shohan.omnicalc.core.math.ErrorCode) -> String,
) : ViewModel() {

    private val _ui = MutableStateFlow(restoreState())
    val ui: StateFlow<CalculatorUiState> = _ui.asStateFlow()

    private var settings: AppSettings = AppSettings()

    init {
        viewModelScope.launch {
            services.settingsRepository.settings.collect { s ->
                settings = s
                _ui.update {
                    it.copy(
                        memory = s.memoryValue,
                        angleModeLabel = s.angleMode.name,
                        hapticsEnabled = s.hapticsEnabled,
                    )
                }
                recomputePreview()
            }
        }
    }

    // ---- input actions ----------------------------------------------------

    /** Inserts text at the caret (replacing any selection). */
    fun insert(text: String) {
        val field = _ui.value.field
        val start = field.selection.min
        val end = field.selection.max
        val newText = field.text.substring(0, start) + text + field.text.substring(end)
        val newCursor = start + text.length

        val afterEquals = _ui.value.justEvaluated
        val startsFresh = afterEquals && text.firstOrNull()?.let { it.isDigit() || it == '(' || it == '.' } == true

        val finalText: String
        val finalCursor: Int
        if (startsFresh) {
            finalText = text
            finalCursor = text.length
        } else if (afterEquals && _ui.value.committedResult != null && text.firstOrNull()?.let { !it.isLetter() && !it.isDigit() } == true) {
            // Operator right after "=" continues from the result.
            finalText = _ui.value.committedResult + text
            finalCursor = finalText.length
        } else {
            finalText = newText
            finalCursor = newCursor
        }

        setField(finalText, finalCursor)
    }

    fun backspace() {
        val field = _ui.value.field
        if (field.selection.start != field.selection.end) {
            val text = field.text.removeRange(field.selection.min, field.selection.max)
            setField(text, field.selection.min)
            return
        }
        val cursor = field.selection.end
        if (cursor == 0) return
        // Delete the whole token (function name) when it ends at the caret —
        // discoverable long-press-free editing for "sin(" etc.
        val startOfToken = findTokenStart(field.text, cursor)
        val text = field.text.removeRange(startOfToken, cursor)
        setField(text, startOfToken)
    }

    private fun findTokenStart(text: String, cursor: Int): Int {
        var i = cursor
        if (i > 0 && text[i - 1] == ')') return i - 1
        if (i > 0 && (text[i - 1] == '(')) {
            // "sin(" → remove name too
            var j = i - 2
            while (j >= 0 && text[j].isLetter()) j--
            return if (j < i - 1) j + 1 else i - 1
        }
        while (i > 0 && (text[i - 1].isLetter() || text[i - 1].isDigit() || text[i - 1] == '.')) {
            i--
        }
        return if (i == cursor) cursor - 1 else i
    }

    fun clearAll() {
        setField("", 0)
        _ui.update { it.copy(committedResult = null, errorMessage = null, justEvaluated = false) }
        persistState("", 0)
    }

    fun clearEntry() {
        // CE: drop the current number token only.
        val field = _ui.value.field
        val cursor = field.selection.end
        val start = findTokenStart(field.text, cursor)
        val text = field.text.removeRange(start, cursor)
        setField(text, start)
    }

    fun toggleSign() {
        val field = _ui.value.field
        val cursor = field.selection.end
        val text = field.text
        // Find the number token containing/ending at the caret.
        var start = cursor
        while (start > 0 && (text[start - 1].isDigit() || text[start - 1] == '.')) start--
        if (start > 0 && text[start - 1] == '-' && (start == 1 || !text[start - 2].isDigit())) {
            // Remove existing unary minus.
            setField(text.removeRange(start - 1, start), cursor - 1)
        } else {
            setField(text.substring(0, start) + "-" + text.substring(start), cursor + 1)
        }
    }

    /** "=" — evaluate, commit to history on success, surface errors otherwise. */
    fun evaluate() {
        val text = _ui.value.field.text
        if (text.isBlank()) {
            _ui.update { it.copy(errorMessage = null, committedResult = null, justEvaluated = false) }
            return
        }
        when (val result = Engine.evaluate(text, evalContext())) {
            is CalcResult.Success -> {
                val formatted = formatResult(result.value, result.exact)
                _ui.update {
                    it.copy(
                        committedResult = formatted,
                        committedExact = result.exact,
                        preview = formatted,
                        errorMessage = null,
                        justEvaluated = true,
                    )
                }
                persistState(text, _ui.value.field.selection.end)
                viewModelScope.launch { recordHistory(text, result) }
            }
            is CalcResult.Failure -> {
                val localized = localizeError(result.error.code)
                _ui.update {
                    it.copy(errorMessage = localized, justEvaluated = false)
                }
            }
        }
    }

    // ---- memory (spec §4.6) ---------------------------------------------

    fun memoryClear() {
        persistMemory(null)
    }

    fun memoryRecall() {
        val mem = settings.memoryValue ?: return
        insertNumberString(mem)
    }

    fun memoryAdd() {
        val current = currentNumericValue() ?: return
        val mem = settings.memoryValue?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
        persistMemory(mem.add(current).toPlainString())
    }

    fun memorySubtract() {
        val current = currentNumericValue() ?: return
        val mem = settings.memoryValue?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
        persistMemory(mem.subtract(current).toPlainString())
    }

    fun memoryStore() {
        val current = currentNumericValue() ?: return
        persistMemory(current.toPlainString())
    }

    private fun persistMemory(value: String?) {
        viewModelScope.launch { services.settingsRepository.setMemoryValue(value) }
        // settings collector updates state.memory
    }

    private fun insertNumberString(value: String) {
        val field = _ui.value.field
        if (_ui.value.justEvaluated) {
            _ui.update {
                it.copy(
                    field = TextFieldValue(value, selection = TextRange(value.length)),
                    justEvaluated = false,
                    errorMessage = null,
                )
            }
            recomputePreview()
            return
        }
        insert(value)
    }

    /** Numeric value of the current expression or committed result, if any. */
    private fun currentNumericValue(): java.math.BigDecimal? {
        val source = _ui.value.committedResult?.takeIf { _ui.value.justEvaluated }
            ?: _ui.value.field.text
        val text = com.shohan.omnicalc.core.common.Numerals.normalizeToWesternDigits(source)
        return text.toBigDecimalOrNull()
            ?: Engine.evaluate(text, evalContext()).let { (it as? CalcResult.Success)?.value }
    }

    // ---- history (spec §4.7, §15.1) --------------------------------------

    private suspend fun recordHistory(expression: String, result: CalcResult.Success) {
        val latest = services.historyRepository.observeRecent(1).first().firstOrNull()
        if (latest != null && latest.expression == expression && latest.resultPlain == result.value.toPlainString()) {
            return // dedup: "=" pressed repeatedly
        }
        services.historyRepository.record(
            expression = expression,
            resultPlain = result.value.toPlainString(),
            exact = result.exact,
            toolType = if (expression.any { it.isLetter() || it == '\u221A' || it == '\u221B' }) "scientific" else "standard",
        )
    }

    /** Cycles DEG → RAD → GRAD → DEG (spec §6.1 angle indicator/toggle). */
    fun cycleAngleMode() {
        val next = when (settings.angleMode) {
            com.shohan.omnicalc.core.common.AngleMode.DEG -> com.shohan.omnicalc.core.common.AngleMode.RAD
            com.shohan.omnicalc.core.common.AngleMode.RAD -> com.shohan.omnicalc.core.common.AngleMode.GRAD
            com.shohan.omnicalc.core.common.AngleMode.GRAD -> com.shohan.omnicalc.core.common.AngleMode.DEG
        }
        viewModelScope.launch { services.settingsRepository.setAngleMode(next) }
    }

    fun reuseExpression(expression: String, result: String? = null) {
        _ui.update {
            it.copy(
                field = TextFieldValue(expression, selection = TextRange(expression.length)),
                committedResult = result,
                errorMessage = null,
                justEvaluated = false,
                loadedFromHistory = true,
            )
        }
        recomputePreview()
        persistState(expression, expression.length)
    }

    // ---- internals --------------------------------------------------------

    private fun evalContext(): EvalContext = EvalContext(
        angleMode = settings.angleMode,
        percentMode = settings.percentMode,
        significantDigits = 15,
    )

    private fun formatResult(value: java.math.BigDecimal, exact: Boolean): String {
        val plain = when (settings.precisionMode) {
            com.shohan.omnicalc.core.common.PrecisionMode.AUTO -> value.stripTrailingZeros().toPlainString()
            com.shohan.omnicalc.core.common.PrecisionMode.FIXED ->
                value.setScale(settings.decimalPlaces, java.math.RoundingMode.HALF_UP).toPlainString()
        }
        val grouped = com.shohan.omnicalc.core.common.NumberGrouping.group(plain, settings.groupingStyle)
        return com.shohan.omnicalc.core.common.Numerals.applyNumeralStyle(grouped, settings.numeralStyle)
    }

    /** Localized error message via the injected string-resource mapper. */
    private fun localizeError(code: com.shohan.omnicalc.core.math.ErrorCode): String =
        errorMapper(code)

    private fun recomputePreview() {
        val text = _ui.value.field.text
        if (text.isBlank()) {
            _ui.update { it.copy(preview = null, errorMessage = null) }
            return
        }
        when (val result = Engine.evaluate(text, evalContext())) {
            is CalcResult.Success ->
                _ui.update { it.copy(preview = formatResult(result.value, result.exact), errorMessage = null) }
            is CalcResult.Failure ->
                _ui.update { it.copy(preview = null) } // errors only surface on "="
        }
    }

    private fun onTextChanged(newValue: TextFieldValue) {
        _ui.update {
            it.copy(field = newValue, justEvaluated = false, errorMessage = null, loadedFromHistory = false)
        }
        recomputePreview()
        persistState(newValue.text, newValue.selection.end)
    }

    private fun setField(text: String, cursor: Int) {
        onTextChanged(TextFieldValue(text, selection = TextRange(cursor.coerceIn(0, text.length))))
    }

    private fun persistState(text: String, cursor: Int) {
        savedState[KEY_EXPRESSION] = text
        savedState[KEY_CURSOR] = cursor
    }

    private fun restoreState(): CalculatorUiState {
        val text = savedState.get<String>(KEY_EXPRESSION) ?: ""
        return CalculatorUiState(
            field = TextFieldValue(text, selection = TextRange(savedState.get<Int>(KEY_CURSOR) ?: text.length)),
        )
    }

    /** Called by the screen on every keystroke from the text field. */
    fun onFieldChange(newValue: TextFieldValue) = onTextChanged(newValue)

    companion object {
        private const val KEY_EXPRESSION = "calc_expression"
        private const val KEY_CURSOR = "calc_cursor"
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                val context = (app as android.app.Application).applicationContext
                val mapper: (com.shohan.omnicalc.core.math.ErrorCode) -> String = { code ->
                    context.getString(
                        com.shohan.omnicalc.core.localization.EngineErrorStrings.messageRes(code),
                    )
                }
                CalculatorViewModel(app, createSavedStateHandle(), mapper)
            }
        }
    }
}
