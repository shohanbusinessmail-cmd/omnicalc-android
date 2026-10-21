package com.shohan.omnicalc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shohan.omnicalc.R
import com.shohan.omnicalc.core.designsystem.OmniCard

/**
 * Global search (spec §16): fast, fully local index over calculator modes,
 * destinations and settings — EN + BN terms, category labels, no fake hits.
 * The index grows with each feature cycle (see docs/FEATURE_MATRIX.md).
 */
private data class SearchEntry(
    val route: String,
    val titleEn: String,
    val titleBn: String,
    val categoryEn: String,
    val categoryBn: String,
    val keywords: String,
)

private val INDEX = listOf(
    SearchEntry("calculate", "Calculate", "হিসাব", "Calculator", "ক্যালকুলেটর",
        "standard arithmetic basic percent decimal add subtract multiply divide যোগ বিয়োগ গুণ ভাগ"),
    SearchEntry("scientific", "Scientific", "বৈজ্ঞানিক", "Calculator", "ক্যালকুলেটর",
        "sin cos tan log ln sqrt root factorial trigonometry radians degrees ত্রিকোণমিতি"),
    SearchEntry("formulas", "Formulas", "সূত্র", "Library", "লাইব্রেরি",
        "geometry area volume math formulas জ্যামিতি ক্ষেত্রফল আয়তন"),
    SearchEntry("tools", "Tools", "টুলস", "Convert", "রূপান্তর",
        "converter units land currency date finance রূপান্তর একক জমি মুদ্রা"),
    SearchEntry("library", "Library", "লাইব্রেরি", "Library", "লাইব্রেরি",
        "history favorites settings ইতিহাস পছন্দ সেটিংস"),
    SearchEntry("library", "Settings", "সেটিংস", "Library", "লাইব্রেরি",
        "theme language numerals grouping angle haptics থিম ভাষা সংখ্যা"),
    SearchEntry("about", "About OmniCalc", "OmniCalc সম্পর্কে", "App", "অ্যাপ",
        "developer contact privacy version যোগাযোগ গোপনীয়তা"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigate: (route: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()

    val results = if (trimmed.isBlank()) INDEX else INDEX.filter { entry ->
        entry.titleEn.contains(trimmed, ignoreCase = true) ||
            entry.titleBn.contains(trimmed) ||
            entry.keywords.contains(trimmed, ignoreCase = true) ||
            entry.keywords.contains(trimmed) ||
            entry.categoryEn.contains(trimmed, ignoreCase = true) ||
            entry.categoryBn.contains(trimmed)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.shohan.omnicalc.core.localization.R.string.action_close))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            )

            if (trimmed.isNotBlank() && results.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.route + it.titleEn }) { entry ->
                        OmniCard(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = { onNavigate(entry.route) },
                        ) {
                            Text(
                                text = entry.titleEn + " · " + entry.titleBn,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.clickable { onNavigate(entry.route) },
                            )
                            Text(
                                text = entry.categoryEn + " · " + entry.categoryBn,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
