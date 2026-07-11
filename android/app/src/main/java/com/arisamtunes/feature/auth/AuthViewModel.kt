package com.arisamtunes.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.auth.AuthConnectionFailure
import com.arisamtunes.data.auth.AuthConnectionIssue
import com.arisamtunes.data.auth.AuthFailure
import com.arisamtunes.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()
    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: AuthEvent) {
        if (event == AuthEvent.Submit) {
            submit(_state.value)
            return
        }
        val current = _state.value
        _state.value = when (event) {
            is AuthEvent.EmailChanged -> current.copy(email = event.value, validationError = null, authError = null)
            is AuthEvent.PasswordChanged -> current.copy(password = event.value, validationError = null, authError = null)
            is AuthEvent.DisplayNameChanged -> current.copy(displayName = event.value, validationError = null, authError = null)
            AuthEvent.TogglePasswordVisibility -> current.copy(isPasswordVisible = !current.isPasswordVisible)
            AuthEvent.ToggleMode -> current.copy(
                mode = if (current.mode == AuthMode.Login) AuthMode.Register else AuthMode.Login,
                isLoading = false,
                validationError = null,
                authError = null,
            )
            AuthEvent.Submit -> current
        }
    }

    private fun submit(state: AuthUiState) {
        if (state.isLoading) return
        val error = when {
            !state.email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) -> AuthValidationError.Email
            state.password.length < 8 -> AuthValidationError.Password
            state.mode == AuthMode.Register && state.displayName.trim().length < 2 -> AuthValidationError.DisplayName
            else -> null
        }
        if (error != null) { _state.value = state.copy(validationError = error); return }
        viewModelScope.launch {
            _state.value = state.copy(isLoading = true, authError = null)
            runCatching { repository.authenticate(state.mode, state.email.trim(), state.password, state.displayName.trim()) }
                .onSuccess { _state.value = _state.value.copy(isLoading = false); _effects.send(AuthEffect.Authenticated) }
                .onFailure { failure ->
                    val uiError = when (failure) {
                        is AuthConnectionFailure -> when (failure.issue) {
                            AuthConnectionIssue.Offline -> AuthUiError.Offline
                            AuthConnectionIssue.ServerUnavailable -> AuthUiError.ServerUnavailable
                            AuthConnectionIssue.TimedOut -> AuthUiError.TimedOut
                        }
                        is AuthFailure -> when (failure.code) {
                            "AUTH_INVALID_CREDENTIALS" -> AuthUiError.InvalidCredentials
                            "USER_ALREADY_EXISTS" -> AuthUiError.UserExists
                            "RATE_LIMITED" -> AuthUiError.RateLimited
                            else -> AuthUiError.Unknown
                        }
                        else -> AuthUiError.Unknown
                    }
                    _state.value = _state.value.copy(isLoading = false, authError = uiError)
                }
        }
    }
}
