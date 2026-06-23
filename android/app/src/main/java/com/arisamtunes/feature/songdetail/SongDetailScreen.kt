package com.arisamtunes.feature.songdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.arisamtunes.data.catalog.SongDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

@Composable
fun SongDetailRoute(
    onBack: () -> Unit,
    viewModel: SongDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SongDetailScreen(state, onBack, viewModel::refresh)
}

@Composable
private fun SongDetailScreen(
    state: SongDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.song_detail_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        when {
            state.isLoading -> Loading()
            state.hasError -> ErrorState(onRetry)
            state.song == null -> EmptyState()
            else -> SongMetadata(state.song)
        }
    }
}

@Composable
private fun SongMetadata(song: SongDto) {
    val spacing = AriSamThemeTokens.spacing
    val facts = song.metadataFacts()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SongHero(song) }
        if (song.tags.isNotEmpty() || song.isExplicit || song.isLocal || song.isDemo) {
            item { SongChips(song) }
        }
        item {
            MetadataSection(
                title = R.string.metadata_core,
                rows = facts.take(8),
            )
        }
        item {
            MetadataSection(
                title = R.string.metadata_audio,
                rows = facts.drop(8).take(8),
            )
        }
        item {
            MetadataSection(
                title = R.string.metadata_catalog,
                rows = facts.drop(16),
            )
        }
        val extraRows = song.extraMetadata.metadataRows()
        if (extraRows.isNotEmpty()) {
            item { MetadataSection(R.string.metadata_discovered_extra, extraRows) }
        }
        song.lyrics?.takeIf(String::isNotBlank)?.let { lyrics ->
            item {
                MetadataSection(
                    title = R.string.metadata_lyrics,
                    rows = listOf(MetadataRow(R.string.metadata_lyrics, lyrics)),
                )
            }
        }
    }
}

@Composable
private fun SongHero(song: SongDto) {
    val spacing = AriSamThemeTokens.spacing
    Box(Modifier.fillMaxWidth().height(320.dp).padding(horizontal = spacing.lg)) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge),
        )
        Box(
            Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge).background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .9f))),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(song.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(song.artistName, color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.titleMedium)
            song.album?.takeIf(String::isNotBlank)?.let {
                Text(it, color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        PressScaleBox(
            onClick = { },
            modifier = Modifier.align(Alignment.BottomEnd).padding(spacing.lg).size(54.dp).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primary),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun SongChips(song: SongDto) {
    val spacing = AriSamThemeTokens.spacing
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (song.isExplicit) AssistChip(onClick = { }, label = { Text(stringResource(R.string.metadata_explicit)) }, leadingIcon = { Icon(Icons.Rounded.Explicit, null) })
        if (song.isLocal) AssistChip(onClick = { }, label = { Text(stringResource(R.string.metadata_local)) })
        if (song.isDemo) AssistChip(onClick = { }, label = { Text(stringResource(R.string.metadata_demo)) })
        song.tags.take(2).forEach { tag -> AssistChip(onClick = { }, label = { Text(tag, maxLines = 1) }) }
    }
}

@Composable
private fun MetadataSection(@StringRes title: Int, rows: List<MetadataRow>) {
    val visibleRows = rows.filter { it.value.isNotBlank() }
    if (visibleRows.isEmpty()) return
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        contentPadding = PaddingValues(AriSamThemeTokens.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm)) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            visibleRows.forEach { row -> MetadataLine(row) }
        }
    }
}

@Composable
private fun MetadataLine(row: MetadataRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(row.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(row.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorState(onRetry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    TextButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry)) }
}

@Composable
private fun EmptyState() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.song_detail_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class MetadataRow(@param:StringRes val label: Int, val value: String)

private fun SongDto.metadataFacts() = listOfNotNull(
    text(R.string.metadata_title, title),
    text(R.string.metadata_artist, artistName),
    text(R.string.metadata_album, album),
    text(R.string.metadata_album_artist, albumArtist),
    text(R.string.metadata_genre, genre),
    text(R.string.metadata_composer, composer),
    text(R.string.metadata_producer, producer),
    text(R.string.metadata_mood, mood),
    durationSeconds.takeIf { it > 0 }?.let { MetadataRow(R.string.metadata_duration, formatDuration(it)) },
    bitrateKbps?.let { MetadataRow(R.string.metadata_bitrate, "$it kbps") },
    sampleRateHz?.let { MetadataRow(R.string.metadata_sample_rate, "$it Hz") },
    text(R.string.metadata_channels, channels),
    text(R.string.metadata_codec, codec),
    text(R.string.metadata_file_format, fileFormat),
    audioFileSize?.takeIf { it > 0L }?.let { MetadataRow(R.string.metadata_file_size, formatBytes(it)) },
    text(R.string.metadata_source_file, sourceFileName),
    trackNumber?.let { MetadataRow(R.string.metadata_track_number, it.toString()) },
    discNumber?.let { MetadataRow(R.string.metadata_disc_number, it.toString()) },
    releaseYear?.let { MetadataRow(R.string.metadata_release_year, it.toString()) },
    text(R.string.metadata_release_date, releaseDate),
    text(R.string.metadata_language, language),
    MetadataRow(R.string.metadata_popularity, popularity.toString()),
    MetadataRow(R.string.metadata_play_count, playCount.toString()),
    text(R.string.metadata_created_at, createdAt),
    text(R.string.metadata_updated_at, updatedAt),
    text(R.string.metadata_audio_url, audioUrl),
    text(R.string.metadata_cover_url, coverImageUrl),
)

private fun text(@StringRes label: Int, value: String?) = value?.takeIf { it.isNotBlank() }?.let { MetadataRow(label, it) }

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return "%.2f MB".format(mb)
}

private fun JsonObject.metadataRows(): List<MetadataRow> = entries.mapNotNull { (key, value) ->
    val rendered = when (value) {
        JsonNull -> null
        is JsonPrimitive -> value.contentOrNull ?: value.booleanOrNull?.toString() ?: value.longOrNull?.toString() ?: value.doubleOrNull?.toString()
        is JsonArray -> value.joinToString(", ") { it.toString().trim('"') }
        is JsonObject -> value.toString()
    }?.takeIf { it.isNotBlank() && it != "{}" && it != "[]" }
    rendered?.let { MetadataRow(R.string.metadata_extra_item, "$key: $it") }
}
