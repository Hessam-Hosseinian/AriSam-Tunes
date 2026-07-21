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
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arisamtunes.feature.player.Media3PlaybackController
import dagger.hilt.android.AndroidEntryPoint
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.navigation.AriSamTunesRoot
import com.arisamtunes.core.navigation.AppPreferencesViewModel
import com.arisamtunes.data.preferences.LanguagePreference
import com.arisamtunes.data.preferences.ThemePreference
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var playbackController: Media3PlaybackController

    private val runtimePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.READ_PHONE_STATE] == true) {
            playbackController.enablePhoneCallMonitoring()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val missingPermissions = buildList {
            if (
                packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.READ_PHONE_STATE)
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (missingPermissions.isNotEmpty()) runtimePermissions.launch(missingPermissions.toTypedArray())
        else playbackController.enablePhoneCallMonitoring()
        setContent { AriSamTunesApp() }
    }
}

@Composable
private fun AriSamTunesApp(preferencesViewModel: AppPreferencesViewModel = hiltViewModel()) {
    val preferences by preferencesViewModel.preferences.collectAsStateWithLifecycle()
    val baseContext = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val systemLocale = systemConfiguration.locales[0]
    val locale = preferences.language.toLocale() ?: systemLocale
    val localizedConfiguration = remember(systemConfiguration, locale) {
        Configuration(systemConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val localizedContext = remember(baseContext, localizedConfiguration) {
        ContextThemeWrapper(baseContext, baseContext.theme).apply {
            applyOverrideConfiguration(localizedConfiguration)
        }
    }
    SideEffect { Locale.setDefault(locale) }
    val darkTheme = when (preferences.theme) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val layoutDirection = if (locale.language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalContext provides localizedContext,
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
