package com.arisamtunes.feature.auth

data class AuthUiState(
    val mode: AuthMode = AuthMode.Login,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val validationError: AuthValidationError? = null,
)

enum class AuthMode { Login, Register }
enum class AuthValidationError { Email, Password, DisplayName }

sealed interface AuthEvent {
    data class EmailChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data class DisplayNameChanged(val value: String) : AuthEvent
    data object TogglePasswordVisibility : AuthEvent
    data object ToggleMode : AuthEvent
    data object Submit : AuthEvent
}

sealed interface AuthEffect {
    data class SubmitCredentials(
        val mode: AuthMode,
        val email: String,
        val password: String,
        val displayName: String,
    ) : AuthEffect
}
