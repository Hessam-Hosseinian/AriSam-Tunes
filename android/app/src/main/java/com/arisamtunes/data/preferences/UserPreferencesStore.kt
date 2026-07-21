package com.arisamtunes.data.preferences

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "auth_tokens",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

enum class LanguagePreference { System, English, Persian }
enum class ThemePreference { System, Light, Dark }

data class StoredAuthTokens(val accessToken: String, val refreshToken: String)

data class UserPreferences(
    val language: LanguagePreference = LanguagePreference.System,
    val theme: ThemePreference = ThemePreference.System,
    val fontScale: Float = 1f,
    val isPremium: Boolean = false,
    val currentUserId: String? = null,
    val hasAuthTokens: Boolean = false,
    val shouldShowMusicSuggestions: Boolean = false,
)

@Singleton
class UserPreferencesStore @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val languageKey = stringPreferencesKey("language")
    private val themeKey = stringPreferencesKey("theme")
    private val fontScaleKey = floatPreferencesKey("font_scale")
    private val isPremiumKey = booleanPreferencesKey("is_premium")
    private val currentUserIdKey = stringPreferencesKey("current_user_id")
    private val shouldShowMusicSuggestionsKey = booleanPreferencesKey("should_show_music_suggestions")

    val preferences: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { values ->
        UserPreferences(
            language = values[languageKey].toEnum(LanguagePreference.System),
            theme = values[themeKey].toEnum(ThemePreference.System),
            fontScale = sanitizeFontScale(values[fontScaleKey]),
            isPremium = values[isPremiumKey] ?: false,
            currentUserId = values[currentUserIdKey],
            hasAuthTokens = !values[accessTokenKey].isNullOrBlank() && !values[refreshTokenKey].isNullOrBlank(),
            shouldShowMusicSuggestions = values[shouldShowMusicSuggestionsKey] ?: false,
        )
    }

    suspend fun loadTokens(): StoredAuthTokens? = context.userPreferencesDataStore.data.first().let { values ->
        val access = values[accessTokenKey]?.takeIf(String::isNotBlank) ?: return null
        val refresh = values[refreshTokenKey]?.takeIf(String::isNotBlank) ?: return null
        StoredAuthTokens(access, refresh)
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String? = null,
        shouldShowMusicSuggestions: Boolean? = null,
    ) {
        context.userPreferencesDataStore.edit { values ->
            values[accessTokenKey] = accessToken
            values[refreshTokenKey] = refreshToken
            userId?.let { values[currentUserIdKey] = it }
            shouldShowMusicSuggestions?.let { values[shouldShowMusicSuggestionsKey] = it }
        }
    }

    suspend fun shouldShowMusicSuggestions(): Boolean =
        context.userPreferencesDataStore.data.first()[shouldShowMusicSuggestionsKey] ?: false

    suspend fun markMusicSuggestionsShown() {
        context.userPreferencesDataStore.edit { it[shouldShowMusicSuggestionsKey] = false }
    }

    suspend fun clearTokens() {
        context.userPreferencesDataStore.edit { values ->
            values.remove(accessTokenKey)
            values.remove(refreshTokenKey)
            values.remove(shouldShowMusicSuggestionsKey)
            values.remove(isPremiumKey)
            values.remove(currentUserIdKey)
        }
    }

    suspend fun setLanguage(language: LanguagePreference) {
        context.userPreferencesDataStore.edit { it[languageKey] = language.name }
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.userPreferencesDataStore.edit { it[themeKey] = theme.name }
    }

    suspend fun setFontScale(fontScale: Float) {
        context.userPreferencesDataStore.edit { it[fontScaleKey] = sanitizeFontScale(fontScale) }
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.userPreferencesDataStore.edit { it[isPremiumKey] = isPremium }
    }

    suspend fun setCurrentUser(userId: String, isPremium: Boolean? = null) {
        context.userPreferencesDataStore.edit { values ->
            values[currentUserIdKey] = userId
            isPremium?.let { values[isPremiumKey] = it }
        }
    }

    suspend fun clearAll() {
        context.userPreferencesDataStore.edit { it.clear() }
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
}

internal const val MinFontScale = 0.85f
internal const val MaxFontScale = 1.35f

internal fun sanitizeFontScale(value: Float?): Float = value
    ?.takeIf(Float::isFinite)
    ?.coerceIn(MinFontScale, MaxFontScale)
    ?: 1f
