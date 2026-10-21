package com.shohan.omnicalc.feature.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohan.omnicalc.core.designsystem.OmniCard
import com.shohan.omnicalc.core.designsystem.OmniSectionHeader
import com.shohan.omnicalc.core.localization.R as LocR

/**
 * Tools destination (spec §11–§14). Home lists only fully working tools —
 * each opens a real sub-screen backed by [ToolsViewModel].
 */
@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    viewModel: ToolsViewModel = viewModel(factory = ToolsViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    when (ui.tool) {
        ToolsViewModel.Tool.HOME -> ToolsHome(ui = ui, onOpen = viewModel::open, modifier = modifier)
        ToolsViewModel.Tool.CONVERTER -> ConverterTool(ui = ui, viewModel = viewModel, modifier = modifier)
        ToolsViewModel.Tool.PROGRAMMER -> ProgrammerTool(ui = ui, viewModel = viewModel, modifier = modifier)
        ToolsViewModel.Tool.DATE -> DateTool(ui = ui, viewModel = viewModel, modifier = modifier)
        ToolsViewModel.Tool.LAND -> LandTool(modifier = modifier)
        ToolsViewModel.Tool.VAT -> VatTool(modifier = modifier)
        ToolsViewModel.Tool.SALARY -> SalaryTool(modifier = modifier)
        ToolsViewModel.Tool.CURRENCY -> CurrencyTool(modifier = modifier)
        ToolsViewModel.Tool.GRAPH -> GraphTool(modifier = modifier)
    }
}

@Composable
private fun ToolsHome(
    ui: ToolsViewModel.UiState,
    onOpen: (ToolsViewModel.Tool) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        OmniSectionHeader(stringResource(LocR.string.tools_home_title))

        ToolCard(
            title = stringResource(LocR.string.tool_unit_converter),
            subtitle = stringResource(LocR.string.tool_unit_converter_desc),
            onClick = { onOpen(ToolsViewModel.Tool.CONVERTER) },
        )
        ToolCard(
            title = stringResource(LocR.string.tool_programmer),
            subtitle = stringResource(LocR.string.tool_programmer_desc),
            onClick = { onOpen(ToolsViewModel.Tool.PROGRAMMER) },
        )
        ToolCard(
            title = stringResource(LocR.string.tool_datetime),
            subtitle = stringResource(LocR.string.tool_datetime_desc),
            onClick = { onOpen(ToolsViewModel.Tool.DATE) },
        )
        ToolCard(
            title = stringResource(LocR.string.tool_land),
            subtitle = stringResource(LocR.string.tool_land_desc),
            onClick = { onOpen(ToolsViewModel.Tool.LAND) },
        )
        ToolCard(
            title = stringResource(LocR.string.tool_salary),
            subtitle = stringResource(LocR.string.tool_salary_desc),
            onClick = { onOpen(ToolsViewModel.Tool.SALARY) },
        )
        ToolCard(
            title = stringResource(LocR.string.tool_vat),
            subtitle = stringResource(LocR.string.tool_vat_desc),
            onClick = { onOpen(ToolsViewModel.Tool.VAT) },
        )
        ToolCard(
            title = stringResource(LocR.string.tool_currency),
            subtitle = stringResource(LocR.string.tool_currency_desc),
            onClick = { onOpen(ToolsViewModel.Tool.CURRENCY) },
        )
        ToolCard(
            title = stringResource(LocR.string.tool_graph),
            subtitle = stringResource(LocR.string.tool_graph_desc),
            onClick = { onOpen(ToolsViewModel.Tool.GRAPH) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, onClick: () -> Unit) {
    OmniCard(modifier = Modifier.padding(vertical = 4.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Back affordance shared by all tool sub-screens. */
@Composable
internal fun ToolBackButton(onBack: () -> Unit) {
    Text(
        text = "‹ " + stringResource(LocR.string.tools_home_title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable(onClick = onBack)
            .padding(12.dp),
    )
}
