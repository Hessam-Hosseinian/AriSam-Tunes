package com.arisamtunes.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arisamtunes.R
import com.arisamtunes.core.design.components.GlassCard
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.design.theme.AriSamThemeTokens

@Composable
fun AuthScreen(state: AuthUiState, onEvent: (AuthEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = AriSamThemeTokens.spacing
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { launched = true }
    val introScale by animateFloatAsState(
        targetValue = if (launched) 1f else .86f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "authIntroScale",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF081721), Color(0xFF0B2230), Color(0xFF102B3B)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl, vertical = spacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthBrandMark()
            Spacer(Modifier.height(spacing.xl))
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(introScale),
            )
            AnimatedContent(
                targetState = state.mode,
                transitionSpec = { fadeIn(tween(220, delayMillis = 80)) togetherWith fadeOut(tween(120)) },
                label = "authTaglineSwap",
            ) { mode ->
                Text(
                    text = stringResource(if (mode == AuthMode.Login) R.string.auth_login_tagline else R.string.auth_register_tagline),
                    color = Color(0xFF8ED8FF),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(spacing.xxl))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                AnimatedContent(
                    targetState = state.mode,
                    transitionSpec = {
                        val goingToRegister = targetState == AuthMode.Register
                        val direction = if (goingToRegister) 1 else -1
                        (
                            slideInHorizontally(
                                animationSpec = tween(360, easing = FastOutSlowInEasing),
                                initialOffsetX = { it * direction },
                            ) + fadeIn(tween(220, delayMillis = 80))
                        ) togetherWith (
                            slideOutHorizontally(
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                targetOffsetX = { -it * direction },
                            ) + fadeOut(tween(180))
                        ) using SizeTransform(clip = false)
                    },
                    label = "authModeSwap",
                ) { mode ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        Text(
                            stringResource(if (mode == AuthMode.Login) R.string.login_title else R.string.register_title),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = stringResource(if (mode == AuthMode.Login) R.string.auth_login_support else R.string.auth_register_support),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        AnimatedVisibility(
                            visible = mode == AuthMode.Register,
                            enter = slideInVertically(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                initialOffsetY = { -it / 2 },
                            ) + fadeIn(tween(220)),
                            exit = slideOutVertically(
                                animationSpec = tween(200, easing = FastOutSlowInEasing),
                                targetOffsetY = { -it / 2 },
                            ) + fadeOut(tween(150)),
                        ) {
                            OutlinedTextField(
                                value = state.displayName,
                                onValueChange = { onEvent(AuthEvent.DisplayNameChanged(it)) },
                                label = { Text(stringResource(R.string.display_name)) },
                                leadingIcon = { Icon(Icons.Rounded.Badge, null) },
                                isError = state.validationError == AuthValidationError.DisplayName,
                                colors = authTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                            label = { Text(stringResource(R.string.email)) },
                            leadingIcon = { Icon(Icons.Rounded.AlternateEmail, null) },
                            isError = state.validationError == AuthValidationError.Email,
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                            label = { Text(stringResource(R.string.password)) },
                            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                            isError = state.validationError == AuthValidationError.Password,
                            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { onEvent(AuthEvent.TogglePasswordVisibility) }) {
                                    Icon(
                                        if (state.isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        stringResource(R.string.toggle_password_visibility),
                                    )
                                }
                            },
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        state.validationError?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
                        state.authError?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
                        Button(
                            onClick = { onEvent(AuthEvent.Submit) },
                            enabled = !state.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0797DB),
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(stringResource(if (mode == AuthMode.Login) R.string.login else R.string.create_account))
                            }
                        }
                        TextButton(onClick = { onEvent(AuthEvent.ToggleMode) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(
                                stringResource(if (mode == AuthMode.Login) R.string.need_account else R.string.have_account),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthBrandMark() {
    val transition = rememberInfiniteTransition(label = "authBrandPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1_900
                1f at 0 using FastOutSlowInEasing
                1.12f at 850 using FastOutSlowInEasing
                1f at 1_900 using FastOutSlowInEasing
            },
        ),
        label = "authPulseScale",
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = .18f,
        targetValue = .04f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1_900
                .18f at 0 using FastOutSlowInEasing
                .04f at 850 using FastOutSlowInEasing
                .18f at 1_900 using FastOutSlowInEasing
            },
        ),
        label = "authPulseAlpha",
    )
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { launched = true }
    val logoScale by animateFloatAsState(
        targetValue = if (launched) 1f else .2f,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "authLogoLaunch",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(156.dp)) {
        Box(
            modifier = Modifier
                .size(138.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Color(0xFF0797DB).copy(alpha = pulseAlpha)),
        )
        Surface(
            modifier = Modifier
                .size(148.dp)
                .scale(logoScale)
                .clip(CircleShape),
            color = Color.White,
            shadowElevation = 0.dp,
        ) {}
        Image(
            painter = painterResource(R.drawable.arisam_mark_dark),
            contentDescription = stringResource(R.string.app_logo_description),
//            contentScale = ContentScale.Fit,
//            colorFilter = ColorFilter.tint(Color(0xFF081721)),
//            modifier = Modifier
//                .scale(logoScale)
//                .fillMaxWidth(.72f)
//                .aspectRatio(1f),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(22.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Color(0xFF0797DB)),
        )
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF0797DB),
    focusedLabelColor = Color(0xFF0797DB),
    focusedLeadingIconColor = Color(0xFF0797DB),
    cursorColor = Color(0xFF0797DB),
)

private fun AuthUiError.messageRes() = when (this) {
    AuthUiError.InvalidCredentials -> R.string.error_invalid_credentials
    AuthUiError.UserExists -> R.string.error_user_exists
    AuthUiError.RateLimited -> R.string.error_rate_limited
    AuthUiError.Network -> R.string.error_network
    AuthUiError.Unknown -> R.string.error_unknown
}

private fun AuthValidationError.messageRes() = when (this) {
    AuthValidationError.Email -> R.string.invalid_email
    AuthValidationError.Password -> R.string.invalid_password
    AuthValidationError.DisplayName -> R.string.invalid_display_name
}

@Preview
@Composable
private fun LoginPreview() { AriSamTheme(darkTheme = true) { AuthScreen(AuthUiState(), {}) } }

@Preview
@Composable
private fun RegisterPreview() { AriSamTheme { AuthScreen(AuthUiState(mode = AuthMode.Register), {}) } }
