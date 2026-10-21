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

// ------------------------------------------------------- currency tool --

class CurrencyViewModel(private val services: AppServices, private val numberMessage: String) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings(),
        val rows: List<FinanceTools.CurrencyRow> = FinanceTools.defaultCurrencyRows(),
        val selectedCode: String = "USD",
        val amountText: String = "1",
        /** true → foreign to BDT; false → BDT to foreign. */
        val toBdt: Boolean = true,
        val resultDisplay: String? = null,
        val error: String? = null,
        val missingRate: Boolean = false,
        val editCode: String? = null,
        val editRate: String = "",
        val editSource: String = "",
        val editDate: String = "",
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            services.settingsRepository.settings.collect { s ->
                _ui.update { it.copy(settings = s, rows = FinanceTools.deserializeCurrency(s.currencyRatesRaw)) }
                recompute()
            }
        }
    }

    fun select(code: String) = _ui.update { it.copy(selectedCode = code) }.also { recompute() }
    fun setAmount(t: String) = _ui.update { it.copy(amountText = t) }.also { recompute() }
    fun setToBdt(v: Boolean) = _ui.update { it.copy(toBdt = v) }.also { recompute() }

    fun beginEdit(code: String) {
        _ui.update { st ->
            val r = st.rows.firstOrNull { it.code == code } ?: return@update st
            st.copy(editCode = code, editRate = r.rate?.toPlainString().orEmpty(), editSource = r.source, editDate = r.effectiveDate)
        }
    }

    fun endEdit() = _ui.update { it.copy(editCode = null) }

    fun setEditRate(v: String) = _ui.update { it.copy(editRate = v) }
    fun setEditSource(v: String) = _ui.update { it.copy(editSource = v) }
    fun setEditDate(v: String) = _ui.update { it.copy(editDate = v) }

    fun saveEdit() {
        val st = _ui.value
        val code = st.editCode ?: return
        val rate = st.editRate.trim().ifEmpty { null }?.let {
            try {
                BigDecimal(Numerals.normalizeToWesternDigits(it))
            } catch (e: NumberFormatException) {
                null
            }
        }
        val rows = st.rows.map { r ->
            if (r.code == code) r.copy(rate = rate, source = st.editSource, effectiveDate = st.editDate) else r
        }
        viewModelScope.launch {
            services.settingsRepository.setCurrencyRates(FinanceTools.serializeCurrency(rows))
            _ui.update { it.copy(editCode = null) }
        }
        recompute()
    }

    private fun recompute() {
        val st = _ui.value
        val row = st.rows.firstOrNull { it.code == st.selectedCode } ?: return
        val raw = st.amountText.trim()
        if (raw.isEmpty()) {
            _ui.update { it.copy(resultDisplay = null, error = null, missingRate = row.rate == null) }
            return
        }
        val amount = try {
            BigDecimal(Numerals.normalizeToWesternDigits(raw))
        } catch (e: NumberFormatException) {
            _ui.update { it.copy(resultDisplay = null, error = numberMessage, missingRate = false) }
            return
        }
        if (row.rate == null) {
            _ui.update { it.copy(resultDisplay = null, error = null, missingRate = true) }
            return
        }
        val out = if (st.toBdt) FinanceTools.toBdt(amount, row) else FinanceTools.fromBdt(amount, row)
        _ui.update {
            it.copy(
                resultDisplay = out?.let { v -> DisplayFormatter.format(v, it.settings) },
                error = if (out == null) numberMessage else null,
                missingRate = false,
            )
        }
    }

    fun commitHistory() {
        val st = _ui.value
        val display = st.resultDisplay ?: return
        val row = st.rows.firstOrNull { it.code == st.selectedCode } ?: return
        val amount = try {
            BigDecimal(Numerals.normalizeToWesternDigits(st.amountText.trim()))
        } catch (e: NumberFormatException) {
            return
        }
        viewModelScope.launch {
            services.historyRepository.record(
                expression = "${amount.toPlainString()} ${if (st.toBdt) row.code else "BDT"} → ${if (st.toBdt) "BDT" else row.code} (offline ref ${row.effectiveDate.ifBlank { "undated" }})",
                resultPlain = if (st.toBdt) FinanceTools.toBdt(amount, row)?.toPlainString() ?: return@launch
                else FinanceTools.fromBdt(amount, row)?.toPlainString() ?: return@launch,
                exact = false,
                toolType = "currency",
                category = row.code,
                contextPairs = "rate=${row.rate?.toPlainString().orEmpty()}&source=${row.source.take(80)}&effective=${row.effectiveDate}",
            )
        }
        @Suppress("UNUSED_EXPRESSION") display
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                val ctx = (app as android.app.Application).applicationContext
                CurrencyViewModel(app, ctx.getString(com.shohan.omnicalc.core.localization.R.string.formula_error_number))
            }
        }
    }
}
