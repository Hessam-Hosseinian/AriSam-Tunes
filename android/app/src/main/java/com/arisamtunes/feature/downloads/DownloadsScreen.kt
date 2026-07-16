package com.arisamtunes.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import com.arisamtunes.data.local.toSongDto
import java.io.File

private val DownloadPageBackground = Color(0xFF0C1821)
private val DownloadCardBackground = Color(0xFF172536)
private val DownloadAccent = Color(0xFFFFC857)
private val DownloadSecondary = Color(0xFFBAE6FD)

@Composable
fun DownloadsRoute(
    onSongClick: (SongDto) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val downloads = viewModel.downloads.collectAsLazyPagingItems()
    DownloadsScreen(
        state = state,
        itemCount = downloads.itemCount,
        isLoading = downloads.loadState.refresh is LoadState.Loading,
        hasError = downloads.loadState.refresh is LoadState.Error,
        onRetry = downloads::retry,
        onEvent = viewModel::onEvent,
    ) {
        items(downloads.itemCount, key = { index -> downloads.peek(index)?.songId ?: index }) { index ->
            downloads[index]?.let { item ->
                DownloadTrackCard(
                    item = item,
                    onClick = { onSongClick(item.toSongDto()) },
                )
            }
        }
    }
}

@Composable
private fun DownloadsScreen(
    state: DownloadsUiState,
    itemCount: Int,
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    onEvent: (DownloadsEvent) -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DownloadPageBackground),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item { DownloadsHero(itemCount, state.isPremium) }
        item { OfflineNotice() }
        state.errorCode?.let { code -> item { DownloadErrorCard(code, onEvent) } }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.downloads_library), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.downloads_count, itemCount), color = DownloadSecondary, style = MaterialTheme.typography.labelLarge)
            }
        }
        when {
            isLoading -> item { DownloadsLoading() }
            hasError -> item { DownloadsError(onRetry) }
            itemCount == 0 -> item { EmptyDownloads(onEvent) }
            else -> content()
        }
    }
}

@Composable
private fun DownloadsHero(itemCount: Int, isPremium: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(Color(0xFF075985), Color(0xFF123246), Color(0xFF1E2A3A))))
            .padding(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.medium).background(DownloadAccent),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.CloudDone, null, tint = DownloadPageBackground, modifier = Modifier.size(26.dp)) }
                Column {
                    Text(stringResource(R.string.downloads_title), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.downloads_offline_title), color = Color(0xFFD9F2FF), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(stringResource(R.string.downloads_offline_hint), color = Color(0xFFB9E8FF), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(Icons.Rounded.Download, stringResource(R.string.downloads_count, itemCount), Color.White.copy(alpha = .15f))
                StatusPill(
                    Icons.Rounded.WorkspacePremium,
                    stringResource(if (isPremium) R.string.premium_active else R.string.premium_required_title),
                    if (isPremium) Color(0xFF2A6F5A) else Color.White.copy(alpha = .12f),
                )
            }
        }
    }
}

@Composable
private fun StatusPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier.clip(MaterialTheme.shapes.small).background(color).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun OfflineNotice() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Color(0xFF102532)).border(1.dp, Color(0xFF2D6682), MaterialTheme.shapes.medium).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = DownloadAccent)
        Text(stringResource(R.string.downloads_ready_offline), color = DownloadSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DownloadTrackCard(item: DownloadedSongEntity, onClick: () -> Unit) {
    val isReady = item.downloadState == LocalLibraryRepository.DownloadStateCompleted && File(item.localFilePath).isFile
    PressScaleBox(onClick = { if (isReady) onClick() }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(DownloadCardBackground)
                .border(1.dp, if (isReady) Color(0xFF28445A) else Color(0xFF6E3A44), MaterialTheme.shapes.large)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = item.coverImageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(58.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFF264052)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(item.artistName, item.album).joinToString(" • "), color = DownloadSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(if (isReady) R.string.downloads_ready_offline else R.string.downloads_not_ready),
                    color = if (isReady) DownloadAccent else Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Box(
                modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.medium).background(if (isReady) DownloadAccent.copy(alpha = .16f) else Color(0xFFFFB4AB).copy(alpha = .12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (isReady) Icons.Rounded.PlayArrow else Icons.Rounded.ErrorOutline, null, tint = if (isReady) DownloadAccent else Color(0xFFFFB4AB))
            }
        }
    }
}

@Composable
private fun DownloadErrorCard(code: String, onEvent: (DownloadsEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Color(0xFF3D252B)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = Color(0xFFFFB4AB))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.premium_required_title), color = Color.White, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.error_code_format, code), color = Color(0xFFFFDAD6), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { onEvent(DownloadsEvent.DismissError) }) { Text(stringResource(R.string.dismiss)) }
    }
}

@Composable
private fun EmptyDownloads(onEvent: (DownloadsEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(DownloadCardBackground).padding(vertical = 46.dp, horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(68.dp).clip(MaterialTheme.shapes.large).background(Color(0xFF123246)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Download, null, tint = DownloadAccent, modifier = Modifier.size(34.dp))
        }
        Text(stringResource(R.string.downloads_empty), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.downloads_empty_hint), color = DownloadSecondary, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { onEvent(DownloadsEvent.DownloadTapped) }) { Text(stringResource(R.string.try_download)) }
    }
}

@Composable
private fun DownloadsLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(82.dp)) }
    }
}

@Composable
private fun DownloadsError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Refresh, null, tint = DownloadAccent, modifier = Modifier.size(32.dp))
        Text(stringResource(R.string.downloads_load_error), color = DownloadSecondary)
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = DownloadAccent, contentColor = DownloadPageBackground)) { Text(stringResource(R.string.retry)) }
    }
}

@Preview(name = "Downloads — Empty", showBackground = true)
@Composable
private fun DownloadsEmptyPreview() {
    AriSamTheme(darkTheme = true) {
        DownloadsScreen(DownloadsUiState(isPremium = true), 0, false, false, {}, {}, content = {})
    }
}
