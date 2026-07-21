package com.arisamtunes.feature.auth

import com.arisamtunes.core.design.spacing.AriSamDimensions
import com.arisamtunes.core.design.colors.AriSamPalette
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WifiOff
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.arisamtunes.R
import com.arisamtunes.core.design.components.GlassCard
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.design.theme.AriSamThemeTokens

@Composable
fun AuthScreen(state: AuthUiState, onEvent: (AuthEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = AriSamThemeTokens.spacing
    val displayNameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val horizontalDirection = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
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
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainer),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl, vertical = spacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthBrandMark()
            Spacer(Modifier.height(spacing.xl))
            Text(
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.primary,
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
                                initialOffsetX = { it * direction * horizontalDirection },
                            ) + fadeIn(tween(220, delayMillis = 80))
                        ) togetherWith (
                            slideOutHorizontally(
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                targetOffsetX = { -it * direction * horizontalDirection },
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
                            visible = state.authError != null,
                            enter = slideInVertically(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                initialOffsetY = { -it / 3 },
                            ) + fadeIn(tween(220)),
                            exit = slideOutVertically(
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                                targetOffsetY = { -it / 3 },
                            ) + fadeOut(tween(140)),
                        ) {
                            state.authError?.let { AuthErrorBanner(error = it) }
                        }
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
                                supportingText = {
                                    AuthFieldError(
                                        visible = state.validationError == AuthValidationError.DisplayName,
                                        text = stringResource(R.string.invalid_display_name),
                                    )
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
                                colors = authTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().focusRequester(displayNameFocusRequester),
                            )
                        }
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                            label = { Text(stringResource(R.string.email)) },
                            leadingIcon = { Icon(Icons.Rounded.AlternateEmail, null) },
                            isError = state.validationError == AuthValidationError.Email,
                            supportingText = {
                                AuthFieldError(
                                    visible = state.validationError == AuthValidationError.Email,
                                    text = stringResource(R.string.invalid_email),
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().focusRequester(emailFocusRequester),
                        )
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                            label = { Text(stringResource(R.string.password)) },
                            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                            isError = state.validationError == AuthValidationError.Password,
                            supportingText = {
                                AuthFieldError(
                                    visible = state.validationError == AuthValidationError.Password,
                                    text = stringResource(R.string.invalid_password),
                                )
                            },
                            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { onEvent(AuthEvent.TogglePasswordVisibility) }) {
                                    Icon(
                                        if (state.isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        stringResource(R.string.toggle_password_visibility),
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                onEvent(AuthEvent.Submit)
                            }),
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
                        )
                        Button(
                            onClick = { onEvent(AuthEvent.Submit) },
                            enabled = !state.isLoading,
                            colors = ButtonDefaults.buttonColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AriSamDimensions.dp52),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(AriSamDimensions.dp22),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = AriSamDimensions.dp2,
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
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(AriSamDimensions.dp156)) {
        Box(
            modifier = Modifier
                .size(AriSamDimensions.dp138)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(AriSamPalette.brandBlue.copy(alpha = pulseAlpha)),
        )
        Surface(
            modifier = Modifier
                .size(AriSamDimensions.dp148)
                .scale(logoScale)
                .clip(CircleShape),
            color = AriSamPalette.white,
            shadowElevation = AriSamDimensions.dp0,
        ) {}
        Image(
            painter = painterResource(R.drawable.arisam_mark_dark),
            contentDescription = stringResource(R.string.app_logo_description),
//            contentScale = ContentScale.Fit,
//            colorFilter = ColorFilter.tint(AriSamPalette.authInk),
//            modifier = Modifier
//                .scale(logoScale)
//                .fillMaxWidth(.72f)
//                .aspectRatio(1f),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(AriSamDimensions.dp22)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(AriSamPalette.brandBlue),
        )
    }
}

@Composable
private fun AuthErrorBanner(error: AuthUiError, modifier: Modifier = Modifier) {
    val accent = when (error) {
        AuthUiError.Offline, AuthUiError.ServerUnavailable, AuthUiError.TimedOut -> AriSamPalette.sky400
        AuthUiError.RateLimited -> AriSamPalette.amber500
        else -> AriSamPalette.rose400
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AriSamDimensions.dp18))
            .background(AriSamPalette.authInk.copy(alpha = .82f))
            .border(AriSamDimensions.dp1, accent.copy(alpha = .36f), RoundedCornerShape(AriSamDimensions.dp18))
            .padding(horizontal = AriSamDimensions.dp14, vertical = AriSamDimensions.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AriSamDimensions.dp38)
                .clip(CircleShape)
                .background(accent.copy(alpha = .16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = error.icon(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(AriSamDimensions.dp21),
            )
        }
        Spacer(Modifier.width(AriSamDimensions.dp12))
        Column(verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp2)) {
            Text(
                text = stringResource(error.titleRes()),
                color = AriSamPalette.white,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(error.messageRes()),
                color = AriSamPalette.white.copy(alpha = .82f),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(error.hintRes()),
                color = accent.copy(alpha = .92f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun AuthFieldError(visible: Boolean, text: String) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140)) + slideInVertically(
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            initialOffsetY = { -it / 3 },
        ),
        exit = fadeOut(tween(100)),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
    errorLeadingIconColor = MaterialTheme.colorScheme.error,
    errorTrailingIconColor = MaterialTheme.colorScheme.error,
    errorSupportingTextColor = MaterialTheme.colorScheme.error,
)

private fun AuthUiError.messageRes() = when (this) {
    AuthUiError.InvalidCredentials -> R.string.error_invalid_credentials
    AuthUiError.UserExists -> R.string.error_user_exists
    AuthUiError.RateLimited -> R.string.error_rate_limited
    AuthUiError.Offline -> R.string.error_offline
    AuthUiError.ServerUnavailable -> R.string.error_server_unavailable
    AuthUiError.TimedOut -> R.string.error_timeout
    AuthUiError.Unknown -> R.string.error_unknown
}

private fun AuthUiError.titleRes() = when (this) {
    AuthUiError.InvalidCredentials -> R.string.error_invalid_credentials_title
    AuthUiError.UserExists -> R.string.error_user_exists_title
    AuthUiError.RateLimited -> R.string.error_rate_limited_title
    AuthUiError.Offline -> R.string.error_offline_title
    AuthUiError.ServerUnavailable -> R.string.error_server_unavailable_title
    AuthUiError.TimedOut -> R.string.error_timeout_title
    AuthUiError.Unknown -> R.string.error_unknown_title
}

private fun AuthUiError.hintRes() = when (this) {
    AuthUiError.InvalidCredentials -> R.string.error_invalid_credentials_hint
    AuthUiError.UserExists -> R.string.error_user_exists_hint
    AuthUiError.RateLimited -> R.string.error_rate_limited_hint
    AuthUiError.Offline -> R.string.error_offline_hint
    AuthUiError.ServerUnavailable -> R.string.error_server_unavailable_hint
    AuthUiError.TimedOut -> R.string.error_timeout_hint
    AuthUiError.Unknown -> R.string.error_unknown_hint
}

private fun AuthUiError.icon(): ImageVector = when (this) {
    AuthUiError.Offline, AuthUiError.ServerUnavailable, AuthUiError.TimedOut -> Icons.Rounded.WifiOff
    else -> Icons.Rounded.ErrorOutline
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
