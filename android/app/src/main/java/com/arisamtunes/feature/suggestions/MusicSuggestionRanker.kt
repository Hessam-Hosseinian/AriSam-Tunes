package com.arisamtunes.feature.suggestions

import com.arisamtunes.data.catalog.SongDto
import kotlin.math.abs

/**
 * Lightweight content-based recommendations for a brand-new account with no listening history.
 * Selected songs remain at the front; candidates are scored from their shared metadata and then
 * diversified so a single artist cannot take over the generated playlist.
 */
object MusicSuggestionRanker {
    fun buildPlaylist(
        allSongs: List<SongDto>,
        selectedSongIds: Set<String>,
        targetSize: Int = DefaultTargetSize,
    ): List<SongDto> {
        val uniqueSongs = allSongs.distinctBy(SongDto::id)
        val selected = uniqueSongs.filter { it.id in selectedSongIds }
        if (selected.isEmpty()) return emptyList()

        val selectedIds = selected.mapTo(mutableSetOf(), SongDto::id)
        val rankedCandidates = uniqueSongs.asSequence()
            .filterNot { it.id in selectedIds }
            .map { song -> RankedSong(song, selected.maxOf { seed -> similarity(seed, song) }) }
            .sortedWith(
                compareByDescending<RankedSong> { it.score }
                    .thenByDescending { it.song.popularity }
                    .thenBy { it.song.title },
            )
            .toList()

        val result = selected.toMutableList()
        val artistCounts = selected.groupingBy { it.artistKey() }.eachCount().toMutableMap()
        val desiredSize = maxOf(targetSize, selected.size)
        val deferred = mutableListOf<SongDto>()
        rankedCandidates.forEach { ranked ->
            if (result.size >= desiredSize) return@forEach
            val artistKey = ranked.song.artistKey()
            if ((artistCounts[artistKey] ?: 0) < MaxSongsPerArtist) {
                result += ranked.song
                artistCounts[artistKey] = (artistCounts[artistKey] ?: 0) + 1
            } else {
                deferred += ranked.song
            }
        }
        deferred.forEach { song ->
            if (result.size < desiredSize) result += song
        }
        return result
    }

    private fun similarity(seed: SongDto, candidate: SongDto): Double {
        var score = 0.0
        if (seed.artistKey() == candidate.artistKey()) score += 5.0
        if (seed.genre.matches(candidate.genre)) score += 4.0
        if (seed.mood.matches(candidate.mood)) score += 2.5
        if (seed.language.matches(candidate.language)) score += 2.0
        if (seed.album.matches(candidate.album)) score += 1.5
        if (seed.composer.matches(candidate.composer)) score += 1.0
        score += tagSimilarity(seed.tags, candidate.tags) * 4.0
        score += releaseYearSimilarity(seed.releaseYear, candidate.releaseYear)
        score += candidate.popularity.coerceIn(0, 100) / 100.0
        if (!seed.isExplicit && candidate.isExplicit) score -= .75
        return score
    }

    private fun tagSimilarity(left: List<String>, right: List<String>): Double {
        val leftTags = left.mapNotNullTo(mutableSetOf()) { it.normalized() }
        val rightTags = right.mapNotNullTo(mutableSetOf()) { it.normalized() }
        if (leftTags.isEmpty() || rightTags.isEmpty()) return 0.0
        return leftTags.intersect(rightTags).size.toDouble() / leftTags.union(rightTags).size
    }

    private fun releaseYearSimilarity(left: Int?, right: Int?): Double {
        if (left == null || right == null) return 0.0
        return when (abs(left - right)) {
            in 0..2 -> 1.0
            in 3..5 -> .6
            in 6..10 -> .25
            else -> 0.0
        }
    }

    private fun String?.matches(other: String?): Boolean {
        val normalized = this?.normalized()
        return normalized != null && normalized == other?.normalized()
    }

    private fun String.normalized(): String? = trim().lowercase().takeIf(String::isNotBlank)

    private fun SongDto.artistKey(): String =
        artistId?.normalized() ?: artistName.normalized().orEmpty()

    private data class RankedSong(val song: SongDto, val score: Double)

    private const val DefaultTargetSize = 30
    private const val MaxSongsPerArtist = 4
}
