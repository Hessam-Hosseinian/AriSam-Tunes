package com.arisamtunes.feature.player

import androidx.lifecycle.ViewModel
import com.arisamtunes.data.catalog.SongDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(private val repository: PlayerStateRepository) : ViewModel() {
    val state = repository.state

    fun play(song: SongDto) = repository.play(song)

    fun togglePlayPause() = repository.togglePlayPause()

    fun seekTo(seconds: Int) = repository.seekTo(seconds)

    fun close() = repository.close()
}
