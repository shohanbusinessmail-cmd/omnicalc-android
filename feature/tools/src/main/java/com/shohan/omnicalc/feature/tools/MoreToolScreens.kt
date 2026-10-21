package com.shohan.omnicalc.feature.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohan.omnicalc.core.designsystem.ErrorBanner
import com.shohan.omnicalc.core.designsystem.OmniCard
import com.shohan.omnicalc.core.designsystem.OmniSectionHeader
import com.shohan.omnicalc.core.designsystem.ResultText
import com.shohan.omnicalc.core.localization.R as LocR
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------- land ----

@Composable
fun LandTool(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LandViewModel = viewModel(factory = LandViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val isBn = androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0).language == "bn"
    val profile = ui.profiles.firstOrNull { it.id == ui.selectedProfileId } ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = onBack)
        OmniSectionHeader(stringResource(LocR.string.tool_land))

        Text(
            text = stringResource(LocR.string.land_verify_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))

        // Profile selector + provenance card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ui.profiles.forEach { p ->
                FilterChip(
                    selected = p.id == ui.selectedProfileId,
                    onClick = { viewModel.selectProfile(p.id) },
                    label = { Text(if (isBn) p.nameBn else p.nameEn, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        OmniCard {
            Text(
                text = stringResource(LocR.string.converter_category) + ": " + profile.region,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(LocR.string.land_source),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = profile.source, style = MaterialTheme.typography.bodySmall)
            Text(
                text = stringResource(LocR.string.land_effective) + ": " + profile.effectiveDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = ui.amountText,
            onValueChange = viewModel::setAmount,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.converter_amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            trailingIcon = {
                TextButton(onClick = viewModel::commitHistory) {
                    Text(stringResource(LocR.string.converter_save))
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        UnitPickRow(label = stringResource(LocR.string.converter_from), units = profile.units.keys.toList(), selected = ui.fromUnit, onSelect = viewModel::setFromUnit)
        UnitPickRow(label = stringResource(LocR.string.converter_to), units = profile.units.keys.toList(), selected = ui.toUnit, onSelect = viewModel::setToUnit)
        OutlinedButton(onClick = viewModel::swap, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(LocR.string.converter_swap))
        }
        Spacer(Modifier.height(10.dp))

        ui.error?.let { ErrorBanner(message = it, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(6.dp)) }
        ui.resultDisplay?.let { r ->
            ResultText(
                text = "$r ${ui.toUnit}",
                style = MaterialTheme.typography.headlineSmall,
                contentDescription = r,
            )
        }

        Spacer(Modifier.height(12.dp))
        OmniSectionHeader(stringResource(LocR.string.land_units_note))
        profile.units.keys.forEach { unit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(unit, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = profile.units[unit]?.toPlainString().orEmpty(),
                    onValueChange = { viewModel.setUnitFactor(unit, it) },
                    modifier = Modifier.width(150.dp),
                    singleLine = true,
                    label = { Text(stringResource(LocR.string.land_sqft_factor)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        OutlinedTextField(
            value = ui.editingSource,
            onValueChange = viewModel::setEditingSource,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(LocR.string.land_source)) },
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = ui.editingDate,
            onValueChange = viewModel::setEditingDate,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.land_effective)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::saveProfile, enabled = ui.dirty) {
                Text(stringResource(LocR.string.land_save))
            }
            OutlinedButton(onClick = viewModel::resetToBuiltIn) {
                Text(stringResource(LocR.string.land_reset))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun UnitPickRow(label: String, units: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            units.forEach { u ->
                FilterChip(
                    selected = u == selected,
                    onClick = { onSelect(u) },
                    label = { Text(u, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- VAT ----

@Composable
fun VatTool(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VatViewModel = viewModel(factory = VatViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = onBack)
        OmniSectionHeader(stringResource(LocR.string.tool_vat))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(LocR.string.vat_mode_exclusive), style = MaterialTheme.typography.labelLarge)
            Switch(checked = ui.inclusive, onCheckedChange = viewModel::setInclusive)
            Text(stringResource(LocR.string.vat_mode_inclusive), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = ui.amountText,
            onValueChange = viewModel::setAmount,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.converter_amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = ui.rateText,
            onValueChange = viewModel::setRate,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.vat_rate)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(8.dp))

        ui.error?.let { ErrorBanner(message = it, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(6.dp)) }
        val r = ui.result
        if (r != null) {
            val vf = { v: java.math.BigDecimal -> com.shohan.omnicalc.core.common.DisplayFormatter.format(v, ui.settings) }
            KeyValueSimple(stringResource(LocR.string.vat_net), vf(r.net))
            KeyValueSimple(stringResource(LocR.string.vat_tax), vf(r.tax))
            KeyValueSimple(stringResource(LocR.string.vat_gross), vf(r.gross))
            Spacer(Modifier.height(6.dp))
            Button(onClick = viewModel::commitHistory, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(LocR.string.converter_save))
            }
        }

        Spacer(Modifier.height(12.dp))
        OmniSectionHeader(stringResource(LocR.string.vat_metadata))
        OutlinedTextField(
            value = ui.source,
            onValueChange = viewModel::setSource,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(LocR.string.currency_source)) },
            placeholder = { Text(stringResource(LocR.string.land_verify_note)) },
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = ui.effectiveDate,
            onValueChange = viewModel::setEffectiveDate,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.land_effective)) },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::saveMetadata, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(LocR.string.land_save))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun KeyValueSimple(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

// ------------------------------------------------------------ salary ----

@Composable
fun SalaryTool(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SalaryViewModel = viewModel(factory = SalaryViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = onBack)
        OmniSectionHeader(stringResource(LocR.string.tool_salary))

        OmniSectionHeader(stringResource(LocR.string.salary_earning))
        ui.earnings.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = row.label,
                    onValueChange = { viewModel.setLabel(row.id, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(LocR.string.salary_label_hint)) },
                )
                OutlinedTextField(
                    value = row.amount.toPlainString().let { if (it == "0") "" else it },
                    onValueChange = { viewModel.setAmount(row.id, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(LocR.string.converter_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                IconButton(onClick = { viewModel.removeRow(row.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(LocR.string.action_delete))
                }
            }
        }
        OutlinedButton(onClick = viewModel::addEarning, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" " + stringResource(LocR.string.salary_add_earning))
        }

        OmniSectionHeader(stringResource(LocR.string.salary_deduction))
        ui.deductions.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = row.label,
                    onValueChange = { viewModel.setLabel(row.id, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(LocR.string.salary_label_hint)) },
                )
                OutlinedTextField(
                    value = row.amount.toPlainString().let { if (it == "0") "" else it },
                    onValueChange = { viewModel.setAmount(row.id, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(LocR.string.converter_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                IconButton(onClick = { viewModel.removeRow(row.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(LocR.string.action_delete))
                }
            }
        }
        OutlinedButton(onClick = viewModel::addDeduction, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" " + stringResource(LocR.string.salary_add_deduction))
        }

        Spacer(Modifier.height(8.dp))
        ui.error?.let { ErrorBanner(message = it, modifier = Modifier.fillMaxWidth()) }
        val sf = { v: java.math.BigDecimal -> com.shohan.omnicalc.core.common.DisplayFormatter.format(v, ui.settings) }
        KeyValueSimple(stringResource(LocR.string.salary_earning), sf(ui.totals.earnings))
        KeyValueSimple(stringResource(LocR.string.salary_deduction), sf(ui.totals.deductions))
        ui.totals.deductionPercent?.let {
            KeyValueSimple(stringResource(LocR.string.salary_deduction_pct), "${it.stripTrailingZeros().toPlainString()}%")
        }
        KeyValueSimple(stringResource(LocR.string.salary_net), sf(ui.totals.net))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::commitHistory, enabled = ui.totals.earnings.signum() > 0) {
                Text(stringResource(LocR.string.converter_save))
            }
            OutlinedButton(onClick = viewModel::clearAll) {
                Text(stringResource(LocR.string.action_clear))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------ quick tools (§11) ----

@Composable
fun PercentTool(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickToolsViewModel = viewModel(factory = QuickToolsViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = onBack)
        OmniSectionHeader(stringResource(LocR.string.tool_percent))
        OutlinedTextField(
            value = ui.pctPart,
            onValueChange = viewModel::setPctPart,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.pct_part)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = ui.pctWhole,
            onValueChange = viewModel::setPctWhole,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.pct_whole)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(8.dp))
        ui.pctError?.let { ErrorBanner(message = it, modifier = Modifier.fillMaxWidth()) }
        val pctResult = ui.pctResult
        if (pctResult != null) {
            ResultText(
                text = "$pctResult%",
                style = MaterialTheme.typography.headlineSmall,
                contentDescription = stringResource(LocR.string.pct_result_label) + ": $pctResult%",
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DiscountTool(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickToolsViewModel = viewModel(factory = QuickToolsViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = onBack)
        OmniSectionHeader(stringResource(LocR.string.tool_discount))
        OutlinedTextField(
            value = ui.discPrice,
            onValueChange = viewModel::setDiscPrice,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.discount_price)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = ui.discRate,
            onValueChange = viewModel::setDiscRate,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.discount_rate)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(8.dp))
        ui.discError?.let { ErrorBanner(message = it, modifier = Modifier.fillMaxWidth()) }
        val pay = ui.discPay
        val save = ui.discSave
        if (pay != null) {
            ResultText(
                text = pay,
                style = MaterialTheme.typography.headlineSmall,
                contentDescription = stringResource(LocR.string.discount_pay) + ": $pay",
            )
            Text(
                text = stringResource(LocR.string.discount_saved) + ": " + (save ?: ""),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SplitTool(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickToolsViewModel = viewModel(factory = QuickToolsViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = onBack)
        OmniSectionHeader(stringResource(LocR.string.tool_split))
        OutlinedTextField(
            value = ui.splitTotal,
            onValueChange = viewModel::setSplitTotal,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.split_total)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ui.splitPeople,
                onValueChange = viewModel::setSplitPeople,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(LocR.string.split_people)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = ui.splitTip,
                onValueChange = viewModel::setSplitTip,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(LocR.string.split_tip)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Spacer(Modifier.height(8.dp))
        ui.splitError?.let { ErrorBanner(message = it, modifier = Modifier.fillMaxWidth()) }
        val per = ui.splitPer
        val grand = ui.splitGrand
        if (per != null) {
            ResultText(
                text = per,
                style = MaterialTheme.typography.headlineSmall,
                contentDescription = stringResource(LocR.string.split_per_person) + ": $per",
            )
            grand?.let {
                Text(
                    text = stringResource(LocR.string.split_grand) + ": $it",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ----------------------------------------------------------- graph ----

@Composable
fun GraphTool(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GraphViewModel = viewModel(factory = GraphViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = onBack)
        OmniSectionHeader(stringResource(LocR.string.tool_graph))

        OutlinedTextField(
            value = ui.expr,
            onValueChange = viewModel::setExpr,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.graph_expr)) },
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ui.xMinText,
                onValueChange = viewModel::setXMin,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = ui.xMaxText,
                onValueChange = viewModel::setXMax,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("max") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Spacer(Modifier.height(6.dp))
        Button(onClick = viewModel::plot, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(LocR.string.graph_plot))
        }
        Spacer(Modifier.height(6.dp))

        ui.error?.let {
            if (it.isNotBlank()) {
                ErrorBanner(message = it, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
            }
        }

        GraphCanvas(samples = ui.samples, revision = ui.revision)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "${ui.plotted} " + stringResource(LocR.string.graph_points),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${ui.skipped} " + stringResource(LocR.string.graph_skipped),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GraphCanvas(samples: List<Pair<Double, Double>?>, revision: Int) {
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val bgColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = 8.dp),
    ) {
        drawRect(color = bgColor)
        if (samples.isEmpty() || samples.none { it != null }) return@Canvas

        val xs = samples.mapNotNull { it?.first }
        val ys = samples.mapNotNull { it?.second }
        val rawXMin = xs.min()
        val rawXMax = xs.max()
        var yMin = ys.min()
        var yMax = ys.max()
        if (yMin == yMax) {
            yMin -= 1.0
            yMax += 1.0
        }
        // Keep x span sane when min/max collapsed
        val xMin = rawXMin
        val xMax = if (rawXMax > rawXMin) rawXMax else rawXMin + 1.0

        fun px(x: Double) = ((x - xMin) / (xMax - xMin) * size.width).toFloat()
        fun py(y: Double) = (size.height - ((y - yMin) / (yMax - yMin) * size.height)).toFloat()

        // Grid + axes
        for (i in 1 until 4) {
            val gy = size.height * i / 4f
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            val gx = size.width * i / 4f
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
        }
        if (yMin < 0 && yMax > 0) {
            val zeroY = py(0.0)
            drawLine(axisColor, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 2f)
        }
        if (xMin < 0 && xMax > 0) {
            val zeroX = px(0.0)
            drawLine(axisColor, Offset(zeroX, 0f), Offset(zeroX, size.height), strokeWidth = 2f)
        }

        // Polyline with breaks at nulls
        val path = Path()
        var started = false
        for (sample in samples) {
            if (sample == null) {
                started = false
                continue
            }
            val cx = px(sample.first)
            val cy = py(sample.second)
            if (!started) {
                path.moveTo(cx, cy)
                started = true
            } else {
                path.lineTo(cx, cy)
            }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}
