package com.arisamtunes.feature.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.FollowedArtistsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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
    val isFollowed: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ArtistViewModel @Inject constructor(
    private val artistCatalog: ArtistCatalog,
    private val catalogRepository: CatalogRepository,
    private val followedArtistsRepository: FollowedArtistsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId: String = checkNotNull(savedStateHandle["artistId"])
    private val _state = MutableStateFlow(ArtistUiState())
    val state = _state.asStateFlow()
    private val resolvedArtistId = MutableStateFlow<String?>(null)

    init {
        load()
        viewModelScope.launch {
            resolvedArtistId.filterNotNull()
                .flatMapLatest(followedArtistsRepository::observeIsFollowed)
                .collect { followed -> _state.value = _state.value.copy(isFollowed = followed) }
        }
    }

    fun load() = viewModelScope.launch {
        val artists = artistCatalog.all()
        val artist = artists.firstOrNull {
            it.profile.id == artistId || it.profile.name.equals(artistId, ignoreCase = true)
        }
        _state.value = ArtistUiState(
            artist = artist,
            artists = artists,
            isLoading = artist != null,
            isFollowed = _state.value.isFollowed,
        )
        if (artist == null) return@launch
        resolvedArtistId.value = artist.profile.id

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

    fun toggleFollow() = viewModelScope.launch {
        val artist = _state.value.artist ?: return@launch
        val target = !_state.value.isFollowed
        _state.value = _state.value.copy(isFollowed = target)
        runCatching {
            followedArtistsRepository.setFollowed(
                artistId = artist.profile.id,
                name = artist.profile.name,
                imageUri = artist.imageUri,
                followed = target,
            )
        }.onFailure {
            _state.value = _state.value.copy(isFollowed = !target)
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
