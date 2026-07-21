package com.arisamtunes.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.SearchHistoryEntry
import com.arisamtunes.data.local.SearchHistoryRepository
import com.arisamtunes.feature.artist.ArtistCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val recentSearches: List<SearchHistoryEntry> = emptyList(),
)

private data class SearchCriteria(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.All,
    val artists: List<SearchArtistResult> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    artistCatalog: ArtistCatalog,
) : ViewModel() {
    private val allArtists = artistCatalog.all().map { artist ->
        SearchArtistResult(
            id = artist.profile.id,
            name = artist.profile.name,
            imageUrl = artist.imageUri,
        )
    }
    private val criteria = MutableStateFlow(SearchCriteria())
    val state = combine(criteria, searchHistoryRepository.observeRecent()) { current, history ->
        SearchUiState(
            query = current.query,
            filter = current.filter,
            artists = current.artists,
            recentSearches = history,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    val results = criteria
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { current ->
            val query = current.query.trim()
            if (query.isBlank()) {
                flowOf(PagingData.empty<SongDto>())
            } else {
                searchHistoryRepository.record(query, current.filter.apiValue)
                if (current.filter == SearchFilter.Artist) {
                    flowOf(PagingData.empty<SongDto>())
                } else {
                    repository.searchPager(query, current.filter.apiValue)
                }
            }
        }
        .cachedIn(viewModelScope)

    fun updateQuery(value: String) {
        if (value.length <= 200) updateCriteria(query = value, filter = criteria.value.filter)
    }

    fun updateFilter(value: SearchFilter) {
        updateCriteria(query = criteria.value.query, filter = value)
    }

    fun selectRecentSearch(entry: SearchHistoryEntry) {
        updateCriteria(
            query = entry.query,
            filter = SearchFilter.entries.firstOrNull { it.apiValue == entry.filter } ?: SearchFilter.All,
        )
    }

    fun deleteRecentSearch(id: Long) {
        viewModelScope.launch { searchHistoryRepository.delete(id) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { searchHistoryRepository.clear() }
    }

    private fun updateCriteria(query: String, filter: SearchFilter) {
        val normalizedQuery = query.normalizedForSearch()
        val artists = if (normalizedQuery.isNotBlank() && filter in setOf(SearchFilter.All, SearchFilter.Artist)) {
            allArtists.filter { it.name.normalizedForSearch().contains(normalizedQuery) }
        } else {
            emptyList()
        }
        criteria.value = SearchCriteria(query = query, filter = filter, artists = artists)
    }
}

private fun String.normalizedForSearch(): String = trim()
    .lowercase()
    .replace('ي', 'ی')
    .replace('ك', 'ک')
