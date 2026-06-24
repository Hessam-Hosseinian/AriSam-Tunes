package com.arisamtunes.feature.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.catalog.SongDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val items: List<PlaylistDto> = emptyList(),
    val hasError: Boolean = false,
    val isSaving: Boolean = false,
    val editingPlaylist: PlaylistDto? = null,
    val showEditor: Boolean = false,
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(private val repository: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(PlaylistsUiState())
    val state = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, hasError = false)
        runCatching { repository.playlists() }
            .onSuccess { _state.value = _state.value.copy(isLoading = false, items = it, hasError = false) }
            .onFailure { _state.value = _state.value.copy(isLoading = false, hasError = true) }
    }

    fun openCreate() {
        _state.value = _state.value.copy(showEditor = true, editingPlaylist = null)
    }

    fun openEdit(playlist: PlaylistDto) {
        _state.value = _state.value.copy(showEditor = true, editingPlaylist = playlist)
    }

    fun closeEditor() {
        _state.value = _state.value.copy(showEditor = false, editingPlaylist = null, isSaving = false)
    }

    fun savePlaylist(name: String, description: String?, isPublic: Boolean) = viewModelScope.launch {
        val safeName = name.trim()
        if (safeName.isBlank()) return@launch
        val current = _state.value
        _state.value = current.copy(isSaving = true, hasError = false)
        runCatching {
            current.editingPlaylist?.let { playlist ->
                repository.updatePlaylist(playlist.id, safeName, description?.trim(), isPublic)
            } ?: repository.createPlaylist(safeName, description?.trim(), isPublic)
            repository.playlists()
        }.onSuccess { playlists ->
            _state.value = _state.value.copy(items = playlists, isSaving = false, showEditor = false, editingPlaylist = null)
        }.onFailure {
            _state.value = _state.value.copy(isSaving = false, hasError = true)
        }
    }

    fun deletePlaylist(playlist: PlaylistDto) = viewModelScope.launch {
        _state.value = _state.value.copy(isSaving = true, hasError = false)
        runCatching {
            repository.deletePlaylist(playlist.id)
            repository.playlists()
        }.onSuccess { playlists ->
            _state.value = _state.value.copy(items = playlists, isSaving = false, showEditor = false, editingPlaylist = null)
        }.onFailure {
            _state.value = _state.value.copy(isSaving = false, hasError = true)
        }
    }
}

data class PlaylistDetailUiState(val playlist: PlaylistDto? = null, val isLoading: Boolean = true, val hasError: Boolean = false)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])
    private val _state = MutableStateFlow(PlaylistDetailUiState())
    val state = _state.asStateFlow()
    val songs = repository.playlistSongsPager(playlistId).cachedIn(viewModelScope)
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, hasError = false)
        runCatching { repository.playlist(playlistId) }
            .onSuccess { _state.value = PlaylistDetailUiState(playlist = it, isLoading = false) }
            .onFailure { _state.value = PlaylistDetailUiState(isLoading = false, hasError = true) }
    }

    fun removeSong(songId: String, onDone: () -> Unit = {}) = viewModelScope.launch {
        runCatching { repository.removeSongFromPlaylist(playlistId, songId) }
            .onSuccess { updated ->
                _state.value = _state.value.copy(playlist = updated)
                onDone()
            }
            .onFailure { _state.value = _state.value.copy(hasError = true) }
    }
}
