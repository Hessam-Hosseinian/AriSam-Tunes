package com.arisamtunes.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
)

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("song_count") val songCount: Long = 0,
)

@Serializable
data class PlaylistListDto(val items: List<PlaylistDto>)
