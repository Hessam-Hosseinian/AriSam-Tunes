package com.arisamtunes.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.feature.artist.ArtistCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

enum class SearchFilter(val apiValue: String) { All("all"), Song("title"), Artist("artist"), Album("album"), Genre("genre") }

data class SearchArtistResult(
    val id: String,
    val name: String,
    val imageUrl: String,
)

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.All,
    val artists: List<SearchArtistResult> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: CatalogRepository,
    artistCatalog: ArtistCatalog,
) : ViewModel() {
    private val allArtists = artistCatalog.all().map { artist ->
        SearchArtistResult(
            id = artist.profile.id,
            name = artist.profile.name,
            imageUrl = artist.imageUri,
        )
    }
    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    val results = _state
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { criteria ->
            if (criteria.query.isBlank() || criteria.filter == SearchFilter.Artist) flowOf(PagingData.empty<SongDto>())
            else repository.searchPager(criteria.query.trim(), criteria.filter.apiValue)
        }
        .cachedIn(viewModelScope)

    fun updateQuery(value: String) {
        if (value.length <= 200) updateCriteria(query = value, filter = _state.value.filter)
    }

    fun updateFilter(value: SearchFilter) {
        updateCriteria(query = _state.value.query, filter = value)
    }

    private fun updateCriteria(query: String, filter: SearchFilter) {
        val normalizedQuery = query.normalizedForSearch()
        val artists = if (normalizedQuery.isNotBlank() && filter in setOf(SearchFilter.All, SearchFilter.Artist)) {
            allArtists.filter { it.name.normalizedForSearch().contains(normalizedQuery) }
        } else {
            emptyList()
        }
        _state.value = SearchUiState(query = query, filter = filter, artists = artists)
    }
}

private fun String.normalizedForSearch(): String = trim()
    .lowercase()
    .replace('ي', 'ی')
    .replace('ك', 'ک')
