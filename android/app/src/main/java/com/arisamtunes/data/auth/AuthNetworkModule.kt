package com.arisamtunes.data.auth

import com.arisamtunes.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthNetworkModule {
    @Provides @Singleton
    fun provideHttpClient(tokenStore: AuthTokenStore): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        defaultRequest { url("${BuildConfig.API_BASE_URL}/api/v1/") }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 12_000
            socketTimeoutMillis = 12_000
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
        install(WebSockets)
        install(Auth) {
            bearer {
                loadTokens { tokenStore.load() }
                sendWithoutRequest { request ->
                    request.url.toString().let { path ->
                        path.endsWith("/auth/me") || path.endsWith("/auth/logout") || !path.contains("/auth/")
                    }
                }
                refreshTokens {
                    val refresh = oldTokens?.refreshToken ?: return@refreshTokens null
                    runCatching {
                        val tokens = client.post("auth/refresh") {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshDto(refresh))
                        }.body<TokenDto>()
                        tokenStore.save(tokens)
                        BearerTokens(tokens.accessToken, tokens.refreshToken)
                    }.getOrElse { tokenStore.clear(); null }
                }
            }
        }
    }
}
