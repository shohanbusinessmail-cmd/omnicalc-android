package com.shohan.omnicalc.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shohan.omnicalc.R
import com.shohan.omnicalc.core.designsystem.ErrorBanner

/**
 * INTERIM, explicitly-labeled placeholders for destinations whose feature
 * modules land in the next build increments. They contain NO fake controls —
 * only an honest status statement (master-instruction rule 7/26 and
 * docs/FEATURE_MATRIX.md track the schedule).
 */
@Composable
private fun InterimNote(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ErrorBanner(message = message, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.interim_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PlaceholderFormulas(modifier: Modifier = Modifier) {
    InterimNote(stringResource(R.string.interim_formulas), modifier)
}

@Composable
fun PlaceholderTools(modifier: Modifier = Modifier) {
    InterimNote(stringResource(R.string.interim_tools), modifier)
}
