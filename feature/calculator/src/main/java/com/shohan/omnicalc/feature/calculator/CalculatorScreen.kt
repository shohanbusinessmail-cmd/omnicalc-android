package com.shohan.omnicalc.feature.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohan.omnicalc.core.designsystem.CalcKey
import com.shohan.omnicalc.core.designsystem.CalcKeyOutlined
import com.shohan.omnicalc.core.designsystem.CalcKeyVariant
import com.shohan.omnicalc.core.designsystem.ErrorBanner
import com.shohan.omnicalc.core.designsystem.ResultText
import com.shohan.omnicalc.core.localization.R as LocR

/**
 * Standard calculator (spec §5): editable expression, live preview,
 * memory functions, copy/share, accessible keypad labels.
 *
 * @param pendingExpression expression handed over from history/library
 * @param onExpressionConsumed one-shot callback after loading it
 * @param scientific when true the host (scientific screen) adds its own key
 *   rows above the shared pad; behavior here is identical.
 */
@Composable
fun CalculatorScreen(
    pendingExpression: String?,
    onExpressionConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    scientificContent: (@Composable () -> Unit)? = null,
) {
    val vm: CalculatorViewModel = viewModel(factory = CalculatorViewModel.factory())
    val ui by vm.ui.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(pendingExpression) {
        if (!pendingExpression.isNullOrEmpty()) {
            vm.reuseExpression(pendingExpression)
            onExpressionConsumed()
        }
    }

    val maybeHaptic: () -> Unit = {
        if (ui.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val shown = ui.committedResult ?: ui.preview

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        // ---- Display ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
        ) {
            if (ui.errorMessage != null) {
                ErrorBanner(message = ui.errorMessage!!, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (ui.memory != null) {
                    Text(
                        text = "M",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                if (shown != null) {
                    // Compact copy/share affordances live in the status line so
                    // the result itself keeps every vertical pixel.
                    androidx.compose.material3.IconButton(
                        onClick = { maybeHaptic(); clipboard.setText(AnnotatedString(shown)) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(LocR.string.action_copy),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = {
                            maybeHaptic()
                            shareCalculation(context, ui.field.text, shown)
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(LocR.string.action_share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Editable expression (cursor preserved; hardware keyboards type here).
            BasicTextField(
                value = ui.field,
                onValueChange = { vm.onFieldChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.End,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { vm.evaluate() }),
                maxLines = 3,
            )

            if (shown != null) {
                Spacer(Modifier.height(4.dp))
                ResultText(
                    text = shown,
                    style = MaterialTheme.typography.displaySmall,
                    contentDescription = stringResource(LocR.string.label_result) + ": " + shown,
                )
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }

        // ---- Memory row (spec §4.6) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CalcKeyOutlined("MC", stringResource(LocR.string.key_desc_memory_clear), { maybeHaptic(); vm.memoryClear() }, Modifier.weight(1f), enabled = ui.memory != null)
            CalcKeyOutlined("MR", stringResource(LocR.string.key_desc_memory_recall), { maybeHaptic(); vm.memoryRecall() }, Modifier.weight(1f), enabled = ui.memory != null)
            CalcKeyOutlined("M+", stringResource(LocR.string.key_desc_memory_add), { maybeHaptic(); vm.memoryAdd() }, Modifier.weight(1f))
            CalcKeyOutlined("M−", stringResource(LocR.string.key_desc_memory_subtract), { maybeHaptic(); vm.memorySubtract() }, Modifier.weight(1f))
            CalcKeyOutlined("MS", stringResource(LocR.string.key_desc_memory_store), { maybeHaptic(); vm.memoryStore() }, Modifier.weight(1f))
        }

        // ---- Scientific rows (hosted above standard pad when present) ----
        if (scientificContent != null) {
            Spacer(Modifier.height(4.dp))
            scientificContent()
        }

        // ---- Standard keypad ----
        // With the scientific rows on screen, keys shrink to the a11y-legal
        // 48dp so the display/result area keeps real estate (spec §6.3 UX).
        val padHeight: Dp = if (scientificContent != null) 48.dp else 56.dp
        Spacer(Modifier.height(4.dp))
        KeypadRow {
            CalcKey("AC", stringResource(LocR.string.key_desc_clear), { maybeHaptic(); vm.clearAll() }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.DESTRUCTIVE, fontSize = MaterialTheme.typography.titleLarge.fontSize)
            CalcKey("⌫", stringResource(LocR.string.key_desc_backspace), { maybeHaptic(); vm.backspace() }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.FUNCTION)
            CalcKey("(", stringResource(LocR.string.key_desc_open_paren), { maybeHaptic(); vm.insert("(") }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.FUNCTION)
            CalcKey(")", stringResource(LocR.string.key_desc_close_paren), { maybeHaptic(); vm.insert(")") }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.FUNCTION)
            CalcKey("÷", stringResource(LocR.string.key_desc_divide), { maybeHaptic(); vm.insert("÷") }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.OPERATOR)
        }
        KeypadRow {
            CalcKey("7", "7", { maybeHaptic(); vm.insert("7") }, Modifier.weight(1f).height(padHeight))
            CalcKey("8", "8", { maybeHaptic(); vm.insert("8") }, Modifier.weight(1f).height(padHeight))
            CalcKey("9", "9", { maybeHaptic(); vm.insert("9") }, Modifier.weight(1f).height(padHeight))
            CalcKey("×", stringResource(LocR.string.key_desc_multiply), { maybeHaptic(); vm.insert("×") }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.OPERATOR)
            CalcKey("−", stringResource(LocR.string.key_desc_subtract), { maybeHaptic(); vm.insert("−") }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.OPERATOR)
        }
        KeypadRow {
            CalcKey("4", "4", { maybeHaptic(); vm.insert("4") }, Modifier.weight(1f).height(padHeight))
            CalcKey("5", "5", { maybeHaptic(); vm.insert("5") }, Modifier.weight(1f).height(padHeight))
            CalcKey("6", "6", { maybeHaptic(); vm.insert("6") }, Modifier.weight(1f).height(padHeight))
            CalcKey("+", stringResource(LocR.string.key_desc_add), { maybeHaptic(); vm.insert("+") }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.OPERATOR)
            CalcKey("±", stringResource(LocR.string.key_desc_toggle_sign), { maybeHaptic(); vm.toggleSign() }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.FUNCTION)
        }
        KeypadRow {
            CalcKey("1", "1", { maybeHaptic(); vm.insert("1") }, Modifier.weight(1f).height(padHeight))
            CalcKey("2", "2", { maybeHaptic(); vm.insert("2") }, Modifier.weight(1f).height(padHeight))
            CalcKey("3", "3", { maybeHaptic(); vm.insert("3") }, Modifier.weight(1f).height(padHeight))
            CalcKey(".", stringResource(LocR.string.key_desc_decimal), { maybeHaptic(); vm.insert(".") }, Modifier.weight(1f).height(padHeight))
            CalcKey("%", stringResource(LocR.string.key_desc_percent), { maybeHaptic(); vm.insert("%") }, Modifier.weight(1f).height(padHeight), variant = CalcKeyVariant.OPERATOR)
        }
        KeypadRow {
            CalcKey(
                "0", "0",
                { maybeHaptic(); vm.insert("0") },
                Modifier.weight(2f).height(padHeight),
            )
            CalcKey(
                "=",
                stringResource(LocR.string.key_desc_equals),
                { maybeHaptic(); vm.evaluate() },
                Modifier.weight(2f).height(padHeight),
                variant = CalcKeyVariant.EQUALS,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun KeypadRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

private typealias RowScope = androidx.compose.foundation.layout.RowScope

/** Shares `expression = result` via the system share sheet (spec §15.3). */
private fun shareCalculation(context: android.content.Context, expression: String, result: String) {
    val text = if (expression.isNotBlank()) "$expression = $result" else result
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(intent, null))
}
