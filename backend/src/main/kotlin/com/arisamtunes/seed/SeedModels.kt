package com.arisamtunes.seed

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

enum class CoverSource { EMBEDDED, FOLDER, NONE }

data class ExtractedSong(
    val source: DiscoveredAudio,
    val title: String,
    val artist: String,
    val albumArtist: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val comment: String? = null,
    val composer: String? = null,
    val lyrics: String? = null,
    val language: String? = null,
    val mood: String? = null,
    val bpm: Int? = null,
    val musicalKey: String? = null,
    val grouping: String? = null,
    val publisher: String? = null,
    val copyright: String? = null,
    val encoder: String? = null,
    val officialArtistUrl: String? = null,
    val durationSeconds: Int,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val channels: String? = null,
    val codec: String? = null,
    val coverFileName: String? = null,
    val coverSource: CoverSource = CoverSource.NONE,
    val usedFallbackFields: Set<String> = emptySet(),
    val extraMetadata: JsonObject = buildJsonObject {},
    val isDemo: Boolean = false,
)

data class SeedSummary(
    var totalFound: Int = 0,
    var duplicatesSkipped: Int = 0,
    var fullyExtracted: Int = 0,
    var partiallyExtracted: Int = 0,
    var failed: Int = 0,
    var demoGenerated: Int = 0,
    var embeddedCovers: Int = 0,
    var folderCovers: Int = 0,
    var noCovers: Int = 0,
)
