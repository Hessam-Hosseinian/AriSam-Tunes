package com.arisamtunes.feature.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()
    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: AuthEvent) {
        val current = _state.value
        _state.value = when (event) {
            is AuthEvent.EmailChanged -> current.copy(email = event.value, validationError = null)
            is AuthEvent.PasswordChanged -> current.copy(password = event.value, validationError = null)
            is AuthEvent.DisplayNameChanged -> current.copy(displayName = event.value, validationError = null)
            AuthEvent.TogglePasswordVisibility -> current.copy(isPasswordVisible = !current.isPasswordVisible)
            AuthEvent.ToggleMode -> AuthUiState(mode = if (current.mode == AuthMode.Login) AuthMode.Register else AuthMode.Login)
            AuthEvent.Submit -> submit(current)
        }
    }

    private fun submit(state: AuthUiState): AuthUiState {
        val error = when {
            !state.email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) -> AuthValidationError.Email
            state.password.length < 8 -> AuthValidationError.Password
            state.mode == AuthMode.Register && state.displayName.trim().length < 2 -> AuthValidationError.DisplayName
            else -> null
        }
        if (error == null) _effects.trySend(AuthEffect.SubmitCredentials(state.mode, state.email.trim(), state.password, state.displayName.trim()))
        return state.copy(validationError = error)
    }
}
