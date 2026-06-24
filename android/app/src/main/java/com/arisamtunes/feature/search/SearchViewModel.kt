package com.arisamtunes.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
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

data class SearchUiState(val query: String = "", val filter: SearchFilter = SearchFilter.All)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(repository: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    val results = _state
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { criteria ->
            if (criteria.query.isBlank()) flowOf(PagingData.empty<SongDto>())
            else repository.searchPager(criteria.query.trim(), criteria.filter.apiValue)
        }
        .cachedIn(viewModelScope)

    fun updateQuery(value: String) { if (value.length <= 200) _state.value = _state.value.copy(query = value) }
    fun updateFilter(value: SearchFilter) { _state.value = _state.value.copy(filter = value) }
}
