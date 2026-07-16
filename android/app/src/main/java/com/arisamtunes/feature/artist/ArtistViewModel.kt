package com.arisamtunes.feature.artist

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

data class ArtistAlbum(
    val name: String,
    val coverUrl: String,
    val trackCount: Int,
    val releaseYear: Int?,
)

data class ArtistUiState(
    val artist: LocalArtistProfile? = null,
    val artists: List<LocalArtistProfile> = emptyList(),
    val albums: List<ArtistAlbum> = emptyList(),
    val songs: List<SongDto> = emptyList(),
    val isLoading: Boolean = true,
    val hasCatalogError: Boolean = false,
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val artistCatalog: ArtistCatalog,
    private val catalogRepository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId: String = checkNotNull(savedStateHandle["artistId"])
    private val _state = MutableStateFlow(ArtistUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        val artists = artistCatalog.all()
        val artist = artists.firstOrNull {
            it.profile.id == artistId || it.profile.name.equals(artistId, ignoreCase = true)
        }
        _state.value = ArtistUiState(artist = artist, artists = artists, isLoading = artist != null)
        if (artist == null) return@launch

        runCatching { catalogRepository.search(artist.profile.name, "artist", page = 0, size = 100).items }
            .onSuccess { songs ->
                _state.value = _state.value.copy(
                    songs = songs,
                    albums = songs.toArtistAlbums(),
                    isLoading = false,
                )
            }
            .onFailure {
                _state.value = _state.value.copy(isLoading = false, hasCatalogError = true)
            }
    }
}

private fun List<SongDto>.toArtistAlbums(): List<ArtistAlbum> =
    groupBy { it.album?.takeIf(String::isNotBlank) }
        .filterKeys { it != null }
        .map { (name, songs) ->
            ArtistAlbum(
                name = checkNotNull(name),
                coverUrl = songs.firstNotNullOfOrNull { it.albumCoverUrl ?: it.coverImageUrl }.orEmpty(),
                trackCount = songs.size,
                releaseYear = songs.mapNotNull(SongDto::releaseYear).minOrNull(),
            )
        }
        .sortedBy { it.name }
