package com.arisamtunes.feature.social

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.social.PublicUserDto
import com.arisamtunes.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SocialEffect {
    data object ActionFailed : SocialEffect
    data object AvatarUpdated : SocialEffect
    data object AvatarUploadFailed : SocialEffect
}

data class SocialProfileUiState(
    val isLoading: Boolean = true,
    val user: PublicUserDto? = null,
    val hasError: Boolean = false,
    val isUpdatingFollow: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isOwnProfile: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SocialProfileViewModel @Inject constructor(
    private val repository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val requestedUserId: String? = savedStateHandle["userId"]
    private val _state = MutableStateFlow(SocialProfileUiState())
    val state = _state.asStateFlow()
    private val loadedUserId = MutableStateFlow<String?>(null)
    val playlists: Flow<PagingData<PlaylistDto>> = loadedUserId.filterNotNull()
        .flatMapLatest(repository::publicPlaylistsPager)
        .cachedIn(viewModelScope)
    private val _effects = Channel<SocialEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, hasError = false)
        runCatching { if (requestedUserId.isNullOrBlank()) repository.currentUser() else repository.user(requestedUserId) }
            .onSuccess { user ->
                loadedUserId.value = user.id
                _state.value = SocialProfileUiState(user = user, isLoading = false, isOwnProfile = requestedUserId.isNullOrBlank())
            }
            .onFailure { _state.value = _state.value.copy(isLoading = false, hasError = true) }
    }

    fun toggleFollow() = viewModelScope.launch {
        val before = _state.value
        val user = before.user ?: return@launch
        if (before.isOwnProfile || before.isUpdatingFollow) return@launch
        _state.update { it.copy(user = user.optimisticFollowToggle(), isUpdatingFollow = true) }
        try {
            val updated = if (user.isFollowing) repository.unfollow(user.id) else repository.follow(user.id)
            _state.update { it.copy(user = updated) }
        } catch (cancelled: CancellationException) {
            _state.update { it.copy(user = user) }
            throw cancelled
        } catch (_: Throwable) {
            _state.update { it.copy(user = user) }
            _effects.send(SocialEffect.ActionFailed)
        } finally {
            _state.update { it.copy(isUpdatingFollow = false) }
        }
    }

    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        if (!_state.value.isOwnProfile || _state.value.isUploadingAvatar) return@launch
        _state.value = _state.value.copy(isUploadingAvatar = true)
        runCatching { repository.uploadAvatar(uri) }
            .onSuccess { user ->
                _state.value = _state.value.copy(user = user, isUploadingAvatar = false)
                _effects.send(SocialEffect.AvatarUpdated)
            }
            .onFailure {
                _state.value = _state.value.copy(isUploadingAvatar = false)
                _effects.send(SocialEffect.AvatarUploadFailed)
            }
    }
}

enum class SocialListKind { Following, Followers }

data class SocialUsersUiState(
    val title: SocialListKind = SocialListKind.Following,
    val updatingUserIds: Set<String> = emptySet(),
)

@HiltViewModel
class SocialUsersViewModel @Inject constructor(
    private val repository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val userId: String? = savedStateHandle["userId"]
    private val kind = if (savedStateHandle.get<String>("kind") == "followers") SocialListKind.Followers else SocialListKind.Following
    private val _state = MutableStateFlow(SocialUsersUiState(title = kind))
    val state = _state.asStateFlow()
    private val overrides = MutableStateFlow<Map<String, PublicUserDto>>(emptyMap())
    private val source = if (kind == SocialListKind.Followers) repository.followersPager(userId) else repository.followingPager(userId)
    val users: Flow<PagingData<PublicUserDto>> = source.combine(overrides) { page, updates ->
        page.map { updates[it.id] ?: it }
    }.cachedIn(viewModelScope)
    private val _effects = Channel<SocialEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun toggleFollow(user: PublicUserDto) = viewModelScope.launch {
        if (user.id in _state.value.updatingUserIds) return@launch
        val before = overrides.value[user.id] ?: user
        _state.update { it.copy(updatingUserIds = it.updatingUserIds + user.id) }
        overrides.update { it + (user.id to before.optimisticFollowToggle()) }
        try {
            val updated = if (before.isFollowing) repository.unfollow(user.id) else repository.follow(user.id)
            overrides.update { it + (updated.id to updated) }
        } catch (cancelled: CancellationException) {
            overrides.update { it + (user.id to before) }
            throw cancelled
        } catch (_: Throwable) {
            overrides.update { it + (user.id to before) }
            _effects.send(SocialEffect.ActionFailed)
        } finally {
            _state.update { it.copy(updatingUserIds = it.updatingUserIds - user.id) }
        }
    }
}

internal fun PublicUserDto.optimisticFollowToggle(): PublicUserDto {
    val willFollow = !isFollowing
    val followerDelta = if (willFollow) 1 else -1
    return copy(
        isFollowing = willFollow,
        followersCount = (followersCount + followerDelta).coerceAtLeast(0),
    )
}
