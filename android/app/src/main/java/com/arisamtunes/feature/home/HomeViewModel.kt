package com.arisamtunes.feature.home

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

data class HomeUiState(
    val isLoading: Boolean = true,
    val trending: List<SongDto> = emptyList(),
    val popular: List<SongDto> = emptyList(),
    val newReleases: List<SongDto> = emptyList(),
    val globalPlaylists: List<PlaylistDto> = emptyList(),
    val localPlaylists: List<PlaylistDto> = emptyList(),
    val userPlaylists: List<PlaylistDto> = emptyList(),
    val hasError: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (_state.value.isLoading && _state.value.trending.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, hasError = false)
            runCatching { repository.home() }
                .onSuccess { catalog ->
                    _state.value = HomeUiState(
                        isLoading = false, trending = catalog.trending, popular = catalog.popular,
                        newReleases = catalog.newReleases, globalPlaylists = catalog.globalPlaylists,
                        localPlaylists = catalog.localPlaylists,
                        userPlaylists = catalog.userPlaylists,
                    )
                }
                .onFailure { _state.value = _state.value.copy(isLoading = false, hasError = true) }
        }
    }
}
