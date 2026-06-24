package com.arisamtunes.playlist

import com.arisamtunes.catalog.SongResponse
import com.arisamtunes.model.PaginationMeta
import kotlinx.serialization.Serializable

@Serializable
enum class PlaylistScope { GLOBAL, LOCAL, USER }

@Serializable
data class PlaylistResponse(
    val id: String,
    val ownerId: String?,
    val name: String,
    val description: String?,
    val coverImageUrl: String?,
    val scope: PlaylistScope,
    val isPublic: Boolean,
    val songCount: Long,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class PlaylistListResponse(val items: List<PlaylistResponse>)

@Serializable
data class PlaylistSongsResponse(
    val playlist: PlaylistResponse,
    val items: List<SongResponse>,
    val pagination: PaginationMeta,
)

@Serializable
data class CreatePlaylistRequest(
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val isPublic: Boolean = false,
)

@Serializable
data class UpdatePlaylistRequest(
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val isPublic: Boolean = false,
)

@Serializable
data class PlaylistSongRequest(
    val songId: String,
)
