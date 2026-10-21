package com.shohan.omnicalc.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shohan.omnicalc.core.designsystem.ErrorBanner
import com.shohan.omnicalc.core.designsystem.OmniSectionHeader
import com.shohan.omnicalc.core.designsystem.ResultText
import com.shohan.omnicalc.core.math.UnitCatalog
import com.shohan.omnicalc.core.math.UnitCategory
import com.shohan.omnicalc.core.localization.R as LocR
import java.util.Calendar
import java.util.TimeZone

// ---------------------------------------------------------------- converter --

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterTool(
    ui: ToolsViewModel.UiState,
    viewModel: ToolsViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = { viewModel.open(ToolsViewModel.Tool.HOME) })
        OmniSectionHeader(stringResource(LocR.string.tool_unit_converter))

        // Category selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            UnitCategory.entries.forEach { cat ->
                val label = categoryName(cat, isBn())
                FilterChip(
                    selected = ui.category == cat,
                    onClick = { viewModel.setCategory(cat) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        val units = UnitCatalog.units(ui.category)
        val from = units.firstOrNull { it.id == ui.fromId } ?: units[0]
        val to = units.firstOrNull { it.id == ui.toId } ?: units[1]

        OutlinedTextField(
            value = ui.amountText,
            onValueChange = viewModel::setAmount,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.converter_amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            trailingIcon = {
                TextButton(onClick = viewModel::commitConversion) {
                    Text(stringResource(LocR.string.converter_save))
                }
            },
        )
        Spacer(Modifier.height(8.dp))

        UnitPickerRow(
            label = stringResource(LocR.string.converter_from),
            units = units,
            selected = from,
            isBn = isBn(),
            onSelect = viewModel::setFromUnit,
        )
        UnitPickerRow(
            label = stringResource(LocR.string.converter_to),
            units = units,
            selected = to,
            isBn = isBn(),
            onSelect = viewModel::setToUnit,
        )

        OutlinedButton(onClick = viewModel::swapUnits, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(LocR.string.converter_swap))
        }
        Spacer(Modifier.height(12.dp))

        val convertError = ui.convertError
        if (convertError != null) {
            ErrorBanner(message = convertError, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        val converted = ui.convertedDisplay
        if (converted != null) {
            ResultText(
                text = "$converted ${to.symbol}",
                style = MaterialTheme.typography.headlineSmall,
                contentDescription = stringResource(LocR.string.formula_result) + ": $converted ${to.symbol}",
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun UnitPickerRow(
    label: String,
    units: List<com.shohan.omnicalc.core.math.UnitDef>,
    selected: com.shohan.omnicalc.core.math.UnitDef,
    isBn: Boolean,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(72.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            units.forEach { u ->
                FilterChip(
                    selected = u.id == selected.id,
                    onClick = { onSelect(u.id) },
                    label = {
                        Text(
                            text = u.symbol,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
            }
        }
    }
    val note = selected.note
    if (note != null) {
        Text(
            text = note,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -------------------------------------------------------------- programmer --

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammerTool(
    ui: ToolsViewModel.UiState,
    viewModel: ToolsViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = { viewModel.open(ToolsViewModel.Tool.HOME) })
        OmniSectionHeader(stringResource(LocR.string.tool_programmer))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolsViewModel.Base.entries.forEach { base ->
                FilterChip(
                    selected = ui.base == base,
                    onClick = { viewModel.setBase(base) },
                    label = { Text(base.label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = ui.valueA,
            onValueChange = viewModel::setValueA,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.programmer_a)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (ui.base == ToolsViewModel.Base.HEX) KeyboardType.Text else KeyboardType.Number,
            ),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = ui.valueB,
            onValueChange = viewModel::setValueB,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(LocR.string.programmer_b)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (ui.base == ToolsViewModel.Base.HEX) KeyboardType.Text else KeyboardType.Number,
            ),
        )
        Spacer(Modifier.height(8.dp))

        val bitError = ui.bitError
        if (bitError != null) {
            ErrorBanner(message = bitError, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
        }

        // Always-visible base readouts (operand A in every base).
        if (ui.bitResultByBase.isNotEmpty()) {
            ToolsViewModel.Base.entries.forEach { base ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = base.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = ui.bitResultByBase[base] ?: "—",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        val opRows = listOf(
            listOf(
                ToolsViewModel.BitOp.AND to stringResource(LocR.string.programmer_and),
                ToolsViewModel.BitOp.OR to stringResource(LocR.string.programmer_or),
            ),
            listOf(
                ToolsViewModel.BitOp.XOR to stringResource(LocR.string.programmer_xor),
                ToolsViewModel.BitOp.NOT to stringResource(LocR.string.programmer_not),
            ),
            listOf(
                ToolsViewModel.BitOp.SHL to stringResource(LocR.string.programmer_shl),
                ToolsViewModel.BitOp.SHR to stringResource(LocR.string.programmer_shr),
            ),
            listOf(
                ToolsViewModel.BitOp.ADD to stringResource(LocR.string.programmer_add),
                ToolsViewModel.BitOp.SUB to stringResource(LocR.string.programmer_sub),
            ),
            listOf(
                ToolsViewModel.BitOp.MUL to stringResource(LocR.string.programmer_mul),
                ToolsViewModel.BitOp.DIV to stringResource(LocR.string.programmer_div),
            ),
        )
        opRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { (op, label) ->
                    OutlinedButton(
                        onClick = { viewModel.applyOp(op) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        val bitDisplay = ui.bitResultDisplay
        if (bitDisplay != null) {
            Spacer(Modifier.height(8.dp))
            ResultText(
                text = "${ui.base.label}: $bitDisplay",
                style = MaterialTheme.typography.titleLarge,
                contentDescription = "${ui.base.label}: $bitDisplay",
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ------------------------------------------------------------ date & time --

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTool(
    ui: ToolsViewModel.UiState,
    viewModel: ToolsViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        ToolBackButton(onBack = { viewModel.open(ToolsViewModel.Tool.HOME) })
        OmniSectionHeader(stringResource(LocR.string.tool_datetime))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = ui.dateMode == ToolsViewModel.DateMode.DIFF,
                onClick = { viewModel.setDateMode(ToolsViewModel.DateMode.DIFF) },
                label = { Text(stringResource(LocR.string.date_diff_result)) },
            )
            FilterChip(
                selected = ui.dateMode == ToolsViewModel.DateMode.SHIFT,
                onClick = { viewModel.setDateMode(ToolsViewModel.DateMode.SHIFT) },
                label = { Text(stringResource(LocR.string.date_add_days)) },
            )
            FilterChip(
                selected = ui.dateMode == ToolsViewModel.DateMode.AGE,
                onClick = { viewModel.setDateMode(ToolsViewModel.DateMode.AGE) },
                label = { Text(stringResource(LocR.string.date_age_result)) },
            )
        }
        Spacer(Modifier.height(8.dp))

        when (ui.dateMode) {
            ToolsViewModel.DateMode.DIFF -> {
                DateField(stringResource(LocR.string.date_start), ui.startDate) { viewModel.setStartDate(it) }
                Spacer(Modifier.height(6.dp))
                DateField(stringResource(LocR.string.date_end), ui.endDate) { viewModel.setEndDate(it) }
            }
            ToolsViewModel.DateMode.SHIFT -> {
                DateField(stringResource(LocR.string.date_start), ui.shiftDate) { viewModel.setShiftDate(it) }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = ui.shiftDays,
                    onValueChange = viewModel::setShiftDays,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(LocR.string.date_add_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            ToolsViewModel.DateMode.AGE -> {
                DateField(stringResource(LocR.string.date_birth), ui.birthDate) { viewModel.setBirthDate(it) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::computeDate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(LocR.string.formula_compute))
        }
        Spacer(Modifier.height(8.dp))

        val dateError = ui.dateError
        if (dateError != null) {
            ErrorBanner(message = dateError, modifier = Modifier.fillMaxWidth())
        }
        val dateResult = ui.dateResult
        if (dateResult != null) {
            ResultText(
                text = dateResult,
                style = MaterialTheme.typography.headlineSmall,
                contentDescription = stringResource(LocR.string.formula_result) + ": " + dateResult,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, isoValue: String, onPicked: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = isoValue,
        onValueChange = { text ->
            if (text.length <= 10) onPicked(text)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        trailingIcon = {
            TextButton(onClick = { showPicker = true }) {
                Text(stringResource(LocR.string.date_pick))
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )

    if (showPicker) {
        val initialMillis = isoToUtcMillis(isoValue)
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPicked(utcMillisToIso(it)) }
                    showPicker = false
                }) { Text(stringResource(LocR.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(LocR.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

// ------------------------------------------------------------------ utils --

@Composable
private fun isBn(): Boolean =
    androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0).language == "bn"

private fun isoToUtcMillis(iso: String): Long {
    val parts = iso.trim().split('-')
    val y = parts.getOrNull(0)?.toIntOrNull() ?: 1970
    val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
    val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(y, m.coerceIn(0, 11), d.coerceIn(1, 31), 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun utcMillisToIso(millis: Long): String {
    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return "%04d-%02d-%02d".format(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH),
    )
}

private fun categoryName(category: UnitCategory, isBn: Boolean): String = if (isBn) {
    when (category) {
        UnitCategory.LENGTH -> "দৈর্ঘ্য"
        UnitCategory.MASS -> "ভর"
        UnitCategory.VOLUME -> "আয়তন"
        UnitCategory.AREA -> "ক্ষেত্রফল"
        UnitCategory.TEMPERATURE -> "তাপমাত্রা"
        UnitCategory.SPEED -> "গতি"
        UnitCategory.TIME -> "সময়"
        UnitCategory.DATA -> "ডেটা"
    }
} else {
    when (category) {
        UnitCategory.LENGTH -> "Length"
        UnitCategory.MASS -> "Mass"
        UnitCategory.VOLUME -> "Volume"
        UnitCategory.AREA -> "Area"
        UnitCategory.TEMPERATURE -> "Temp"
        UnitCategory.SPEED -> "Speed"
        UnitCategory.TIME -> "Time"
        UnitCategory.DATA -> "Data"
    }
}
