package com.arisamtunes.catalog

import com.arisamtunes.model.PaginationMeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SongResponse(
    val id: String,
    val artistId: String?,
    val title: String,
    val artistName: String,
    val album: String?,
    val albumArtist: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val genre: String?,
    val durationSeconds: Int,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val channels: String?,
    val codec: String?,
    val fileFormat: String?,
    val releaseYear: Int?,
    val releaseDate: String?,
    val language: String?,
    val lyrics: String?,
    val composer: String?,
    val producer: String?,
    val copyright: String?,
    val publisher: String?,
    val mood: String?,
    val tags: List<String>,
    val isExplicit: Boolean,
    val popularity: Int,
    val playCount: Long,
    val isLocal: Boolean,
    val isDemo: Boolean,
    val sourceFileName: String?,
    val sourceRelativePath: String?,
    val audioFileSize: Long?,
    val coverFileName: String?,
    val coverRelativePath: String?,
    val audioUrl: String,
    val coverImageUrl: String,
    val artistImageUrl: String?,
    val albumCoverUrl: String?,
    val extraMetadata: JsonObject,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ArtistResponse(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val biography: String?,
    val songCount: Long,
    val extraMetadata: JsonObject,
    val createdAt: String,
)

@Serializable
data class SongPageResponse(val items: List<SongResponse>, val pagination: PaginationMeta)

@Serializable
data class ArtistListResponse(val items: List<ArtistResponse>)
