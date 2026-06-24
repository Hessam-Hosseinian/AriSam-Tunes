package com.arisamtunes.data.preferences

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(private val store: UserPreferencesStore) {
    val preferences: Flow<UserPreferences> = store.preferences

    suspend fun setLanguage(language: LanguagePreference) = store.setLanguage(language)

    suspend fun setTheme(theme: ThemePreference) = store.setTheme(theme)

    suspend fun setFontScale(fontScale: Float) = store.setFontScale(fontScale)

    suspend fun setPremium(isPremium: Boolean) = store.setPremium(isPremium)
}
