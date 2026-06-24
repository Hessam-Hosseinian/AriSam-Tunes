package com.arisamtunes.data.catalog

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.map
import com.arisamtunes.data.local.AriSamDatabase
import com.arisamtunes.data.local.dao.CachedSongDao
import com.arisamtunes.data.local.dao.RemoteKeyDao
import com.arisamtunes.data.local.toSongDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
class CatalogRepository @Inject constructor(
    private val client: HttpClient,
    private val database: AriSamDatabase,
    private val cachedSongDao: CachedSongDao,
    private val remoteKeyDao: RemoteKeyDao,
) {
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

    suspend fun songs(page: Int, size: Int): SongPageDto =
        client.get("songs") {
            parameter("page", page)
            parameter("size", size)
        }.body()

    suspend fun song(id: String): SongDto = client.get("songs/$id").body()

    @OptIn(ExperimentalPagingApi::class)
    fun songsPager(): Flow<PagingData<SongDto>> = Pager(
        config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 5),
        remoteMediator = CachedCatalogRemoteMediator(
            scope = ScopeSongs,
            cacheKey = CacheKeyAllSongs,
            database = database,
            remoteKeyDao = remoteKeyDao,
            loadPage = ::songs,
        ),
        pagingSourceFactory = cachedSongDao::pagingSource,
    ).flow.map { pagingData -> pagingData.map { it.toSongDto() } }

    @OptIn(ExperimentalPagingApi::class)
    fun searchPager(query: String, type: String): Flow<PagingData<SongDto>> =
        Pager(
            config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 5),
            remoteMediator = CachedCatalogRemoteMediator(
                scope = ScopeSearch,
                cacheKey = "$type:${query.trim().lowercase()}",
                database = database,
                remoteKeyDao = remoteKeyDao,
                loadPage = { page, size -> search(query, type, page, size) },
            ),
            pagingSourceFactory = { cachedSongDao.searchPagingSource(query, type) },
        ).flow.map { pagingData -> pagingData.map { it.toSongDto() } }

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

    private companion object {
        const val ScopeSongs = "songs"
        const val ScopeSearch = "song_search"
        const val CacheKeyAllSongs = "all"
    }
}
