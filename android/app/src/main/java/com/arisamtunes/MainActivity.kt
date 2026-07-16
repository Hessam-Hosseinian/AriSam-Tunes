package com.arisamtunes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalConfiguration
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
import android.content.res.Configuration
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { AriSamTunesApp() }
    }
}

@Composable
private fun AriSamTunesApp(preferencesViewModel: AppPreferencesViewModel = hiltViewModel()) {
    val preferences by preferencesViewModel.preferences.collectAsState()
    val baseContext = LocalContext.current
    val locale = preferences.language.toLocale() ?: Locale.getDefault()
    val localizedConfiguration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
    @Suppress("DEPRECATION")
    baseContext.resources.updateConfiguration(localizedConfiguration, baseContext.resources.displayMetrics)
    val darkTheme = when (preferences.theme) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val layoutDirection = if (locale.language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalLayoutDirection provides layoutDirection,
    ) {
        AriSamTheme(darkTheme = darkTheme, fontScale = preferences.fontScale) {
            AriSamTunesRoot()
        }
    }
}

private fun LanguagePreference.toLocale(): Locale? = when (this) {
    LanguagePreference.System -> null
    LanguagePreference.English -> Locale.forLanguageTag("en")
    LanguagePreference.Persian -> Locale.forLanguageTag("fa")
}

@Preview(showBackground = true)
@Composable
private fun AriSamTunesAppPreview() {
    AriSamTheme { AriSamTunesRoot() }
}
