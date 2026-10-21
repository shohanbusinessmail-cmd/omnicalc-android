package com.shohan.omnicalc.feature.scientific

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohan.omnicalc.core.common.AngleMode
import com.shohan.omnicalc.core.designsystem.CalcKeyOutlined
import com.shohan.omnicalc.feature.calculator.CalculatorScreen
import com.shohan.omnicalc.feature.calculator.CalculatorViewModel
import com.shohan.omnicalc.core.localization.R as LocR

/**
 * Scientific calculator (spec §6): the standard calculator shell plus
 * trigonometry/logarithm key rows and a persisted DEG/RAD/GRAD toggle.
 *
 * Shares one ViewModel instance with [CalculatorScreen] (same nav-entry
 * ViewModelStore) — mode, memory, expression and history behave identically.
 */
@Composable
fun ScientificScreen(
    pendingExpression: String?,
    onExpressionConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: CalculatorViewModel = viewModel(factory = CalculatorViewModel.factory())

    CalculatorScreen(
        pendingExpression = pendingExpression,
        onExpressionConsumed = onExpressionConsumed,
        modifier = modifier,
        scientificContent = { ScientificKeys(vm) },
    )
}

@Composable
private fun ScientificKeys(vm: CalculatorViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // Angle mode indicator + toggle (§6.1): DEG → RAD → GRAD.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        TextButton(onClick = vm::cycleAngleMode) {
            val label = when (ui.angleModeLabel) {
                "RAD" -> stringResource(LocR.string.angle_mode_rad)
                "GRAD" -> stringResource(LocR.string.angle_mode_grad)
                else -> stringResource(LocR.string.angle_mode_deg)
            }
            Text(
                text = "$label ▾",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    SciRow {
        CalcKeyOutlined("sin", stringResource(LocR.string.key_desc_sin), { vm.insert("sin(") }, Modifier.weight(1f))
        CalcKeyOutlined("cos", stringResource(LocR.string.key_desc_cos), { vm.insert("cos(") }, Modifier.weight(1f))
        CalcKeyOutlined("tan", stringResource(LocR.string.key_desc_tan), { vm.insert("tan(") }, Modifier.weight(1f))
        CalcKeyOutlined("π", stringResource(LocR.string.key_desc_pi), { vm.insert("π") }, Modifier.weight(1f))
        CalcKeyOutlined("e", stringResource(LocR.string.key_desc_euler), { vm.insert("e") }, Modifier.weight(1f))
    }
    SciRow {
        CalcKeyOutlined("sin⁻¹", stringResource(LocR.string.key_desc_asin), { vm.insert("asin(") }, Modifier.weight(1f))
        CalcKeyOutlined("cos⁻¹", stringResource(LocR.string.key_desc_acos), { vm.insert("acos(") }, Modifier.weight(1f))
        CalcKeyOutlined("tan⁻¹", stringResource(LocR.string.key_desc_atan), { vm.insert("atan(") }, Modifier.weight(1f))
        CalcKeyOutlined("√", stringResource(LocR.string.key_desc_sqrt), { vm.insert("√(") }, Modifier.weight(1f))
        CalcKeyOutlined("x²", stringResource(LocR.string.key_desc_power), { vm.insert("^2") }, Modifier.weight(1f))
    }
    SciRow {
        CalcKeyOutlined("ln", stringResource(LocR.string.key_desc_ln), { vm.insert("ln(") }, Modifier.weight(1f))
        CalcKeyOutlined("log", stringResource(LocR.string.key_desc_log), { vm.insert("log(") }, Modifier.weight(1f))
        CalcKeyOutlined("xʸ", stringResource(LocR.string.key_desc_power), { vm.insert("^") }, Modifier.weight(1f))
        CalcKeyOutlined("n!", stringResource(LocR.string.key_desc_factorial), { vm.insert("!") }, Modifier.weight(1f))
        CalcKeyOutlined("|x|", stringResource(LocR.string.key_desc_abs), { vm.insert("abs(") }, Modifier.weight(1f))
    }
    SciRow {
        CalcKeyOutlined("eˣ", stringResource(LocR.string.key_desc_exp), { vm.insert("exp(") }, Modifier.weight(1f))
        CalcKeyOutlined("10ˣ", stringResource(LocR.string.key_desc_power), { vm.insert("10^") }, Modifier.weight(1f))
        CalcKeyOutlined("1/x", stringResource(LocR.string.key_desc_inverse), { vm.insert("^-1") }, Modifier.weight(1f))
        CalcKeyOutlined("mod", stringResource(LocR.string.key_desc_divide), { vm.insert(" mod ") }, Modifier.weight(1f))
        CalcKeyOutlined("°", stringResource(LocR.string.key_desc_degree_suffix), { vm.insert("°") }, Modifier.weight(1f))
    }
}

@Composable
private fun SciRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
