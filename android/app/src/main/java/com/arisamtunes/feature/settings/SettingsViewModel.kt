package com.arisamtunes.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.preferences.LanguagePreference
import com.arisamtunes.data.preferences.PreferencesRepository
import com.arisamtunes.data.preferences.ThemePreference
import com.arisamtunes.data.preferences.UserPreferences
import com.arisamtunes.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val preferences = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences(),
    )

    fun setLanguage(language: LanguagePreference) = viewModelScope.launch {
        repository.setLanguage(language)
    }

    fun setTheme(theme: ThemePreference) = viewModelScope.launch {
        repository.setTheme(theme)
    }

    fun setFontScale(fontScale: Float) = viewModelScope.launch {
        repository.setFontScale(fontScale)
    }

    fun logout(onComplete: () -> Unit) = viewModelScope.launch {
        authRepository.logout()
        onComplete()
    }
}
