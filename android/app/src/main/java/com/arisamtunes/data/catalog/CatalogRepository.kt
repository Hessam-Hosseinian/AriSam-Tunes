package com.arisamtunes.data.catalog

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
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
}
