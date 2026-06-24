package com.arisamtunes.data.catalog

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator

internal class SearchMemoryStore {
    private val lock = Any()
    private var items: List<SongDto> = emptyList()
    private val sources = mutableSetOf<SearchPagingSource>()
    var nextPage: Int = 0
        private set

    fun pagingSource() = SearchPagingSource(this).also { source ->
        synchronized(lock) { sources += source }
        source.registerInvalidatedCallback { synchronized(lock) { sources -= source } }
    }

    fun snapshot(): List<SongDto> = synchronized(lock) { items }

    fun update(newItems: List<SongDto>, refresh: Boolean, loadedPage: Int) {
        val toInvalidate = synchronized(lock) {
            items = if (refresh) newItems else (items + newItems).distinctBy(SongDto::id)
            nextPage = loadedPage + 1
            sources.toList()
        }
        toInvalidate.forEach(PagingSource<Int, SongDto>::invalidate)
    }
}

internal class SearchPagingSource(private val store: SearchMemoryStore) : PagingSource<Int, SongDto>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongDto> =
        LoadResult.Page(data = store.snapshot(), prevKey = null, nextKey = null)

    override fun getRefreshKey(state: PagingState<Int, SongDto>): Int? = null
}

@OptIn(ExperimentalPagingApi::class)
internal class SearchRemoteMediator(
    private val query: String,
    private val type: String,
    private val repository: CatalogRepository,
    private val store: SearchMemoryStore,
) : RemoteMediator<Int, SongDto>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, SongDto>): MediatorResult {
        if (loadType == LoadType.PREPEND) return MediatorResult.Success(endOfPaginationReached = true)
        val page = if (loadType == LoadType.REFRESH) 0 else store.nextPage
        return runCatching { repository.search(query, type, page, state.config.pageSize) }
            .fold(
                onSuccess = { response ->
                    store.update(response.items, refresh = loadType == LoadType.REFRESH, loadedPage = page)
                    MediatorResult.Success(response.pagination.totalPages == 0 || page >= response.pagination.totalPages - 1)
                },
                onFailure = MediatorResult::Error,
            )
    }
}
