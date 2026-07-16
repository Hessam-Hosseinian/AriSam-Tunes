package com.arisamtunes.data.auth

import com.arisamtunes.core.navigation.SessionBootstrapper
import com.arisamtunes.feature.auth.AuthMode
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val api: AuthApi, private val tokenStore: AuthTokenStore) : SessionBootstrapper {
    suspend fun authenticate(mode: AuthMode, email: String, password: String, displayName: String) {
        val tokens = try {
            if (mode == AuthMode.Login) api.login(email, password) else api.register(email, password, displayName)
        } catch (failure: Throwable) {
            throw failure.asAuthConnectionFailure()
        }
        tokenStore.save(tokens)
    }

    /**
     * A saved session is enough to open the app. Verifying it here made an
     * offline launch look like a logout because the request to /auth/me could
     * not reach the server. Requests refresh their bearer token when a network
     * connection is available; only an explicit logout removes local tokens.
     */
    override suspend fun restoreOrRefreshSession(): Boolean = tokenStore.load() != null

    suspend fun logout() {
        tokenStore.clear()
    }
}

class AuthConnectionFailure(val issue: AuthConnectionIssue, cause: Throwable) : RuntimeException(cause)

enum class AuthConnectionIssue { Offline, ServerUnavailable, TimedOut }

private fun Throwable.asAuthConnectionFailure(): Throwable {
    if (this is AuthFailure || this is AuthConnectionFailure) return this
    val causes = generateSequence(this) { it.cause }.toList()
    return when {
        causes.any { it is HttpRequestTimeoutException || it is SocketTimeoutException } ->
            AuthConnectionFailure(AuthConnectionIssue.TimedOut, this)
        causes.any { it is UnknownHostException } ->
            AuthConnectionFailure(AuthConnectionIssue.Offline, this)
        causes.any { it is ConnectException || it is IOException } ->
            AuthConnectionFailure(AuthConnectionIssue.ServerUnavailable, this)
        else -> this
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds abstract fun bindSessionBootstrapper(repository: AuthRepository): SessionBootstrapper
}
