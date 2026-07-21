package com.arisamtunes.feature.downloads

import com.arisamtunes.core.design.spacing.AriSamDimensions
import com.arisamtunes.core.design.colors.AriSamPalette
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

private val DownloadAccent = AriSamPalette.amberAccent
private val DownloadOnAccent = AriSamPalette.darkBackground

@Composable
fun DownloadsRoute(
    onSongClick: (SongDto, List<SongDto>) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
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
                val dismissState = rememberSwipeToDismissBoxState()
                var confirmDelete by remember(item.songId) { mutableStateOf(false) }
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        confirmDelete = true
                        dismissState.reset()
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { DownloadDismissBackground(dismissState.dismissDirection) },
                ) {
                    DownloadTrackCard(
                        item = item,
                        onClick = {
                            val sortedQueue = when (state.sortOrder) {
                                DownloadSortOrder.Newest -> completedDownloads.sortedByDescending { it.downloadedAt }
                                DownloadSortOrder.Title -> completedDownloads.sortedBy { it.title.lowercase() }
                                DownloadSortOrder.Artist -> completedDownloads.sortedBy { it.artistName?.lowercase().orEmpty() }
                            }
                            onSongClick(item.toSongDto(), sortedQueue.map(DownloadedSongEntity::toSongDto))
                        },
                        onRetry = { viewModel.onEvent(DownloadsEvent.RetryDownload(item.toSongDto())) },
                    )
                }
                if (confirmDelete) {
                    AlertDialog(
                        onDismissRequest = { confirmDelete = false },
                        icon = { Icon(Icons.Rounded.DeleteOutline, null) },
                        title = { Text(stringResource(R.string.download_delete_title)) },
                        text = { Text(stringResource(R.string.download_delete_description, item.title)) },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmDelete = false
                                viewModel.onEvent(DownloadsEvent.DeleteDownload(item.songId))
                            }) { Text(stringResource(R.string.delete)) }
                        },
                        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
                    )
                }
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            DownloadsHero(
                itemCount = itemCount,
                isPremium = state.isPremium,
                isUpdatingPremium = state.isUpdatingPremium,
                onUpgrade = { onEvent(DownloadsEvent.UpgradeTapped) },
            )
        }
        item { DownloadSortControls(state.sortOrder, onEvent) }
        item { OfflineNotice() }
        state.errorCode?.let { code -> item { DownloadErrorCard(code, state.isUpdatingPremium, onEvent) } }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.downloads_library), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.downloads_count, itemCount), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
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
private fun DownloadSortControls(selected: DownloadSortOrder, onEvent: (DownloadsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8)) {
        Text(stringResource(R.string.download_sort_by), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8)) {
            DownloadSortOrder.entries.forEach { order ->
                FilterChip(
                    selected = selected == order,
                    onClick = { onEvent(DownloadsEvent.SortChanged(order)) },
                    label = {
                        Text(
                            stringResource(
                                when (order) {
                                    DownloadSortOrder.Newest -> R.string.download_sort_newest
                                    DownloadSortOrder.Title -> R.string.download_sort_title
                                    DownloadSortOrder.Artist -> R.string.download_sort_artist
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DownloadDismissBackground(direction: SwipeToDismissBoxValue) {
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    Box(
        Modifier.fillMaxSize().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = AriSamDimensions.dp22),
        contentAlignment = alignment,
    ) {
        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun DownloadsHero(
    itemCount: Int,
    isPremium: Boolean,
    isUpdatingPremium: Boolean,
    onUpgrade: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(AriSamPalette.sky800, AriSamPalette.blueGray, AriSamPalette.darkSurface)))
            .padding(AriSamDimensions.dp22),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10)) {
                Box(
                    modifier = Modifier.size(AriSamDimensions.dp44).clip(MaterialTheme.shapes.medium).background(DownloadAccent),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.CloudDone, null, tint = DownloadOnAccent, modifier = Modifier.size(AriSamDimensions.dp26)) }
                Column {
                    Text(stringResource(R.string.downloads_title), color = AriSamPalette.white, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.downloads_offline_title), color = AriSamPalette.skyTint, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(stringResource(R.string.downloads_offline_hint), color = AriSamPalette.cyanTint, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8)) {
                StatusPill(Icons.Rounded.Download, stringResource(R.string.downloads_count, itemCount), AriSamPalette.white.copy(alpha = .15f))
                StatusPill(
                    Icons.Rounded.WorkspacePremium,
                    stringResource(if (isPremium) R.string.premium_active else R.string.premium_required_title),
                    if (isPremium) AriSamPalette.teal700 else AriSamPalette.white.copy(alpha = .12f),
                )
            }
            if (!isPremium) {
                Button(
                    onClick = onUpgrade,
                    enabled = !isUpdatingPremium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DownloadAccent,
                        contentColor = DownloadOnAccent,
                    ),
                ) {
                    if (isUpdatingPremium) {
                        CircularProgressIndicator(Modifier.size(AriSamDimensions.dp18), strokeWidth = AriSamDimensions.dp2, color = DownloadOnAccent)
                    } else {
                        Icon(Icons.Rounded.WorkspacePremium, null)
                        Spacer(Modifier.width(AriSamDimensions.dp7))
                        Text(stringResource(R.string.upgrade))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier.clip(MaterialTheme.shapes.small).background(color).padding(horizontal = AriSamDimensions.dp10, vertical = AriSamDimensions.dp7),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp5),
    ) {
        Icon(icon, null, tint = AriSamPalette.white, modifier = Modifier.size(AriSamDimensions.dp15))
        Text(label, color = AriSamPalette.white, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun OfflineNotice() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer).border(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium).padding(AriSamDimensions.dp14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = DownloadAccent)
        Text(stringResource(R.string.downloads_ready_offline), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DownloadTrackCard(item: DownloadedSongEntity, onClick: () -> Unit, onRetry: () -> Unit) {
    val isReady = item.downloadState == LocalLibraryRepository.DownloadStateCompleted && File(item.localFilePath).isFile
    val isQueued = item.downloadState == LocalLibraryRepository.DownloadStateQueued
    val isRunning = item.downloadState == LocalLibraryRepository.DownloadStateRunning
    val isFailed = item.downloadState == LocalLibraryRepository.DownloadStateFailed ||
        (item.downloadState == LocalLibraryRepository.DownloadStateCompleted && !isReady)
    val statusText = when {
        isReady -> stringResource(R.string.downloads_ready_offline)
        isQueued -> stringResource(R.string.download_status_queued)
        isRunning -> stringResource(R.string.download_status_progress, item.downloadProgress.coerceIn(0, 99))
        else -> stringResource(R.string.downloads_not_ready)
    }
    PressScaleBox(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = isReady) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(AriSamDimensions.dp1, if (isReady) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error.copy(alpha = .55f), MaterialTheme.shapes.large)
                .padding(AriSamDimensions.dp12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12),
        ) {
            AsyncImage(
                model = item.coverImageUrl,
                contentDescription = item.title,
                error = painterResource(R.drawable.arisam_app_icon_dark),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AriSamDimensions.dp58).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp3)) {
                Text(item.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(item.artistName, item.album).joinToString(" • "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    statusText,
                    color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (isRunning) {
                    LinearProgressIndicator(
                        progress = { item.downloadProgress.coerceIn(0, 99) / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = AriSamDimensions.dp3),
                        color = DownloadAccent,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
            }
            Box(
                modifier = Modifier.size(AriSamDimensions.dp40).clip(MaterialTheme.shapes.medium).background(if (isReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isReady -> Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    isQueued || isRunning -> CircularProgressIndicator(
                        modifier = Modifier.size(AriSamDimensions.dp22),
                        strokeWidth = AriSamDimensions.dp2,
                        color = DownloadAccent,
                    )
                    isFailed -> IconButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, stringResource(R.string.retry), tint = MaterialTheme.colorScheme.error)
                    }
                    else -> Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DownloadErrorCard(code: String, isUpdatingPremium: Boolean, onEvent: (DownloadsEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.errorContainer).padding(AriSamDimensions.dp14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    if (code == DownloadsViewModel.PremiumRequired) R.string.premium_required_title
                    else R.string.download_action_failed,
                ),
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    if (code == DownloadsViewModel.PremiumRequired) R.string.downloads_premium_description
                    else R.string.download_action_failed_description,
                ),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (code == DownloadsViewModel.PremiumRequired) {
            TextButton(
                onClick = { onEvent(DownloadsEvent.UpgradeTapped) },
                enabled = !isUpdatingPremium,
            ) {
                if (isUpdatingPremium) {
                    CircularProgressIndicator(Modifier.size(AriSamDimensions.dp18), strokeWidth = AriSamDimensions.dp2)
                } else {
                    Text(stringResource(R.string.upgrade))
                }
            }
        }
        TextButton(onClick = { onEvent(DownloadsEvent.DismissError) }) { Text(stringResource(R.string.dismiss)) }
    }
}

@Composable
private fun EmptyDownloads(onEvent: (DownloadsEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = AriSamDimensions.dp46, horizontal = AriSamDimensions.dp28),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12),
    ) {
        Box(modifier = Modifier.size(AriSamDimensions.dp68).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(AriSamDimensions.dp34))
        }
        Text(stringResource(R.string.downloads_empty), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.downloads_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { onEvent(DownloadsEvent.DownloadTapped) }) { Text(stringResource(R.string.try_download)) }
    }
}

@Composable
private fun DownloadsLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10)) {
        repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(AriSamDimensions.dp82)) }
    }
}

@Composable
private fun DownloadsError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = AriSamDimensions.dp40),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
    ) {
        Icon(Icons.Rounded.Refresh, null, tint = DownloadAccent, modifier = Modifier.size(AriSamDimensions.dp32))
        Text(stringResource(R.string.downloads_load_error), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Preview(name = "Downloads — Empty", showBackground = true)
@Composable
private fun DownloadsEmptyPreview() {
    AriSamTheme(darkTheme = true) {
        DownloadsScreen(DownloadsUiState(isPremium = true), 0, false, false, {}, {}, content = {})
    }
}
