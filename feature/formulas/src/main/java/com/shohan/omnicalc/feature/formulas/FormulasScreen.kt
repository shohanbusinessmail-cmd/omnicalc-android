package com.shohan.omnicalc.feature.formulas

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohan.omnicalc.core.designsystem.ErrorBanner
import com.shohan.omnicalc.core.designsystem.OmniCard
import com.shohan.omnicalc.core.designsystem.OmniDivider
import com.shohan.omnicalc.core.designsystem.ResultText
import com.shohan.omnicalc.core.localization.R as LocR

/**
 * Formula catalog (spec §9): searchable, category-filtered list; expanding a
 * formula reveals its bilingual explanation, engine expression, variable
 * inputs, a live computation through the shared engine, copy + favorite.
 */
@Composable
fun FormulasScreen(
    pendingFormulaId: String?,
    onPendingConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FormulasViewModel = viewModel(factory = FormulasViewModel.factory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val isBn = LocalConfiguration.current.locales.get(0).language == "bn"

    LaunchedEffect(pendingFormulaId) {
        if (!pendingFormulaId.isNullOrBlank()) {
            viewModel.expand(pendingFormulaId)
            onPendingConsumed()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = ui.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            singleLine = true,
            label = { Text(stringResource(LocR.string.formula_search_hint)) },
        )

        // Category filter — stable M3 buttons (no experimental chip APIs).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CategoryButton(
                label = stringResource(LocR.string.formula_all),
                selected = ui.category == null,
                onClick = { viewModel.setCategory(null) },
            )
            FormulaCatalog.categories.forEach { cat ->
                CategoryButton(
                    label = categoryLabel(cat, isBn),
                    selected = ui.category == cat,
                    onClick = { viewModel.setCategory(cat) },
                )
            }
        }

        val query = ui.query.trim()
        val filtered = FormulaCatalog.all.filter { f ->
            (ui.category == null || f.category == ui.category) &&
                (query.isEmpty() ||
                    f.nameEn.contains(query, ignoreCase = true) ||
                    f.nameBn.contains(query) ||
                    f.descriptionEn.contains(query, ignoreCase = true) ||
                    f.descriptionBn.contains(query))
        }

        if (filtered.isEmpty()) {
            Text(
                text = stringResource(LocR.string.formula_no_matches),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { formula ->
                    FormulaCard(
                        formula = formula,
                        isBn = isBn,
                        ui = ui,
                        viewModel = viewModel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FormulaCard(
    formula: Formula,
    isBn: Boolean,
    ui: FormulasViewModel.UiState,
    viewModel: FormulasViewModel,
    modifier: Modifier = Modifier,
) {
    val expanded = ui.expandedId == formula.id
    val clipboard = LocalClipboardManager.current
    val computed = ui.results[formula.id]
    val error = ui.errors[formula.id]
    val name = if (isBn) formula.nameBn else formula.nameEn

    OmniCard(modifier = modifier, onClick = { viewModel.expand(if (expanded) null else formula.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (formula.id in ui.favoriteIds) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = stringResource(LocR.string.action_unfavorite),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { viewModel.toggleFavorite(formula) }) {
                Icon(
                    imageVector = if (formula.id in ui.favoriteIds) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(
                        if (formula.id in ui.favoriteIds) LocR.string.action_unfavorite
                        else LocR.string.action_favorite,
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) LocR.string.formula_collapse else LocR.string.formula_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isBn) formula.descriptionBn else formula.descriptionEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formula.expr,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            formula.vars.forEach { v ->
                OutlinedTextField(
                    value = ui.inputs[formula.id]?.get(v.name).orEmpty(),
                    onValueChange = { viewModel.setInput(formula.id, v.name, it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isBn) v.labelBn else v.labelEn) },
                    placeholder = { Text(if (isBn) v.hintBn else v.hintEn) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(6.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { viewModel.compute(formula) }) {
                    Text(stringResource(LocR.string.formula_compute))
                }
                if (computed != null) {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(computed.display)) }) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(LocR.string.action_copy),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(6.dp))
                ErrorBanner(message = error, modifier = Modifier.fillMaxWidth())
            }

            if (computed != null) {
                Spacer(Modifier.height(6.dp))
                ResultText(
                    text = computed.display,
                    style = MaterialTheme.typography.headlineSmall,
                    contentDescription = stringResource(LocR.string.formula_result) + ": " + computed.display,
                )
            }

            Spacer(Modifier.height(8.dp))
            OmniDivider()
            Spacer(Modifier.height(6.dp))
            SectionText(
                label = stringResource(LocR.string.formula_howto),
                body = if (isBn) formula.howToBn else formula.howToEn,
            )
            SectionText(
                label = stringResource(LocR.string.formula_usedin),
                body = if (isBn) formula.usedInBn else formula.usedInEn,
            )
        }
    }
}

@Composable
private fun SectionText(label: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
    }
}

private fun categoryLabel(category: FormulaCategory, isBn: Boolean): String = if (isBn) {
    when (category) {
        FormulaCategory.GEOMETRY -> "জ্যামিতি"
        FormulaCategory.ALGEBRA -> "বীজগণিত"
        FormulaCategory.FINANCE -> "অর্থায়ন"
        FormulaCategory.PHYSICS -> "পদার্থবিজ্ঞান"
    }
} else {
    when (category) {
        FormulaCategory.GEOMETRY -> "Geometry"
        FormulaCategory.ALGEBRA -> "Algebra"
        FormulaCategory.FINANCE -> "Finance"
        FormulaCategory.PHYSICS -> "Physics"
    }
}
