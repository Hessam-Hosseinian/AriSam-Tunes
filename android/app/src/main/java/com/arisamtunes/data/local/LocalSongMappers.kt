package com.arisamtunes.data.local

import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.entity.CachedSongEntity
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import com.arisamtunes.data.local.entity.LikedSongEntity
import com.arisamtunes.data.local.entity.RecentlyPlayedEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private val metadataJson = Json { ignoreUnknownKeys = true }

fun SongDto.toCachedSongEntity(cachedAt: Long = System.currentTimeMillis()) = CachedSongEntity(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    album = album,
    albumArtist = albumArtist,
    genre = genre,
    durationSeconds = durationSeconds,
    audioUrl = audioUrl,
    coverImageUrl = coverImageUrl,
    trackNumber = trackNumber,
    discNumber = discNumber,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    channels = channels,
    codec = codec,
    fileFormat = fileFormat,
    releaseYear = releaseYear,
    releaseDate = releaseDate,
    language = language,
    composer = composer,
    producer = producer,
    mood = mood,
    tags = tags,
    popularity = popularity,
    playCount = playCount,
    isExplicit = isExplicit,
    isLocal = isLocal,
    isDemo = isDemo,
    audioFileSize = audioFileSize,
    extraMetadataJson = metadataJson.encodeToString(extraMetadata),
    cachedAt = cachedAt,
)

fun SongDto.toLikedSongEntity(likedAt: Long = System.currentTimeMillis()) = LikedSongEntity(
    songId = id,
    title = title,
    artistName = artistName,
    album = album,
    audioUrl = audioUrl,
    coverImageUrl = coverImageUrl,
    durationSeconds = durationSeconds,
    likedAt = likedAt,
)

fun SongDto.toRecentlyPlayedEntity(positionSeconds: Int = 0, playedAt: Long = System.currentTimeMillis()) = RecentlyPlayedEntity(
    songId = id,
    title = title,
    artistName = artistName,
    album = album,
    audioUrl = audioUrl,
    coverImageUrl = coverImageUrl,
    durationSeconds = durationSeconds,
    lastPositionSeconds = positionSeconds,
    playedAt = playedAt,
)

fun SongDto.toDownloadedSongEntity(
    ownerUserId: String,
    localFilePath: String,
    state: String,
    progress: Int = 0,
    failureReason: String? = null,
    downloadedAt: Long = System.currentTimeMillis(),
) = DownloadedSongEntity(
    ownerUserId = ownerUserId,
    songId = id,
    title = title,
    artistName = artistName,
    album = album,
    audioUrl = audioUrl,
    coverImageUrl = coverImageUrl,
    durationSeconds = durationSeconds,
    lyrics = lyrics,
    extraMetadataJson = metadataJson.encodeToString(extraMetadata),
    localFilePath = localFilePath,
    mimeType = fileFormat?.lowercase()?.let { "audio/$it" },
    fileSizeBytes = audioFileSize,
    downloadState = state,
    downloadProgress = progress,
    failureReason = failureReason,
    downloadedAt = downloadedAt,
)

fun DownloadedSongEntity.toSongDto() = SongDto(
    id = songId,
    title = title,
    artistName = artistName,
    album = album,
    durationSeconds = durationSeconds,
    audioUrl = audioUrl,
    coverImageUrl = coverImageUrl.orEmpty(),
    lyrics = lyrics,
    extraMetadata = runCatching {
        metadataJson.decodeFromString<JsonObject>(extraMetadataJson)
    }.getOrDefault(JsonObject(emptyMap())),
)

fun CachedSongEntity.toSongDto() = SongDto(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    album = album,
    genre = genre,
    durationSeconds = durationSeconds,
    audioUrl = audioUrl,
    coverImageUrl = coverImageUrl,
    albumArtist = albumArtist,
    trackNumber = trackNumber,
    discNumber = discNumber,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    channels = channels,
    codec = codec,
    fileFormat = fileFormat,
    releaseYear = releaseYear,
    releaseDate = releaseDate,
    language = language,
    composer = composer,
    producer = producer,
    mood = mood,
    tags = tags,
    popularity = popularity,
    playCount = playCount,
    isExplicit = isExplicit,
    isLocal = isLocal,
    isDemo = isDemo,
    audioFileSize = audioFileSize,
    extraMetadata = runCatching { metadataJson.decodeFromString<JsonObject>(extraMetadataJson) }.getOrDefault(JsonObject(emptyMap())),
)
