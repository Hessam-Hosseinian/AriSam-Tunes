package com.arisamtunes.feature.songdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.LocalLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongDetailUiState(
    val song: SongDto? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val playlists: List<PlaylistDto> = emptyList(),
    val showPlaylistPicker: Boolean = false,
    val isAddingToPlaylist: Boolean = false,
    val playlistActionDone: Boolean = false,
    val isLiked: Boolean = false,
)

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val localLibraryRepository: LocalLibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val songId: String = checkNotNull(savedStateHandle["songId"])
    private val _state = MutableStateFlow(SongDetailUiState())
    val state = _state.asStateFlow()
    private var likeObserverJob: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, hasError = false)
            runCatching { repository.song(songId) }
                .onSuccess {
                    _state.value = _state.value.copy(song = it, isLoading = false, hasError = false)
                    observeLikeState(it.id)
                }
                .onFailure { _state.value = SongDetailUiState(isLoading = false, hasError = true) }
        }
    }

    fun toggleLike() = viewModelScope.launch {
        val song = _state.value.song ?: return@launch
        localLibraryRepository.toggleLiked(song)
    }

    fun openPlaylistPicker() = viewModelScope.launch {
        _state.value = _state.value.copy(showPlaylistPicker = true, playlistActionDone = false)
        runCatching { repository.playlists().filter { it.scope == "USER" } }
            .onSuccess { playlists -> _state.value = _state.value.copy(playlists = playlists) }
            .onFailure { _state.value = _state.value.copy(hasError = true) }
    }

    fun closePlaylistPicker() {
        _state.value = _state.value.copy(showPlaylistPicker = false, isAddingToPlaylist = false, playlistActionDone = false)
    }

    fun addToPlaylist(playlist: PlaylistDto) = viewModelScope.launch {
        _state.value = _state.value.copy(isAddingToPlaylist = true, playlistActionDone = false)
        runCatching { repository.addSongToPlaylist(playlist.id, songId) }
            .onSuccess {
                _state.value = _state.value.copy(
                    isAddingToPlaylist = false,
                    showPlaylistPicker = false,
                    playlistActionDone = true,
                )
            }
            .onFailure { _state.value = _state.value.copy(isAddingToPlaylist = false, hasError = true) }
    }

    private fun observeLikeState(id: String) {
        likeObserverJob?.cancel()
        likeObserverJob = viewModelScope.launch {
            localLibraryRepository.observeIsLiked(id).collect { liked ->
                _state.value = _state.value.copy(isLiked = liked)
            }
        }
    }
}
