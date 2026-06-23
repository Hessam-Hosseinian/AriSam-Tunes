package com.arisamtunes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.navigation.AriSamTunesRoot
import com.arisamtunes.core.navigation.AppPreferencesViewModel
import com.arisamtunes.data.preferences.LanguagePreference
import com.arisamtunes.data.preferences.ThemePreference
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AriSamTunesApp() }
    }
}

@Composable
private fun AriSamTunesApp(preferencesViewModel: AppPreferencesViewModel = hiltViewModel()) {
    val preferences by preferencesViewModel.preferences.collectAsState()
    val baseContext = LocalContext.current
    val locale = preferences.language.toLocale() ?: baseContext.resources.configuration.locales[0]
    val localizedContext = remember(baseContext, preferences.language) {
        preferences.language.toLocale()?.let { localeOverride ->
            val configuration = android.content.res.Configuration(baseContext.resources.configuration)
            configuration.setLocale(localeOverride)
            baseContext.createConfigurationContext(configuration)
        } ?: baseContext
    }
    val darkTheme = when (preferences.theme) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val layoutDirection = if (locale.language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalLayoutDirection provides layoutDirection,
    ) {
        AriSamTheme(darkTheme = darkTheme, fontScale = preferences.fontScale) {
            AriSamTunesRoot()
        }
    }
}

private fun LanguagePreference.toLocale(): Locale? = when (this) {
    LanguagePreference.System -> null
    LanguagePreference.English -> Locale("en")
    LanguagePreference.Persian -> Locale("fa")
}

@Preview(showBackground = true)
@Composable
private fun AriSamTunesAppPreview() {
    AriSamTheme { AriSamTunesRoot() }
}
