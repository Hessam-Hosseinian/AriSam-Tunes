package com.arisamtunes.domain.library

import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.LocalLibraryRepository
import javax.inject.Inject

class CacheSongsUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(songs: List<SongDto>) = repository.cacheSongs(songs)
}

class ObserveLikedSongsUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    operator fun invoke() = repository.likedSongs()
}

class ToggleLikedSongUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(song: SongDto) = repository.toggleLiked(song)
}

class ObserveDownloadsUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    operator fun invoke() = repository.downloads()
}

class SaveDownloadedSongUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(song: SongDto, localFilePath: String) = repository.saveDownload(song, localFilePath)
}

class DeleteDownloadedSongUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(songId: String) = repository.deleteDownload(songId)
}

class ResolvePlaybackSourceUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(song: SongDto) = repository.playbackSource(song)
}

class ObserveRecentlyPlayedUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    operator fun invoke() = repository.recentlyPlayed()
}

class RecordRecentlyPlayedUseCase @Inject constructor(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(song: SongDto, positionSeconds: Int = 0) = repository.recordPlayed(song, positionSeconds)
}
