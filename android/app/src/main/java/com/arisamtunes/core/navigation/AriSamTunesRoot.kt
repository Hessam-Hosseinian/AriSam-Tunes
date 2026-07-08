package com.arisamtunes.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arisamtunes.R
import com.arisamtunes.feature.auth.AuthScreen
import com.arisamtunes.feature.auth.AuthViewModel
import com.arisamtunes.feature.auth.AuthEffect
import kotlinx.coroutines.delay

private const val SplashRoute = "splash"
private const val AuthRoute = "auth"
private const val MainRoute = "main"

@Composable
fun AriSamTunesRoot(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val session by sessionViewModel.state.collectAsState()
    var splashComplete by remember { mutableStateOf(false) }

    LaunchedEffect(session, splashComplete) {
        if (!splashComplete || session == SessionState.Checking) return@LaunchedEffect
        val destination = when (session) {
            SessionState.Authenticated -> MainRoute
            SessionState.Unauthenticated -> AuthRoute
            SessionState.Checking -> SplashRoute
        }
        navController.navigate(destination) {
            popUpTo(navController.graph.startDestinationId) { inclusive = destination != SplashRoute }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = SplashRoute) {
        composable(SplashRoute) { SplashScreen(onFinished = { splashComplete = true }) }
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
        composable(MainRoute) {
            AriSamAppShell(onLoggedOut = sessionViewModel::loggedOut)
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        phase = 1
        delay(360)
        phase = 2
        delay(360)
        phase = 3
        delay(360)
        phase = 4
        delay(620)
        onFinished()
    }

    val dotSize by animateDpAsState(
        targetValue = when {
            phase < 2 -> 0.dp
            phase == 2 -> 70.dp
            else -> 49.dp
        },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "splashDotSize",
    )
    val dotOffsetX by animateDpAsState(
        targetValue = if (phase >= 4) (-94).dp else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "splashDotOffset",
    )
    val dotOffsetY by animateDpAsState(
        targetValue = if (phase == 2) (-86).dp else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "splashDotOffsetY",
    )
    val dotScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "splashDotPulse",
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (phase == 1) .38f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "splashShadowAlpha",
    )
    val markAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "splashMarkAlpha",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07151E), Color(0xFF0B1B26), Color(0xFF0C202D)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val frameScaleX = maxWidth.value / 390f
        val frameScaleY = maxHeight.value / 844f
        Box(
            modifier = Modifier
                .offset(y = 28.dp * frameScaleY)
                .size(width = 230.dp * frameScaleX, height = 62.dp * frameScaleY)
                .alpha(shadowAlpha)
                .blur(6.dp)
                .background(Color(0xFF26364A), RoundedCornerShape(percent = 50)),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(markAlpha),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = dotOffsetX * frameScaleX, y = dotOffsetY * frameScaleY)
                    .size(dotSize)
                    .scale(dotScale)
                    .background(Color(0xFF0369A1), CircleShape),
            )
            AnimatedVisibility(
                visible = phase >= 4,
                enter = fadeIn(tween(260, delayMillis = 40, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120)),
            ) {
                Text(
                    text = "NEXUS APP",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = (-66).dp * frameScaleX),
                )
            }
        }
    }
}
