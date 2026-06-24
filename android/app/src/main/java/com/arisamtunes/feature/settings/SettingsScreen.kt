package com.arisamtunes.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arisamtunes.R
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.preferences.LanguagePreference
import com.arisamtunes.data.preferences.ThemePreference
import com.arisamtunes.data.preferences.UserPreferences
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsState()
    SettingsScreen(
        preferences = preferences,
        onBack = onBack,
        onLanguageChange = viewModel::setLanguage,
        onThemeChange = viewModel::setTheme,
        onFontScaleChange = viewModel::setFontScale,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onLanguageChange: (LanguagePreference) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onFontScaleChange: (Float) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(AriSamThemeTokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.lg),
        ) {
            SettingsSection(
                title = stringResource(R.string.settings_language),
                description = stringResource(R.string.settings_language_description),
                icon = { Icon(Icons.Rounded.Language, null) },
            ) {
                PreferenceChips(
                    options = LanguagePreference.entries,
                    selected = preferences.language,
                    label = { it.label() },
                    onSelected = onLanguageChange,
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_theme),
                description = stringResource(R.string.settings_theme_description),
                icon = { Icon(Icons.Rounded.WbSunny, null) },
            ) {
                PreferenceChips(
                    options = ThemePreference.entries,
                    selected = preferences.theme,
                    label = { it.label() },
                    onSelected = onThemeChange,
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_font_size),
                description = stringResource(R.string.settings_font_size_description),
                icon = { Icon(Icons.Rounded.TextFields, null) },
            ) {
                Text(
                    text = stringResource(R.string.settings_font_size_percent, (preferences.fontScale * 100).roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = preferences.fontScale,
                    onValueChange = onFontScaleChange,
                    valueRange = 0.85f..1.35f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.settings_font_size_preview),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AriSamThemeTokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                icon()
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> PreferenceChips(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { option ->
            if (option == selected) {
                AssistChip(onClick = { onSelected(option) }, label = { Text(label(option)) })
            } else {
                SuggestionChip(onClick = { onSelected(option) }, label = { Text(label(option)) })
            }
        }
    }
}

@Composable
private fun LanguagePreference.label(): String = stringResource(
    when (this) {
        LanguagePreference.System -> R.string.settings_option_system
        LanguagePreference.English -> R.string.settings_language_english
        LanguagePreference.Persian -> R.string.settings_language_persian
    },
)

@Composable
private fun ThemePreference.label(): String = stringResource(
    when (this) {
        ThemePreference.System -> R.string.settings_option_system
        ThemePreference.Light -> R.string.settings_theme_light
        ThemePreference.Dark -> R.string.settings_theme_dark
    },
)
