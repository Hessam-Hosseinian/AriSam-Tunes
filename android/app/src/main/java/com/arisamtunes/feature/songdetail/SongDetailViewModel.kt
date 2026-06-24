package com.arisamtunes.feature.songdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.catalog.SongDto
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val songId: String = checkNotNull(savedStateHandle["songId"])
    private val _state = MutableStateFlow(SongDetailUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, hasError = false)
            runCatching { repository.song(songId) }
                .onSuccess { _state.value = _state.value.copy(song = it, isLoading = false, hasError = false) }
                .onFailure { _state.value = SongDetailUiState(isLoading = false, hasError = true) }
        }
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
}
