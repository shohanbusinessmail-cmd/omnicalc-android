package com.shohan.omnicalc.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohan.omnicalc.core.common.AngleMode
import com.shohan.omnicalc.core.common.GroupingStyle
import com.shohan.omnicalc.core.common.LanguageMode
import com.shohan.omnicalc.core.common.NumeralStyle
import com.shohan.omnicalc.core.common.PercentMode
import com.shohan.omnicalc.core.common.PrecisionMode
import com.shohan.omnicalc.core.common.ThemeMode
import com.shohan.omnicalc.core.designsystem.ErrorBanner
import com.shohan.omnicalc.core.designsystem.OmniCard
import com.shohan.omnicalc.core.designsystem.OmniDivider
import com.shohan.omnicalc.core.designsystem.OmniSectionHeader
import com.shohan.omnicalc.core.localization.R as LocR
import java.text.DateFormat
import java.util.Date

/**
 * Library destination (spec §15, §17): History + Favorites + Settings —
 * every control here performs a real, persisted action.
 *
 * @param onOpenAbout navigates to the About screen (host route)
 * @param onReuseExpression hands an expression back to the calculator
 * @param onLanguageChanged applies per-app locale immediately (host concern)
 */
@Composable
fun LibraryScreen(
    onOpenAbout: () -> Unit,
    onReuseExpression: (expression: String, result: String?) -> Unit,
    onLanguageChanged: (LanguageMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory()),
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(LocR.string.lib_tab_history),
        stringResource(LocR.string.lib_tab_favorites),
        stringResource(LocR.string.lib_tab_settings),
    )

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
        when (tab) {
            0 -> HistoryTab(viewModel, onReuseExpression, modifier)
            1 -> FavoritesTab(viewModel, onReuseExpression, modifier)
            2 -> SettingsTab(viewModel, onOpenAbout, onLanguageChanged, modifier)
        }
    }
}

// ---------------------------------------------------------------- history --

@Composable
private fun HistoryTab(
    vm: LibraryViewModel,
    onReuse: (String, String?) -> Unit,
    modifier: Modifier,
) {
    val history by vm.history.collectAsStateWithLifecycle()
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val favorites by vm.favoritePayloads.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(LocR.string.lib_search_history_hint)) },
            )
            IconButton(onClick = { confirmClear = true }, enabled = history.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(LocR.string.action_clear_history),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (history.isEmpty()) {
            EmptyState(
                if (query.isBlank()) stringResource(LocR.string.lib_history_empty)
                else stringResource(LocR.string.lib_no_results),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { entry ->
                    OmniCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onReuse(entry.expression, entry.resultPlain) },
                                ) {
                                    Text(
                                        text = entry.expression,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "= " + entry.resultPlain,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = dateFormat.format(Date(entry.createdAt)) +
                                            (entry.toolType.takeIf { it.isNotBlank() }?.let { "  ·  $it" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { vm.toggleCalculationFavorite(entry) }) {
                                    Icon(
                                        imageVector = if (entry.expression in favorites) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                        contentDescription = stringResource(
                                            if (entry.expression in favorites) LocR.string.action_unfavorite
                                            else LocR.string.action_favorite,
                                        ),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = { vm.deleteHistory(entry) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(LocR.string.lib_delete_entry),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(LocR.string.action_clear_history)) },
            text = { Text(stringResource(LocR.string.action_confirm_clear_history)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearHistory()
                }) { Text(stringResource(LocR.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(LocR.string.action_cancel))
                }
            },
        )
    }
}

// -------------------------------------------------------------- favorites --

@Composable
private fun FavoritesTab(
    vm: LibraryViewModel,
    onReuse: (String, String?) -> Unit,
    modifier: Modifier,
) {
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            EmptyState(stringResource(LocR.string.lib_favorites_empty))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(favorites, key = { it.id }) { fav ->
                    OmniCard(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        onClick = { onReuse(fav.payload, fav.subtitle) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fav.title, style = MaterialTheme.typography.bodyLarge)
                                fav.subtitle?.let {
                                    Text(
                                        text = "= $it",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            IconButton(onClick = { vm.removeFavorite(fav.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(LocR.string.action_delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- settings --

@Composable
private fun SettingsTab(
    vm: LibraryViewModel,
    onOpenAbout: () -> Unit,
    onLanguageChanged: (LanguageMode) -> Unit,
    modifier: Modifier,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OmniSectionHeader(stringResource(LocR.string.settings_section_appearance))
                DropdownSetting(
                    label = stringResource(LocR.string.setting_theme),
                    valueLabel = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> stringResource(LocR.string.option_system)
                        ThemeMode.LIGHT -> stringResource(LocR.string.option_light)
                        ThemeMode.DARK -> stringResource(LocR.string.option_dark)
                    },
                ) { onSelected ->
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_system)) }, onClick = { vm.setThemeMode(ThemeMode.SYSTEM); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_light)) }, onClick = { vm.setThemeMode(ThemeMode.LIGHT); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_dark)) }, onClick = { vm.setThemeMode(ThemeMode.DARK); onSelected() })
                }

                OmniDivider()
                OmniSectionHeader(stringResource(LocR.string.settings_section_language))
                DropdownSetting(
                    label = stringResource(LocR.string.setting_language),
                    valueLabel = when (settings.languageMode) {
                        LanguageMode.SYSTEM -> stringResource(LocR.string.option_system)
                        LanguageMode.ENGLISH -> stringResource(LocR.string.option_english)
                        LanguageMode.BENGALIC -> stringResource(LocR.string.option_bengali)
                    },
                ) { onSelected ->
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_system)) }, onClick = { vm.setLanguageMode(LanguageMode.SYSTEM); onLanguageChanged(LanguageMode.SYSTEM); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_english)) }, onClick = { vm.setLanguageMode(LanguageMode.ENGLISH); onLanguageChanged(LanguageMode.ENGLISH); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_bengali)) }, onClick = { vm.setLanguageMode(LanguageMode.BENGALIC); onLanguageChanged(LanguageMode.BENGALIC); onSelected() })
                }

                OmniDivider()
                OmniSectionHeader(stringResource(LocR.string.settings_section_numbers))
                DropdownSetting(
                    label = stringResource(LocR.string.setting_numerals),
                    valueLabel = when (settings.numeralStyle) {
                        NumeralStyle.WESTERN -> stringResource(LocR.string.option_western)
                        NumeralStyle.BENGALI -> stringResource(LocR.string.option_bengali_numerals)
                    },
                ) { onSelected ->
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_western)) }, onClick = { vm.setNumeralStyle(NumeralStyle.WESTERN); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_bengali_numerals)) }, onClick = { vm.setNumeralStyle(NumeralStyle.BENGALI); onSelected() })
                }
                DropdownSetting(
                    label = stringResource(LocR.string.setting_grouping),
                    valueLabel = when (settings.groupingStyle) {
                        GroupingStyle.INTERNATIONAL -> stringResource(LocR.string.option_grouping_international)
                        GroupingStyle.SOUTH_ASIAN -> stringResource(LocR.string.option_grouping_south_asian)
                    },
                ) { onSelected ->
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_grouping_international)) }, onClick = { vm.setGroupingStyle(GroupingStyle.INTERNATIONAL); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_grouping_south_asian)) }, onClick = { vm.setGroupingStyle(GroupingStyle.SOUTH_ASIAN); onSelected() })
                }
                DropdownSetting(
                    label = stringResource(LocR.string.setting_precision),
                    valueLabel = when (settings.precisionMode) {
                        PrecisionMode.AUTO -> stringResource(LocR.string.option_precision_auto)
                        PrecisionMode.FIXED -> stringResource(LocR.string.option_precision_fixed)
                    },
                ) { onSelected ->
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_precision_auto)) }, onClick = { vm.setPrecisionMode(PrecisionMode.AUTO); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_precision_fixed)) }, onClick = { vm.setPrecisionMode(PrecisionMode.FIXED); onSelected() })
                }
                if (settings.precisionMode == PrecisionMode.FIXED) {
                    DropdownSetting(
                        label = stringResource(LocR.string.setting_decimal_places),
                        valueLabel = settings.decimalPlaces.toString(),
                    ) { onSelected ->
                        (0..12).forEach { n ->
                            DropdownMenuItem(
                                text = { Text(n.toString()) },
                                onClick = { vm.setDecimalPlaces(n); onSelected() },
                            )
                        }
                    }
                }

                OmniDivider()
                OmniSectionHeader(stringResource(LocR.string.settings_section_calculation))
                DropdownSetting(
                    label = stringResource(LocR.string.setting_angle),
                    valueLabel = when (settings.angleMode) {
                        AngleMode.DEG -> stringResource(LocR.string.angle_mode_deg)
                        AngleMode.RAD -> stringResource(LocR.string.angle_mode_rad)
                        AngleMode.GRAD -> stringResource(LocR.string.angle_mode_grad)
                    },
                ) { onSelected ->
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.angle_mode_deg)) }, onClick = { vm.setAngleMode(AngleMode.DEG); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.angle_mode_rad)) }, onClick = { vm.setAngleMode(AngleMode.RAD); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.angle_mode_grad)) }, onClick = { vm.setAngleMode(AngleMode.GRAD); onSelected() })
                }
                DropdownSetting(
                    label = stringResource(LocR.string.setting_percent),
                    valueLabel = when (settings.percentMode) {
                        PercentMode.STANDARD -> stringResource(LocR.string.option_percent_standard)
                        PercentMode.EXPLICIT -> stringResource(LocR.string.option_percent_explicit)
                    },
                ) { onSelected ->
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_percent_standard)) }, onClick = { vm.setPercentMode(PercentMode.STANDARD); onSelected() })
                    DropdownMenuItem(text = { Text(stringResource(LocR.string.option_percent_explicit)) }, onClick = { vm.setPercentMode(PercentMode.EXPLICIT); onSelected() })
                }

                OmniDivider()
                OmniSectionHeader(stringResource(LocR.string.settings_section_feedback))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(LocR.string.setting_haptics),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(checked = settings.hapticsEnabled, onCheckedChange = vm::setHaptics)
                }

                OmniDivider()
                OmniSectionHeader(stringResource(LocR.string.settings_section_data))
                TextButton(onClick = { confirmReset = true }) {
                    Text(stringResource(LocR.string.settings_reset), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onOpenAbout) {
                    Text(stringResource(LocR.string.settings_about))
                }
                Text(
                    text = stringResource(LocR.string.settings_saved_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(LocR.string.settings_reset)) },
            text = { Text(stringResource(LocR.string.settings_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.resetPreferences()
                }) { Text(stringResource(LocR.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(LocR.string.action_cancel))
                }
            },
        )
    }
}

// ------------------------------------------------------------------ misc --

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Label + dropdown selector. [content] receives an `onSelected` callback to
 * close the menu after an item is chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(
    label: String,
    valueLabel: String,
    content: @Composable (() -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = valueLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            content { expanded = false }
        }
    }
}
