package com.arisamtunes.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    @SerialName("artist_id") val artistId: String? = null,
    @SerialName("artist_name") val artistName: String,
    val album: String? = null,
    val genre: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("cover_image_url") val coverImageUrl: String,
    @SerialName("album_artist") val albumArtist: String? = null,
    @SerialName("track_number") val trackNumber: Int? = null,
    @SerialName("disc_number") val discNumber: Int? = null,
    @SerialName("bitrate_kbps") val bitrateKbps: Int? = null,
    @SerialName("sample_rate_hz") val sampleRateHz: Int? = null,
    val channels: String? = null,
    val codec: String? = null,
    @SerialName("file_format") val fileFormat: String? = null,
    @SerialName("release_year") val releaseYear: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val language: String? = null,
    val lyrics: String? = null,
    val composer: String? = null,
    val producer: String? = null,
    val copyright: String? = null,
    val publisher: String? = null,
    val mood: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("is_explicit") val isExplicit: Boolean = false,
    val popularity: Int = 0,
    @SerialName("play_count") val playCount: Long = 0,
    @SerialName("is_local") val isLocal: Boolean = false,
    @SerialName("is_demo") val isDemo: Boolean = false,
    @SerialName("source_file_name") val sourceFileName: String? = null,
    @SerialName("source_relative_path") val sourceRelativePath: String? = null,
    @SerialName("audio_file_size") val audioFileSize: Long? = null,
    @SerialName("cover_file_name") val coverFileName: String? = null,
    @SerialName("cover_relative_path") val coverRelativePath: String? = null,
    @SerialName("artist_image_url") val artistImageUrl: String? = null,
    @SerialName("album_cover_url") val albumCoverUrl: String? = null,
    @SerialName("extra_metadata") val extraMetadata: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class PlaylistDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String? = null,
    val name: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val scope: String = "USER",
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("song_count") val songCount: Long = 0,
)

@Serializable
data class PlaylistListDto(val items: List<PlaylistDto>)

@Serializable
data class PlaylistMutationDto(
    val name: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
)

@Serializable
data class PlaylistSongMutationDto(
    @SerialName("song_id") val songId: String,
)

@Serializable
data class PaginationDto(
    val page: Int,
    val size: Int,
    @SerialName("total_items") val totalItems: Long,
    @SerialName("total_pages") val totalPages: Int,
)

@Serializable
data class SongPageDto(val items: List<SongDto>, val pagination: PaginationDto)

@Serializable
data class PlaylistSongsDto(
    val playlist: PlaylistDto,
    val items: List<SongDto>,
    val pagination: PaginationDto,
)

@Serializable
data class SongSpectrumDto(
    @SerialName("song_id") val songId: String,
    val bands: Int,
    @SerialName("frame_duration_ms") val frameDurationMs: Int,
    val frames: List<List<Float>>,
)
