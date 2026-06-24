package com.arisamtunes.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.GlassCard
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens

@Composable
fun MiniPlayer(
    onOpen: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val song = state.currentSong ?: return
    PressScaleBox(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.sm, vertical = AriSamThemeTokens.spacing.xs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(AriSamThemeTokens.spacing.sm),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = song.coverImageUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium),
                    )
                    Spacer(Modifier.width(AriSamThemeTokens.spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = viewModel::togglePlayPause) {
                        Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(if (state.isPlaying) R.string.pause else R.string.play))
                    }
                    IconButton(onClick = viewModel::close) { Icon(Icons.Rounded.Close, stringResource(R.string.close_player)) }
                }
                LinearProgressIndicator(
                    progress = { playerProgress(state.progressSeconds, song.durationSeconds) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun NowPlayingRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val song = state.currentSong
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (song == null) {
            EmptyPlayer()
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(AriSamThemeTokens.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.lg),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.extraLarge).background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                    ),
                ) {
                    AsyncImage(song.coverImageUrl, song.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = state.progressSeconds.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toInt()) },
                    valueRange = 0f..song.durationSeconds.coerceAtLeast(1).toFloat(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.lg)) {
                    IconButton(onClick = { }) { Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous_track), modifier = Modifier.size(34.dp)) }
                    IconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(72.dp)) {
                        Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(if (state.isPlaying) R.string.pause else R.string.play), modifier = Modifier.size(48.dp))
                    }
                    IconButton(onClick = { }) { Icon(Icons.Rounded.SkipNext, stringResource(R.string.next_track), modifier = Modifier.size(34.dp)) }
                }
                Text(stringResource(R.string.media3_pending_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyPlayer() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.no_song_playing), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun playerProgress(position: Int, duration: Int): Float = if (duration <= 0) 0f else (position.toFloat() / duration).coerceIn(0f, 1f)
