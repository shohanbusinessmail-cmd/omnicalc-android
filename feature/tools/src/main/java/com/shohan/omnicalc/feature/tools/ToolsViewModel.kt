package com.shohan.omnicalc.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohan.omnicalc.core.common.AppSettings
import com.shohan.omnicalc.core.common.DisplayFormatter
import com.shohan.omnicalc.core.common.Numerals
import com.shohan.omnicalc.core.database.AppServices
import com.shohan.omnicalc.core.math.DateMath
import com.shohan.omnicalc.core.math.UnitCatalog
import com.shohan.omnicalc.core.math.UnitCategory
import com.shohan.omnicalc.core.math.UnitConvert
import com.shohan.omnicalc.core.math.UnitDef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tools destination state (spec §11–§14). All three shipped tools share this
 * ViewModel; every result is computed by real math (UnitConvert/DateMath/
 * BigInteger) and formatted through the shared display pipeline.
 */
class ToolsViewModel(
    private val services: AppServices,
    private val numberMessage: String,
    private val convertErrorMessage: String,
    private val dateOrderMessage: String,
    private val divisionMessage: String,
) : ViewModel() {

    enum class Tool { HOME, CONVERTER, PROGRAMMER, DATE }
    enum class DateMode { DIFF, SHIFT, AGE }
    enum class Base(val radix: Int, val filter: Regex, val label: String) {
        BIN(2, Regex("[01]+"), "BIN"),
        OCT(8, Regex("[0-7]+"), "OCT"),
        DEC(10, Regex("[0-9]+"), "DEC"),
        HEX(16, Regex("[0-9a-fA-F]+"), "HEX"),
    }
    enum class BitOp(val symbol: String) {
        AND("&"), OR("|"), XOR("^"), NOT("!"), SHL("<<"), SHR(">>"),
        ADD("+"), SUB("-"), MUL("*"), DIV("/"),
    }

    data class UiState(
        val tool: Tool = Tool.HOME,
        val settings: AppSettings = AppSettings(),
        // ---- converter ----
        val category: UnitCategory = UnitCategory.LENGTH,
        val fromId: String = "m",
        val toId: String = "ft",
        val amountText: String = "1",
        val convertedDisplay: String? = null,
        val convertError: String? = null,
        // ---- programmer ----
        val base: Base = Base.DEC,
        val valueA: String = "0",
        val valueB: String = "0",
        val bitResultDisplay: String? = null,
        val bitResultByBase: Map<Base, String> = emptyMap(),
        val bitError: String? = null,
        // ---- date/time ----
        val dateMode: DateMode = DateMode.DIFF,
        val startDate: String = "2026-01-01",   // ISO yyyy-MM-dd for saveable state
        val endDate: String = "2026-10-21",
        val shiftDate: String = "2026-01-01",
        val shiftDays: String = "30",
        val birthDate: String = "2000-01-01",
        val dateResult: String? = null,
        val dateError: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            services.settingsRepository.settings.collect { s -> _ui.update { it.copy(settings = s) } }
        }
    }

    // ------------------------------------------------------------- routing --
    fun open(tool: Tool) = _ui.update { it.copy(tool = tool, convertError = null, bitError = null, dateError = null) }

    // ---------------------------------------------------------- converter --
    fun setCategory(category: UnitCategory) {
        _ui.update { s ->
            val units = UnitCatalog.units(category)
            s.copy(
                category = category,
                fromId = units[0].id,
                toId = units[1].id,
                convertedDisplay = null,
                convertError = null,
            )
        }
        recomputeConversion()
    }

    fun setFromUnit(id: String) = _ui.update { it.copy(fromId = id) }.also { recomputeConversion() }

    fun setToUnit(id: String) = _ui.update { it.copy(toId = id) }.also { recomputeConversion() }

    fun swapUnits() = _ui.update { it.copy(fromId = it.toId, toId = it.fromId) }.also { recomputeConversion() }

    fun setAmount(text: String) = _ui.update { it.copy(amountText = text) }.also { recomputeConversion() }

    private fun recomputeConversion() {
        val s = _ui.value
        val from = UnitCatalog.find(s.fromId) ?: return
        val to = UnitCatalog.find(s.toId) ?: return
        val raw = s.amountText.trim()
        if (raw.isEmpty()) {
            _ui.update { it.copy(convertedDisplay = null, convertError = null) }
            return
        }
        val value = try {
            BigDecimal(Numerals.normalizeToWesternDigits(raw))
        } catch (e: NumberFormatException) {
            _ui.update { it.copy(convertedDisplay = null, convertError = numberMessage) }
            return
        }
        val out = UnitConvert.convert(value, from, to)
        if (out == null) {
            _ui.update { it.copy(convertedDisplay = null, convertError = convertErrorMessage) }
        } else {
            _ui.update {
                it.copy(convertedDisplay = DisplayFormatter.format(out, it.settings), convertError = null)
            }
        }
    }

    /** IME "done" in the amount field commits a valid conversion to history. */
    fun commitConversion() {
        val s = _ui.value
        if (s.convertedDisplay == null || s.convertError != null) return
        val from = UnitCatalog.find(s.fromId) ?: return
        val to = UnitCatalog.find(s.toId) ?: return
        val plain = try {
            BigDecimal(Numerals.normalizeToWesternDigits(s.amountText.trim()))
        } catch (e: NumberFormatException) {
            return
        }
        viewModelScope.launch {
            services.historyRepository.record(
                expression = "${plain.toPlainString()} ${from.symbol} → ${to.symbol}",
                resultPlain = UnitConvert.convert(plain, from, to)?.toPlainString() ?: return@launch,
                exact = true,
                toolType = "converter",
                category = s.category.name.lowercase(),
                contextPairs = "from=${from.id}&to=${to.id}",
            )
        }
    }

    // --------------------------------------------------------- programmer --
    fun setBase(base: Base) = _ui.update { it.copy(base = base) }.also { recomputeBitwise() }

    fun setValueA(text: String) = _ui.update { it.copy(valueA = text) }.also { recomputeBitwise() }

    fun setValueB(text: String) = _ui.update { it.copy(valueB = text) }.also { recomputeBitwise() }

    private fun parseInBase(text: String, base: Base): BigInteger? {
        val t = Numerals.normalizeToWesternDigits(text.trim())
        if (t.isEmpty() || !base.filter.matches(t)) return null
        return try {
            BigInteger(t, base.radix)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun recomputeBitwise() {
        val s = _ui.value
        val a = parseInBase(s.valueA, s.base)
        val b = parseInBase(s.valueB, s.base)
        if (a == null || b == null) {
            _ui.update { it.copy(bitResultDisplay = null, bitResultByBase = emptyMap(), bitError = null) }
            return
        }
        // No auto-op: display the two operands in every base (spec §13.0
        // "always-visible base readouts"). Operations apply on demand below.
        val readouts = Base.entries.associateWith { base ->
            formatInBase(a, base)
        }
        _ui.update {
            it.copy(
                bitResultDisplay = formatInBase(a, s.base),
                bitResultByBase = readouts,
                bitError = null,
            )
        }
    }

    /** Applies [op] to operands parsed in the CURRENT base; refreshes all bases. */
    fun applyOp(op: BitOp) {
        val s = _ui.value
        val a = parseInBase(s.valueA, s.base) ?: return failBit()
        val b = parseInBase(s.valueB, s.base) ?: return failBit()
        if (op == BitOp.DIV && b.signum() == 0) return setError(bitError = divisionMessage)
        val result: BigInteger = when (op) {
            BitOp.AND -> a.and(b)
            BitOp.OR -> a.or(b)
            BitOp.XOR -> a.xor(b)
            BitOp.NOT -> a.not()
            BitOp.SHL -> a.shiftLeft(b.toInt().coerceIn(0, 4096))
            BitOp.SHR -> a.shiftRight(b.toInt().coerceIn(0, 4096))
            BitOp.ADD -> a.add(b)
            BitOp.SUB -> a.subtract(b)
            BitOp.MUL -> a.multiply(b)
            BitOp.DIV -> a.divide(b)
        }
        val readouts = Base.entries.associateWith { base -> formatInBase(result, base) }
        _ui.update {
            it.copy(
                valueA = formatInBase(result, s.base),
                bitResultDisplay = formatInBase(result, s.base),
                bitResultByBase = readouts,
                bitError = null,
            )
        }
        viewModelScope.launch {
            val expr = if (op == BitOp.NOT) {
                "NOT ${a.toString(s.base.radix)} (${s.base.label})"
            } else {
                "${a.toString(s.base.radix)} ${op.symbol} ${b.toString(s.base.radix)} (${s.base.label})"
            }
            services.historyRepository.record(
                expression = expr,
                resultPlain = result.toString(),
                exact = true,
                toolType = "programmer",
                category = s.base.label.lowercase(),
            )
        }
    }

    private fun failBit() = setError(bitError = numberMessage)

    private fun setError(bitError: String) {
        _ui.update { it.copy(bitError = bitError, bitResultDisplay = null, bitResultByBase = emptyMap()) }
    }

    private fun formatInBase(v: BigInteger, base: Base): String =
        if (base == Base.DEC) v.toString(10) else v.toString(base.radix).uppercase()

    // ------------------------------------------------------------- date --
    fun setDateMode(mode: DateMode) = _ui.update { it.copy(dateMode = mode, dateResult = null, dateError = null) }

    fun setStartDate(iso: String) = _ui.update { it.copy(startDate = iso, dateResult = null) }
    fun setEndDate(iso: String) = _ui.update { it.copy(endDate = iso, dateResult = null) }
    fun setShiftDate(iso: String) = _ui.update { it.copy(shiftDate = iso, dateResult = null) }
    fun setShiftDays(text: String) = _ui.update { it.copy(shiftDays = text, dateResult = null) }
    fun setBirthDate(iso: String) = _ui.update { it.copy(birthDate = iso, dateResult = null) }

    /** Real computation for the active date mode; errors are localized. */
    fun computeDate() {
        val s = _ui.value
        _ui.update { it.copy(dateResult = null, dateError = null) }
        when (s.dateMode) {
            DateMode.DIFF -> {
                val a = parseIso(s.startDate) ?: return dateFail(numberMessage)
                val b = parseIso(s.endDate) ?: return dateFail(numberMessage)
                if (DateMath.toEpochDay(*a) > DateMath.toEpochDay(*b)) return dateFail(dateOrderMessage)
                val days = DateMath.diffDays(a[0], a[1], a[2], b[0], b[1], b[2])
                val weeks = days / 7
                val rem = days % 7
                val monthsApprox = days / 30
                _ui.update {
                    it.copy(
                        dateResult = "$days days · $weeks w $rem d · ≈ $monthsApprox months",
                        dateError = null,
                    )
                }
            }
            DateMode.SHIFT -> {
                val d = parseIso(s.shiftDate) ?: return dateFail(numberMessage)
                val n = try {
                    Numerals.normalizeToWesternDigits(s.shiftDays.trim()).toInt()
                } catch (e: NumberFormatException) {
                    return dateFail(numberMessage)
                }
                val out = DateMath.addDays(d[0], d[1], d[2], n)
                if (!DateMath.isValidDate(out[0], out[1], out[2])) return dateFail(numberMessage)
                _ui.update { it.copy(dateResult = iso(out), dateError = null) }
            }
            DateMode.AGE -> {
                val b = parseIso(s.birthDate) ?: return dateFail(numberMessage)
                val today = todayIso()
                val t = parseIso(today) ?: return dateFail(numberMessage)
                if (DateMath.toEpochDay(*b) > DateMath.toEpochDay(*t)) return dateFail(dateOrderMessage)
                val years = DateMath.ageYears(b[0], b[1], b[2], t[0], t[1], t[2])
                val lived = DateMath.diffDays(b[0], b[1], b[2], t[0], t[1], t[2])
                _ui.update { it.copy(dateResult = "$years years · $lived days lived", dateError = null) }
            }
        }
    }

    private fun dateFail(message: String) = _ui.update { it.copy(dateError = message, dateResult = null) }

    private fun parseIso(iso: String): IntArray? {
        val parts = iso.trim().split('-')
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (!DateMath.isValidDate(y, m, d)) return null
        return intArrayOf(y, m, d)
    }

    private fun iso(d: IntArray): String =
        "%04d-%02d-%02d".format(d[0], d[1], d[2])

    private fun todayIso(): String {
        val c = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            c.get(java.util.Calendar.YEAR),
            c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH),
        )
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                val context = (app as android.app.Application).applicationContext
                ToolsViewModel(
                    services = app,
                    numberMessage = context.getString(com.shohan.omnicalc.core.localization.R.string.formula_error_number),
                    convertErrorMessage = context.getString(com.shohan.omnicalc.core.localization.R.string.converter_error),
                    dateOrderMessage = context.getString(com.shohan.omnicalc.core.localization.R.string.date_error_order),
                    divisionMessage = context.getString(com.shohan.omnicalc.core.localization.R.string.engine_error_division_by_zero),
                )
            }
        }
    }
}
