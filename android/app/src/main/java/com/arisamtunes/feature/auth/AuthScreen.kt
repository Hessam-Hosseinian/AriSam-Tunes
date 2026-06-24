package com.arisamtunes.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.arisamtunes.R
import com.arisamtunes.core.design.components.GlassCard
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.design.theme.AriSamThemeTokens

@Composable
fun AuthScreen(state: AuthUiState, onEvent: (AuthEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = AriSamThemeTokens.spacing
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.GraphicEq, stringResource(R.string.app_logo_description), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(spacing.xl))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text(
                    stringResource(if (state.mode == AuthMode.Login) R.string.login_title else R.string.register_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (state.mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = { onEvent(AuthEvent.DisplayNameChanged(it)) },
                        label = { Text(stringResource(R.string.display_name)) },
                        isError = state.validationError == AuthValidationError.DisplayName,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                    label = { Text(stringResource(R.string.email)) },
                    isError = state.validationError == AuthValidationError.Email,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                    label = { Text(stringResource(R.string.password)) },
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
                    modifier = Modifier.fillMaxWidth(),
                )
                state.validationError?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
                Button(onClick = { onEvent(AuthEvent.Submit) }, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                    if (state.isLoading) CircularProgressIndicator() else Text(stringResource(if (state.mode == AuthMode.Login) R.string.login else R.string.create_account))
                }
                TextButton(onClick = { onEvent(AuthEvent.ToggleMode) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(if (state.mode == AuthMode.Login) R.string.need_account else R.string.have_account))
                }
            }
        }
    }
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
