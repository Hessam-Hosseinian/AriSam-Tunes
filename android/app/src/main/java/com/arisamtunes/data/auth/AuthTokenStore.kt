package com.arisamtunes.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore("auth_tokens")

@Singleton
class AuthTokenStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    suspend fun load(): BearerTokens? = context.authDataStore.data.first().let { values ->
        val access = values[accessKey] ?: return null
        val refresh = values[refreshKey] ?: return null
        BearerTokens(access, refresh)
    }

    suspend fun save(tokens: TokenDto) {
        context.authDataStore.edit { it[accessKey] = tokens.accessToken; it[refreshKey] = tokens.refreshToken }
    }

    suspend fun clear() { context.authDataStore.edit { it.clear() } }
}
