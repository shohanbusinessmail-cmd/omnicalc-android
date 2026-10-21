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
import com.shohan.omnicalc.core.math.EvalContext
import com.shohan.omnicalc.core.math.Engine
import com.shohan.omnicalc.core.math.CalcResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

// ------------------------------------------------------------ land tool --

class LandViewModel(private val services: AppServices, private val numberMessage: String) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings(),
        val profiles: List<LandProfile> = LandProfiles.BUILT_IN,
        val selectedProfileId: String = LandProfiles.BUILT_IN.first().id,
        val amountText: String = "1",
        val fromUnit: String = "Decimal",
        val toUnit: String = "Square foot",
        val resultDisplay: String? = null,
        val error: String? = null,
        val editingSource: String = "",
        val editingDate: String = "",
        val dirty: Boolean = false,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            services.settingsRepository.settings.collect { s ->
                val profiles = LandProfiles.deserialize(s.landProfilesRaw)
                _ui.update { st ->
                    val selectedStill = profiles.any { it.id == st.selectedProfileId }
                    st.copy(
                        settings = s,
                        profiles = profiles,
                        selectedProfileId = if (selectedStill) st.selectedProfileId else profiles.first().id,
                        editingSource = profiles.firstOrNull { it.id == st.selectedProfileId }?.source ?: st.editingSource,
                        editingDate = profiles.firstOrNull { it.id == st.selectedProfileId }?.effectiveDate ?: st.editingDate,
                    )
                }
                recompute()
            }
        }
    }

    private fun selected(): LandProfile =
        _ui.value.profiles.firstOrNull { it.id == _ui.value.selectedProfileId } ?: LandProfiles.BUILT_IN.first()

    fun selectProfile(id: String) {
        _ui.update { st ->
            val p = st.profiles.firstOrNull { it.id == id } ?: return@update st
            st.copy(
                selectedProfileId = id,
                editingSource = p.source,
                editingDate = p.effectiveDate,
                error = null,
            )
        }
        recompute()
    }

    fun setAmount(text: String) = _ui.update { it.copy(amountText = text) }.also { recompute() }
    fun setFromUnit(u: String) = _ui.update { it.copy(fromUnit = u) }.also { recompute() }
    fun setToUnit(u: String) = _ui.update { it.copy(toUnit = u) }.also { recompute() }
    fun swap() = _ui.update { it.copy(fromUnit = it.toUnit, toUnit = it.fromUnit) }.also { recompute() }

    fun setEditingSource(v: String) = _ui.update { it.copy(editingSource = v, dirty = true) }
    fun setEditingDate(v: String) = _ui.update { it.copy(editingDate = v, dirty = true) }

    /** Updates one unit factor of the selected profile (user's own values). */
    fun setUnitFactor(unit: String, raw: String) {
        val factor = try {
            BigDecimal(Numerals.normalizeToWesternDigits(raw.trim()))
        } catch (e: NumberFormatException) {
            return
        }
        if (factor.signum() <= 0) return
        _ui.update { st ->
            val profiles = st.profiles.map { p ->
                if (p.id == st.selectedProfileId) p.copy(units = p.units + (unit to factor)) else p
            }
            st.copy(profiles = profiles, dirty = true)
        }
        recompute()
    }

    fun saveProfile() {
        val st = _ui.value
        val profiles = st.profiles.map { p ->
            if (p.id == st.selectedProfileId) p.copy(source = st.editingSource, effectiveDate = st.editingDate) else p
        }
        viewModelScope.launch {
            services.settingsRepository.setLandProfiles(LandProfiles.serialize(profiles))
            _ui.update { it.copy(dirty = false) }
        }
    }

    fun resetToBuiltIn() {
        viewModelScope.launch { services.settingsRepository.setLandProfiles("") }
    }

    private fun recompute() {
        val st = _ui.value
        val raw = st.amountText.trim()
        if (raw.isEmpty()) {
            _ui.update { it.copy(resultDisplay = null, error = null) }
            return
        }
        val amount = try {
            BigDecimal(Numerals.normalizeToWesternDigits(raw))
        } catch (e: NumberFormatException) {
            _ui.update { it.copy(resultDisplay = null, error = numberMessage) }
            return
        }
        val out = LandProfiles.convert(amount, st.fromUnit, st.toUnit, selected())
        if (out == null) {
            _ui.update { it.copy(resultDisplay = null, error = st.error ?: numberMessage) }
        } else {
            _ui.update { it.copy(resultDisplay = DisplayFormatter.format(out, it.settings), error = null) }
        }
    }

    fun commitHistory() {
        val st = _ui.value
        val display = st.resultDisplay ?: return
        val amount = try {
            BigDecimal(Numerals.normalizeToWesternDigits(st.amountText.trim()))
        } catch (e: NumberFormatException) {
            return
        }
        viewModelScope.launch {
            services.historyRepository.record(
                expression = "${amount.toPlainString()} ${st.fromUnit} → ${st.toUnit} (${st.selectedProfileId})",
                resultPlain = LandProfiles.convert(amount, st.fromUnit, st.toUnit, selected())?.toPlainString()
                    ?: return@launch,
                exact = true,
                toolType = "land",
                category = st.selectedProfileId,
                contextPairs = "source=${st.editingSource.take(80)}&effective=${st.editingDate}",
            )
        }
        @Suppress("UNUSED_EXPRESSION") display
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                val ctx = (app as android.app.Application).applicationContext
                LandViewModel(app, ctx.getString(com.shohan.omnicalc.core.localization.R.string.formula_error_number))
            }
        }
    }
}

// ------------------------------------------------------------ VAT tool --

class VatViewModel(private val services: AppServices, private val numberMessage: String) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings(),
        val amountText: String = "",
        val rateText: String = "",
        val inclusive: Boolean = false,
        val result: FinanceTools.VatResult? = null,
        val error: String? = null,
        val source: String = "",
        val effectiveDate: String = "",
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            services.settingsRepository.settings.collect { s ->
                _ui.update { st ->
                    st.copy(
                        settings = s,
                        source = st.source.ifEmpty { s.vatSource.orEmpty() },
                        effectiveDate = st.effectiveDate.ifEmpty { s.vatEffectiveDate.orEmpty() },
                    )
                }
                recompute()
            }
        }
    }

    fun setAmount(t: String) = _ui.update { it.copy(amountText = t) }.also { recompute() }
    fun setRate(t: String) = _ui.update { it.copy(rateText = t) }.also { recompute() }
    fun setInclusive(v: Boolean) = _ui.update { it.copy(inclusive = v) }.also { recompute() }
    fun setSource(v: String) = _ui.update { it.copy(source = v) }
    fun setEffectiveDate(v: String) = _ui.update { it.copy(effectiveDate = v) }

    fun saveMetadata() {
        val st = _ui.value
        viewModelScope.launch {
            services.settingsRepository.setVatSource(st.source.ifBlank { null })
            services.settingsRepository.setVatEffectiveDate(st.effectiveDate.ifBlank { null })
        }
    }

    private fun recompute() {
        val st = _ui.value
        val amount = parse(st.amountText)
        val rate = parse(st.rateText)
        if (amount == null || rate == null) {
            _ui.update { it.copy(result = null, error = if (st.amountText.isBlank() && st.rateText.isBlank()) null else numberMessage) }
            return
        }
        val mode = if (st.inclusive) FinanceTools.VatMode.INCLUSIVE else FinanceTools.VatMode.EXCLUSIVE
        val r = FinanceTools.vat(amount, rate, mode)
        _ui.update { it.copy(result = r, error = if (r == null) numberMessage else null) }
    }

    private fun parse(text: String): BigDecimal? {
        val t = text.trim()
        if (t.isEmpty()) return null
        return try {
            BigDecimal(Numerals.normalizeToWesternDigits(t))
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun commitHistory() {
        val st = _ui.value
        val r = st.result ?: return
        val rate = parse(st.rateText) ?: return
        viewModelScope.launch {
            services.historyRepository.record(
                expression = "VAT ${if (st.inclusive) "(incl.)" else "(excl.)"} ${st.amountText} @ ${rate.toPlainString()}%",
                resultPlain = r.gross.toPlainString(),
                exact = true,
                toolType = "vat",
                category = if (st.inclusive) "inclusive" else "exclusive",
                contextPairs = "rate=${rate.toPlainString()}&source=${st.source.take(80)}&effective=${st.effectiveDate}",
            )
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                val ctx = (app as android.app.Application).applicationContext
                VatViewModel(app, ctx.getString(com.shohan.omnicalc.core.localization.R.string.formula_error_number))
            }
        }
    }
}

// ---------------------------------------------------------- salary tool --

class SalaryViewModel(private val services: AppServices, private val numberMessage: String) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings(),
        val earnings: List<FinanceTools.SalaryRow> = emptyList(),
        val deductions: List<FinanceTools.SalaryRow> = emptyList(),
        val totals: FinanceTools.SalaryTotals = FinanceTools.salaryTotals(emptyList(), emptyList()),
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()
    private var nextId = 1L

    init {
        viewModelScope.launch {
            services.settingsRepository.settings.collect { s -> _ui.update { it.copy(settings = s) } }
        }
    }

    fun addEarning() = addRow(earning = true)
    fun addDeduction() = addRow(earning = false)

    private fun addRow(earning: Boolean) {
        _ui.update { st ->
            val row = FinanceTools.SalaryRow(nextId++, "", BigDecimal.ZERO)
            val new = if (earning) st.copy(earnings = st.earnings + row) else st.copy(deductions = st.deductions + row)
            new.copy(totals = FinanceTools.salaryTotals(new.earnings, new.deductions))
        }
    }

    fun removeRow(id: Long) {
        _ui.update { st ->
            val new = st.copy(
                earnings = st.earnings.filterNot { it.id == id },
                deductions = st.deductions.filterNot { it.id == id },
            )
            new.copy(totals = FinanceTools.salaryTotals(new.earnings, new.deductions))
        }
    }

    fun setLabel(id: Long, label: String) = edit(id) { it.copy(label = label) }

    fun setAmount(id: Long, raw: String) {
        val amount = try {
            BigDecimal(Numerals.normalizeToWesternDigits(raw.trim()).ifEmpty { "0" })
        } catch (e: NumberFormatException) {
            _ui.update { it.copy(error = numberMessage) }
            return
        }
        edit(id) { it.copy(amount = amount) }
        _ui.update { it.copy(error = null) }
    }

    private fun edit(id: Long, transform: (FinanceTools.SalaryRow) -> FinanceTools.SalaryRow) {
        _ui.update { st ->
            val new = st.copy(
                earnings = st.earnings.map { if (it.id == id) transform(it) else it },
                deductions = st.deductions.map { if (it.id == id) transform(it) else it },
            )
            new.copy(totals = FinanceTools.salaryTotals(new.earnings, new.deductions))
        }
    }

    fun clearAll() {
        _ui.update { st ->
            val new = st.copy(earnings = emptyList(), deductions = emptyList())
            new.copy(totals = FinanceTools.salaryTotals(emptyList(), emptyList()))
        }
    }

    fun commitHistory() {
        val t = _ui.value.totals
        if (t.earnings.signum() == 0) return
        viewModelScope.launch {
            services.historyRepository.record(
                expression = "Salary: earnings ${t.earnings.toPlainString()} − deductions ${t.deductions.toPlainString()}",
                resultPlain = t.net.toPlainString(),
                exact = true,
                toolType = "salary",
            )
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                val ctx = (app as android.app.Application).applicationContext
                SalaryViewModel(app, ctx.getString(com.shohan.omnicalc.core.localization.R.string.formula_error_number))
            }
        }
    }
}

// ---------------------------------------------------- quick tools (§11) --

/**
 * High-frequency everyday math: percentage, discount and bill split.
 * Pure BigDecimal, live results, formatted through the shared pipeline.
 */
class QuickToolsViewModel(
    private val services: AppServices,
    private val numberMessage: String,
    private val divisionMessage: String,
) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings(),
        // percentage: X is what % of Y
        val pctPart: String = "",
        val pctWhole: String = "",
        val pctResult: String? = null,
        val pctError: String? = null,
        // discount
        val discPrice: String = "",
        val discRate: String = "",
        val discPay: String? = null,
        val discSave: String? = null,
        val discError: String? = null,
        // split bill
        val splitTotal: String = "",
        val splitPeople: String = "2",
        val splitTip: String = "",
        val splitPer: String? = null,
        val splitGrand: String? = null,
        val splitError: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            services.settingsRepository.settings.collect { s -> _ui.update { it.copy(settings = s) } }
        }
    }

    // ---- percentage ----
    fun setPctPart(v: String) = _ui.update { it.copy(pctPart = v) }.also { recomputePct() }
    fun setPctWhole(v: String) = _ui.update { it.copy(pctWhole = v) }.also { recomputePct() }

    private fun recomputePct() {
        val st = _ui.value
        val part = parse(st.pctPart) ?: return clearPct()
        val whole = parse(st.pctWhole) ?: return clearPct()
        if (whole.signum() == 0) {
            _ui.update { it.copy(pctResult = null, pctError = divisionMessage) }
            return
        }
        val pct = part.multiply(BigDecimal(100)).divide(whole, java.math.MathContext(15, java.math.RoundingMode.HALF_UP))
        _ui.update { it.copy(pctResult = fmt(pct), pctError = null) }
    }

    private fun clearPct() = _ui.update { it.copy(pctResult = null, pctError = null) }

    // ---- discount ----
    fun setDiscPrice(v: String) = _ui.update { it.copy(discPrice = v) }.also { recomputeDisc() }
    fun setDiscRate(v: String) = _ui.update { it.copy(discRate = v) }.also { recomputeDisc() }

    private fun recomputeDisc() {
        val st = _ui.value
        val price = parse(st.discPrice) ?: return clearDisc()
        val rate = parse(st.discRate) ?: return clearDisc()
        if (rate.signum() < 0 || rate.compareTo(BigDecimal(100)) > 0) {
            _ui.update { it.copy(discPay = null, discSave = null, discError = numberMessage) }
            return
        }
        val pay = price.multiply(BigDecimal(100).subtract(rate)).divide(BigDecimal(100), java.math.MathContext(15, java.math.RoundingMode.HALF_UP))
        val save = price.subtract(pay)
        _ui.update { it.copy(discPay = fmt(pay), discSave = fmt(save), discError = null) }
    }

    private fun clearDisc() = _ui.update { it.copy(discPay = null, discSave = null, discError = null) }

    // ---- split bill ----
    fun setSplitTotal(v: String) = _ui.update { it.copy(splitTotal = v) }.also { recomputeSplit() }
    fun setSplitPeople(v: String) = _ui.update { it.copy(splitPeople = v) }.also { recomputeSplit() }
    fun setSplitTip(v: String) = _ui.update { it.copy(splitTip = v) }.also { recomputeSplit() }

    private fun recomputeSplit() {
        val st = _ui.value
        val total = parse(st.splitTotal) ?: return clearSplit()
        val people = try {
            com.shohan.omnicalc.core.common.Numerals.normalizeToWesternDigits(st.splitPeople.trim()).toBigDecimalOrNull()
        } catch (e: Exception) {
            null
        }
        if (people == null || people.compareTo(BigDecimal.ONE) < 0 || people.stripTrailingZeros().scale() > 0) {
            if (st.splitPeople.isBlank()) return clearSplit()
            _ui.update { it.copy(splitPer = null, splitGrand = null, splitError = numberMessage) }
            return
        }
        val tip = if (st.splitTip.isBlank()) BigDecimal.ZERO else (parse(st.splitTip) ?: return clearSplit())
        if (tip.signum() < 0) {
            _ui.update { it.copy(splitPer = null, splitGrand = null, splitError = numberMessage) }
            return
        }
        val grand = total.multiply(BigDecimal(100).add(tip)).divide(BigDecimal(100), java.math.MathContext(15, java.math.RoundingMode.HALF_UP))
        val per = grand.divide(people, java.math.MathContext(15, java.math.RoundingMode.HALF_UP))
        _ui.update { it.copy(splitPer = fmt(per), splitGrand = fmt(grand), splitError = null) }
    }

    private fun clearSplit() = _ui.update { it.copy(splitPer = null, splitGrand = null, splitError = null) }

    private fun parse(text: String): BigDecimal? {
        val t = text.trim()
        if (t.isEmpty()) return null
        return try {
            BigDecimal(com.shohan.omnicalc.core.common.Numerals.normalizeToWesternDigits(t))
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun fmt(v: BigDecimal): String = DisplayFormatter.format(v, _ui.value.settings)

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                val ctx = (app as android.app.Application).applicationContext
                QuickToolsViewModel(
                    services = app,
                    numberMessage = ctx.getString(com.shohan.omnicalc.core.localization.R.string.formula_error_number),
                    divisionMessage = ctx.getString(com.shohan.omnicalc.core.localization.R.string.engine_error_division_by_zero),
                )
            }
        }
    }
}
