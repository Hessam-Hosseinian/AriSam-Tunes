package com.arisamtunes.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arisamtunes.R
import com.arisamtunes.feature.auth.AuthScreen
import com.arisamtunes.feature.auth.AuthViewModel
import com.arisamtunes.feature.auth.AuthEffect

private const val SplashRoute = "splash"
private const val AuthRoute = "auth"
private const val MainRoute = "main"

@Composable
fun AriSamTunesRoot(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val session by sessionViewModel.state.collectAsState()

    LaunchedEffect(session) {
        val destination = when (session) {
            SessionState.Checking -> SplashRoute
            SessionState.Authenticated -> MainRoute
            SessionState.Unauthenticated -> AuthRoute
        }
        navController.navigate(destination) {
            popUpTo(navController.graph.startDestinationId) { inclusive = destination != SplashRoute }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = SplashRoute) {
        composable(SplashRoute) { SplashScreen() }
        composable(AuthRoute) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    if (effect == AuthEffect.Authenticated) sessionViewModel.authenticated()
                }
            }
            AuthScreen(state = state, onEvent = viewModel::onEvent)
        }
        composable(MainRoute) { AriSamAppShell() }
    }
}

@Composable
private fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.GraphicEq, stringResource(R.string.app_logo_description), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        CircularProgressIndicator()
    }
}
