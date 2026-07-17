package com.arisamtunes.feature.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MusicSuggestionsUiState(
    val isLoading: Boolean = true,
    val songs: List<SongDto> = emptyList(),
    val hasError: Boolean = false,
)

@HiltViewModel
class MusicSuggestionsViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MusicSuggestionsUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.isLoading && _state.value.songs.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, hasError = false)
            runCatching { catalogRepository.allSongs() }
                .onSuccess { songs ->
                    _state.value = MusicSuggestionsUiState(
                        isLoading = false,
                        songs = songs,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoading = false, hasError = true)
                }
        }
    }

}
