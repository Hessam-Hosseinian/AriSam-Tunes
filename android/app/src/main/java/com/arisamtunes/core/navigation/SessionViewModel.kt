package com.arisamtunes.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.preferences.UserPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SessionState { Checking, Authenticated, MusicSuggestions, Unauthenticated }

interface SessionBootstrapper {
    suspend fun restoreOrRefreshSession(): Boolean
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val bootstrapper: SessionBootstrapper,
    private val preferencesStore: UserPreferencesStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SessionState.Checking)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = when {
                !bootstrapper.restoreOrRefreshSession() -> SessionState.Unauthenticated
                preferencesStore.shouldShowMusicSuggestions() -> SessionState.MusicSuggestions
                else -> SessionState.Authenticated
            }
        }
    }

    fun authenticated() {
        viewModelScope.launch {
            _state.value = if (preferencesStore.shouldShowMusicSuggestions()) {
                SessionState.MusicSuggestions
            } else {
                SessionState.Authenticated
            }
        }
    }

    fun musicSuggestionsShown() {
        viewModelScope.launch {
            preferencesStore.markMusicSuggestionsShown()
            _state.value = SessionState.Authenticated
        }
    }

    fun loggedOut() { _state.value = SessionState.Unauthenticated }
}
