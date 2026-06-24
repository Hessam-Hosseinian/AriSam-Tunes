package com.arisamtunes.domain.preferences

import com.arisamtunes.data.preferences.LanguagePreference
import com.arisamtunes.data.preferences.PreferencesRepository
import com.arisamtunes.data.preferences.ThemePreference
import javax.inject.Inject

class ObservePreferencesUseCase @Inject constructor(private val repository: PreferencesRepository) {
    operator fun invoke() = repository.preferences
}

class SetLanguageUseCase @Inject constructor(private val repository: PreferencesRepository) {
    suspend operator fun invoke(language: LanguagePreference) = repository.setLanguage(language)
}

class SetThemeUseCase @Inject constructor(private val repository: PreferencesRepository) {
    suspend operator fun invoke(theme: ThemePreference) = repository.setTheme(theme)
}

class SetFontScaleUseCase @Inject constructor(private val repository: PreferencesRepository) {
    suspend operator fun invoke(fontScale: Float) = repository.setFontScale(fontScale)
}

class SetPremiumStatusUseCase @Inject constructor(private val repository: PreferencesRepository) {
    suspend operator fun invoke(isPremium: Boolean) = repository.setPremium(isPremium)
}
