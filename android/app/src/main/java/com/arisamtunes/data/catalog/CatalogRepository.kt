package com.arisamtunes.data.catalog

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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
}
