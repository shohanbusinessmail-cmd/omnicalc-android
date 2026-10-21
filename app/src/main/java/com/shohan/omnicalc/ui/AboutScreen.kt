package com.shohan.omnicalc.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shohan.omnicalc.BuildConfig
import com.shohan.omnicalc.R
import com.shohan.omnicalc.core.designsystem.OmniCard
import com.shohan.omnicalc.core.designsystem.OmniDivider
import com.shohan.omnicalc.core.designsystem.OmniSectionHeader

/**
 * About (spec §18): shows EXACT owner information, honest privacy notes and
 * real open-source acknowledgements — nothing invented (no website, office,
 * company or certifications).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var emailFailure by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(88.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            OmniCard {
                OmniSectionHeader(stringResource(R.string.about_developer))
                Text(
                    text = stringResource(R.string.about_owner),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.about_email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                com.shohan.omnicalc.core.designsystem.OmniDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_features),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            OmniCard {
                OmniSectionHeader(stringResource(R.string.about_privacy))
                Text(
                    text = stringResource(R.string.about_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(12.dp))
            OmniCard {
                OmniSectionHeader(stringResource(R.string.about_licenses))
                Text(
                    text = stringResource(R.string.about_licenses_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(16.dp))
            com.shohan.omnicalc.core.designsystem.OmniDivider()
            Spacer(Modifier.height(12.dp))

            androidx.compose.material3.Button(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$EMAIL")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, "OmniCalc")
                }
                val resolvable = intent.resolveActivity(context.packageManager) != null
                if (resolvable) {
                    emailFailure = false
                    context.startActivity(intent)
                } else {
                    emailFailure = true
                }
            }) {
                Icon(Icons.Filled.Email, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.about_contact))
            }
            if (emailFailure) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_email_fallback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private const val EMAIL = "helloiamshohan@gmail.com"
