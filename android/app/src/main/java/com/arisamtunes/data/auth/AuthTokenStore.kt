package com.arisamtunes.data.auth

import com.arisamtunes.data.preferences.UserPreferencesStore
import io.ktor.client.plugins.auth.providers.BearerTokens
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenStore @Inject constructor(private val preferencesStore: UserPreferencesStore) {
    suspend fun load(): BearerTokens? = preferencesStore.loadTokens()?.let { tokens ->
        BearerTokens(tokens.accessToken, tokens.refreshToken)
    }

    suspend fun save(tokens: TokenDto, shouldShowMusicSuggestions: Boolean? = null) =
        preferencesStore.saveTokens(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            shouldShowMusicSuggestions = shouldShowMusicSuggestions,
        )

    suspend fun clear() = preferencesStore.clearTokens()
}
