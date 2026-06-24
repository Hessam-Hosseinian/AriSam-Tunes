package com.arisamtunes.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class SessionState { Checking, Authenticated, Unauthenticated }

interface SessionBootstrapper {
    suspend fun restoreOrRefreshSession(): Boolean
}

@Singleton
class PendingNetworkSessionBootstrapper @Inject constructor() : SessionBootstrapper {
    override suspend fun restoreOrRefreshSession(): Boolean = false
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds abstract fun bindSessionBootstrapper(implementation: PendingNetworkSessionBootstrapper): SessionBootstrapper
}

@HiltViewModel
class SessionViewModel @Inject constructor(private val bootstrapper: SessionBootstrapper) : ViewModel() {
    private val _state = MutableStateFlow(SessionState.Checking)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = if (bootstrapper.restoreOrRefreshSession()) SessionState.Authenticated else SessionState.Unauthenticated
        }
    }

    fun authenticated() { _state.value = SessionState.Authenticated }
    fun loggedOut() { _state.value = SessionState.Unauthenticated }
}
