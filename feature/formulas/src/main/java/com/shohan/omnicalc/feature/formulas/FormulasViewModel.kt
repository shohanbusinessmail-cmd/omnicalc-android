package com.shohan.omnicalc.feature.formulas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohan.omnicalc.core.common.AppSettings
import com.shohan.omnicalc.core.common.DisplayFormatter
import com.shohan.omnicalc.core.common.Numerals
import com.shohan.omnicalc.core.database.AppServices
import com.shohan.omnicalc.core.math.Engine
import com.shohan.omnicalc.core.math.ErrorCode
import com.shohan.omnicalc.core.math.CalcResult
import com.shohan.omnicalc.core.math.EvalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Formula catalog state and actions (spec §9). Computation goes through the
 * shared engine with user-supplied variables — results are never faked, and
 * validated inputs are recorded to history like any other calculation.
 */
class FormulasViewModel(
    private val services: AppServices,
    private val errorMapper: (ErrorCode) -> String,
    private val requiredMessage: String,
    private val numberMessage: String,
) : ViewModel() {

    data class Computed(
        val display: String,
        val plain: String,
        val exact: Boolean,
    )

    data class UiState(
        val query: String = "",
        val category: FormulaCategory? = null,
        val expandedId: String? = null,
        /** formulaId → variable name → raw input text. */
        val inputs: Map<String, Map<String, String>> = emptyMap(),
        val results: Map<String, Computed> = emptyMap(),
        val errors: Map<String, String> = emptyMap(),
        val favoriteIds: Set<String> = emptySet(),
        val settings: AppSettings = AppSettings(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            services.settingsRepository.settings.collect { s ->
                _ui.update { it.copy(settings = s) }
            }
        }
        viewModelScope.launch {
            services.favoritesRepository.observeAll().collect { list ->
                _ui.update {
                    it.copy(favoriteIds = list.filter { f -> f.type == TYPE_FORMULA }.map { f -> f.payload }.toSet())
                }
            }
        }
    }

    fun setQuery(value: String) = _ui.update { it.copy(query = value) }

    fun setCategory(value: FormulaCategory?) = _ui.update { it.copy(category = value) }

    fun expand(id: String?) = _ui.update { it.copy(expandedId = id) }

    fun setInput(formulaId: String, variable: String, raw: String) = _ui.update { state ->
        val perFormula = state.inputs[formulaId].orEmpty() + (variable to raw)
        state.copy(inputs = state.inputs + (formulaId to perFormula))
    }

    fun compute(formula: Formula) {
        val rawInputs = _ui.value.inputs[formula.id].orEmpty()
        val variables = LinkedHashMap<String, BigDecimal>()
        for (v in formula.vars) {
            val raw = rawInputs[v.name]?.trim().orEmpty()
            if (raw.isEmpty()) {
                setError(formula.id, requiredMessage)
                return
            }
            val parsed = try {
                BigDecimal(Numerals.normalizeToWesternDigits(raw))
            } catch (e: NumberFormatException) {
                setError(formula.id, numberMessage)
                return
            }
            variables[v.name] = parsed
        }
        val context = EvalContext(
            angleMode = _ui.value.settings.angleMode,
            percentMode = _ui.value.settings.percentMode,
            variables = variables,
        )
        when (val result = Engine.evaluate(formula.expr, context)) {
            is CalcResult.Success -> {
                val display = DisplayFormatter.format(result.value, _ui.value.settings)
                _ui.update { s ->
                    s.copy(
                        results = s.results + (formula.id to Computed(display, result.value.toPlainString(), result.exact)),
                        errors = s.errors - formula.id,
                    )
                }
                viewModelScope.launch {
                    services.historyRepository.record(
                        expression = formula.expr,
                        resultPlain = result.value.toPlainString(),
                        exact = result.exact,
                        toolType = TYPE_FORMULA,
                        category = formula.category.name.lowercase(),
                        contextPairs = variables.entries.joinToString("&") { "${it.key}=${it.value.toPlainString()}" },
                    )
                }
            }
            is CalcResult.Failure -> setError(formula.id, errorMapper(result.error.code))
        }
    }

    fun toggleFavorite(formula: Formula) {
        viewModelScope.launch {
            if (formula.id in _ui.value.favoriteIds) {
                services.favoritesRepository.removeByPayload(TYPE_FORMULA, formula.id)
            } else {
                services.favoritesRepository.add(
                    type = TYPE_FORMULA,
                    title = formula.nameEn,
                    payload = formula.id,
                    subtitle = formula.expr,
                )
            }
        }
    }

    private fun setError(formulaId: String, message: String) {
        _ui.update { it.copy(errors = it.errors + (formulaId to message), results = it.results - formulaId) }
    }

    companion object {
        const val TYPE_FORMULA = "formula"

        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                val context = (app as android.app.Application).applicationContext
                val mapper: (ErrorCode) -> String = { code ->
                    context.getString(com.shohan.omnicalc.core.localization.EngineErrorStrings.messageRes(code))
                }
                FormulasViewModel(
                    services = app,
                    errorMapper = mapper,
                    requiredMessage = context.getString(com.shohan.omnicalc.core.localization.R.string.formula_error_required),
                    numberMessage = context.getString(com.shohan.omnicalc.core.localization.R.string.formula_error_number),
                )
            }
        }
    }
}
