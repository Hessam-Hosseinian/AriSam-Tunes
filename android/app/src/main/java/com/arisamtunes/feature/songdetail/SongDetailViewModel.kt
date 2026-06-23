package com.arisamtunes.feature.songdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.catalog.CatalogRepository
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
                .onSuccess { _state.value = SongDetailUiState(song = it, isLoading = false) }
                .onFailure { _state.value = SongDetailUiState(isLoading = false, hasError = true) }
        }
    }
}
