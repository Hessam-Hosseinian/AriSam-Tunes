package com.arisamtunes.feature.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.social.PublicUserDto
import com.arisamtunes.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialProfileUiState(
    val isLoading: Boolean = true,
    val user: PublicUserDto? = null,
    val playlists: List<PlaylistDto> = emptyList(),
    val hasError: Boolean = false,
    val isUpdatingFollow: Boolean = false,
    val isOwnProfile: Boolean = false,
)

@HiltViewModel
class SocialProfileViewModel @Inject constructor(
    private val repository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val userId: String? = savedStateHandle["userId"]
    private val _state = MutableStateFlow(SocialProfileUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, hasError = false)
        runCatching {
            val user = if (userId.isNullOrBlank()) repository.currentUser() else repository.user(userId)
            val playlists = async { repository.publicPlaylists(user.id) }
            user to playlists.await()
        }.onSuccess { (user, playlists) ->
            _state.value = SocialProfileUiState(
                isLoading = false,
                user = user,
                playlists = playlists,
                isOwnProfile = userId.isNullOrBlank(),
            )
        }.onFailure {
            _state.value = _state.value.copy(isLoading = false, hasError = true)
        }
    }

    fun toggleFollow() = viewModelScope.launch {
        val user = _state.value.user ?: return@launch
        if (_state.value.isOwnProfile) return@launch
        if (_state.value.isUpdatingFollow) return@launch
        _state.value = _state.value.copy(isUpdatingFollow = true)
        runCatching { if (user.isFollowing) repository.unfollow(user.id) else repository.follow(user.id) }
            .onSuccess { updated -> _state.value = _state.value.copy(user = updated, isUpdatingFollow = false) }
            .onFailure { _state.value = _state.value.copy(isUpdatingFollow = false, hasError = true) }
    }
}

enum class SocialListKind { Following, Followers }

data class SocialUsersUiState(
    val isLoading: Boolean = true,
    val title: SocialListKind = SocialListKind.Following,
    val users: List<PublicUserDto> = emptyList(),
    val hasError: Boolean = false,
)

@HiltViewModel
class SocialUsersViewModel @Inject constructor(
    private val repository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val userId: String? = savedStateHandle["userId"]
    private val kind = when (savedStateHandle.get<String>("kind")) {
        "followers" -> SocialListKind.Followers
        else -> SocialListKind.Following
    }
    private val _state = MutableStateFlow(SocialUsersUiState(title = kind))
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, hasError = false)
        runCatching {
            when (kind) {
                SocialListKind.Following -> repository.following(userId)
                SocialListKind.Followers -> repository.followers(userId)
            }
        }.onSuccess { users ->
            _state.value = SocialUsersUiState(isLoading = false, title = kind, users = users)
        }.onFailure {
            _state.value = _state.value.copy(isLoading = false, hasError = true)
        }
    }
}
