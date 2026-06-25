package com.arisamtunes.data.catalog

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

data class HomeCatalog(
    val trending: List<SongDto>,
    val popular: List<SongDto>,
    val newReleases: List<SongDto>,
    val globalPlaylists: List<PlaylistDto>,
    val localPlaylists: List<PlaylistDto>,
)

@Singleton
class CatalogRepository @Inject constructor(private val client: HttpClient) {
    suspend fun home(): HomeCatalog = coroutineScope {
        val trending = async { client.get("songs/trending").body<List<SongDto>>() }
        val popular = async { client.get("songs/popular").body<List<SongDto>>() }
        val newReleases = async { client.get("songs/new").body<List<SongDto>>() }
        val global = async { client.get("playlists/global").body<PlaylistListDto>().items }
        val local = async { client.get("playlists/local").body<PlaylistListDto>().items }
        HomeCatalog(trending.await(), popular.await(), newReleases.await(), global.await(), local.await())
    }

    suspend fun search(query: String, type: String, page: Int, size: Int): SongPageDto =
        client.get("songs/search") {
            parameter("q", query)
            parameter("type", type)
            parameter("page", page)
            parameter("size", size)
        }.body()

    @OptIn(ExperimentalPagingApi::class)
    fun searchPager(query: String, type: String) = SearchMemoryStore().let { store ->
        Pager(
            config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 5),
            remoteMediator = SearchRemoteMediator(query, type, this, store),
            pagingSourceFactory = store::pagingSource,
        ).flow
    }

    suspend fun playlists(): List<PlaylistDto> = client.get("playlists").body<PlaylistListDto>().items

    suspend fun playlist(id: String): PlaylistDto = client.get("playlists/$id").body()

    private suspend fun playlistSongs(id: String, page: Int, size: Int): PlaylistSongsDto =
        client.get("playlists/$id/songs") {
            parameter("page", page)
            parameter("size", size)
        }.body()

    fun playlistSongsPager(id: String) = Pager(
        config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 5),
        pagingSourceFactory = { PlaylistSongsPagingSource(id, this) },
    ).flow

    private class PlaylistSongsPagingSource(
        private val playlistId: String,
        private val repository: CatalogRepository,
    ) : PagingSource<Int, SongDto>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongDto> {
            val page = params.key ?: 0
            return runCatching { repository.playlistSongs(playlistId, page, params.loadSize.coerceAtMost(100)) }
                .fold(
                    onSuccess = { response -> LoadResult.Page(response.items, if (page == 0) null else page - 1, if (page + 1 < response.pagination.totalPages) page + 1 else null) },
                    onFailure = { LoadResult.Error(it) },
                )
        }

        override fun getRefreshKey(state: PagingState<Int, SongDto>): Int? = state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.let { page -> page.prevKey?.plus(1) ?: page.nextKey?.minus(1) }
        }
    }
}
