package com.arisamtunes.data.auth

import com.arisamtunes.core.navigation.SessionBootstrapper
import com.arisamtunes.feature.auth.AuthMode
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val api: AuthApi, private val tokenStore: AuthTokenStore) : SessionBootstrapper {
    suspend fun authenticate(mode: AuthMode, email: String, password: String, displayName: String) {
        val tokens = if (mode == AuthMode.Login) api.login(email, password) else api.register(email, password, displayName)
        tokenStore.save(tokens)
    }

    override suspend fun restoreOrRefreshSession(): Boolean {
        if (tokenStore.load() == null) return false
        return runCatching { api.me(); true }.getOrElse { tokenStore.clear(); false }
    }

    suspend fun logout() {
        tokenStore.clear()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds abstract fun bindSessionBootstrapper(repository: AuthRepository): SessionBootstrapper
}
