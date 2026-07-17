package com.arisamtunes.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.unit.sp


private const val SplashRoute = "splash"
private const val AuthRoute = "auth"
private const val MainRoute = "main"

@Composable
fun AriSamTunesRoot(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val session by sessionViewModel.state.collectAsStateWithLifecycle()
    var splashComplete by remember { mutableStateOf(false) }

    LaunchedEffect(session, splashComplete) {
        if (!splashComplete || session == SessionState.Checking) return@LaunchedEffect
        val destination = when (session) {
            SessionState.Authenticated -> MainRoute
            SessionState.Unauthenticated -> AuthRoute
            SessionState.Checking -> SplashRoute
        }
        navController.navigate(destination) {
            // Authentication states must never remain underneath each other;
            // otherwise Back from Main can reveal Auth again after login.
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = SplashRoute) {
        composable(SplashRoute) { SplashScreen(onFinished = { splashComplete = true }) }
        composable(AuthRoute) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
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
    val finalContentOffsetX = (-22).dp
    val finalMarkOffsetX = (-88).dp
    val textStartX = 112.dp
    val textWidth = 238.dp

    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        phase = 1
        delay(520)
        phase = 2
        delay(620)
        phase = 3
        delay(520)
        phase = 4
        delay(980)
        onFinished()
    }

    val markSize by animateDpAsState(
        targetValue = when {
            phase < 2 -> 0.dp
            phase == 2 -> 76.dp
            else -> 52.dp
        },
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "splashMarkSize",
    )
    val markOffsetX by animateDpAsState(
        targetValue = if (phase >= 4) finalMarkOffsetX else 0.dp,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "splashMarkOffsetX",
    )
    val markOffsetY by animateDpAsState(
        targetValue = when {
            phase < 2 -> 28.dp
            phase == 2 -> (-78).dp
            else -> 0.dp
        },
        animationSpec = tween(560, easing = FastOutSlowInEasing),
        label = "splashMarkOffsetY",
    )
    val markScale by animateFloatAsState(
        targetValue = when (phase) {
            2 -> 1.04f
            3 -> .98f
            else -> 1f
        },
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "splashMarkPulse",
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (phase == 1) .38f else 0f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "splashShadowAlpha",
    )
    val ovalWidth by animateDpAsState(
        targetValue = if (phase < 2) 230.dp else 0.dp,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "ovalWidth",
    )
    val ovalHeight by animateDpAsState(
        targetValue = if (phase < 2) 62.dp else 0.dp,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "ovalHeight",
    )
    val markAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "splashMarkAlpha",
    )
    val markBackgroundAlpha by animateFloatAsState(
        targetValue = when (phase) {
            2 -> .92f
            3 -> .82f
            else -> 1f
        },
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "splashMarkBackgroundAlpha",
    )
    val textRevealWidth by animateDpAsState(
        targetValue = if (phase >= 4) textWidth else 0.dp,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "splashTextRevealWidth",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (phase >= 4) 1f else 0f,
        animationSpec = tween(220, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "splashTextAlpha",
    )
    val contentOffsetX by animateDpAsState(
        targetValue = if (phase >= 4) finalContentOffsetX else 0.dp,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "splashContentOffsetX",
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
        Canvas(
            modifier = Modifier
                .offset(y = 28.dp * frameScaleY)
                .size(width = ovalWidth * frameScaleX, height = ovalHeight * frameScaleY)
                .alpha(shadowAlpha),
        ) {
            drawOval(
                color = Color(0xFF26364A),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
            )
        }
        Box(
            modifier = Modifier
                .size(width = 330.dp * frameScaleX, height = 92.dp * frameScaleY)
                .offset(x = contentOffsetX * frameScaleX)
                .alpha(markAlpha),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = markOffsetX * frameScaleX, y = markOffsetY * frameScaleY)
                    .size(markSize)
                    .scale(markScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF16435A).copy(alpha = markBackgroundAlpha),
                                Color(0xFF0A2838).copy(alpha = markBackgroundAlpha),
                                Color(0xFF06131D),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(markSize * .88f)
                        .clip(CircleShape)
                        .background(Color(0xFF07151E).copy(alpha = .64f)),
                )
                Image(
                    painter = painterResource(R.drawable.arisam_mark_dark),
                    contentDescription = stringResource(R.string.app_logo_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(markSize * .68f),
                )
            }
            AnimatedVisibility(
                visible = phase >= 4,
                enter = fadeIn(tween(180, delayMillis = 60, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = textStartX * frameScaleX),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = textWidth * frameScaleX, height = 46.dp)
                        .clipToBounds()
                        .alpha(textAlpha),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = textRevealWidth * frameScaleX, height = 46.dp)
                            .clipToBounds(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = "AriSam Tunes",
                            color = Color.White,
                            fontSize = 35.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.requiredSize(width = textWidth * frameScaleX, height = 46.dp),
                        )
                    }
                }
            }
        }
    }
}
