package com.arisamtunes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.navigation.AriSamTunesRoot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AriSamTunesApp() }
    }
}

@Composable
private fun AriSamTunesApp() {
    AriSamTheme {
        AriSamTunesRoot()
    }
}

@Preview(showBackground = true)
@Composable
private fun AriSamTunesAppPreview() {
    AriSamTunesApp()
}
