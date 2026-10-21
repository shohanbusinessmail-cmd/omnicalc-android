package com.shohan.omnicalc.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Visual role of a calculator key (spec §2.4 interaction principles). */
enum class CalcKeyVariant { DIGIT, OPERATOR, FUNCTION, DESTRUCTIVE, EQUALS, MEMORY }

/**
 * Calculator key: ≥56dp height (48dp+ touch target), accessible
 * [contentDescription] separate from the symbol, restrained styling.
 */
@Composable
fun CalcKey(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CalcKeyVariant = CalcKeyVariant.DIGIT,
    enabled: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
) {
    val colors = when (variant) {
        CalcKeyVariant.DIGIT -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        CalcKeyVariant.OPERATOR -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        CalcKeyVariant.FUNCTION -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CalcKeyVariant.MEMORY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        )
        CalcKeyVariant.DESTRUCTIVE -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        CalcKeyVariant.EQUALS -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }

    val shape = RoundedCornerShape(14.dp)

    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            .semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Secondary outlined key variant (e.g. memory row on light backgrounds). */
@Composable
fun CalcKeyOutlined(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
            .semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
