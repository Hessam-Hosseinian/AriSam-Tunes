package com.arisamtunes.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arisamtunes.R
import com.arisamtunes.core.design.components.GlassCard
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.design.theme.AriSamThemeTokens

@Composable
fun DownloadsRoute(viewModel: DownloadsViewModel = hiltViewModel()) {
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
                ListItem(
                    headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("${item.artistName} • ${item.downloadState}", maxLines = 1) },
                    leadingContent = { Icon(Icons.Rounded.Download, null) },
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item {
            Text(stringResource(R.string.downloads_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item { PremiumCard(state.isPremium, onEvent) }
        state.errorCode?.let { code ->
            item { PremiumRequiredCard(code, onEvent) }
        }
        when {
            isLoading -> item { LoadingState() }
            hasError -> item { ErrorState(onRetry) }
            itemCount == 0 -> item { EmptyDownloads(onEvent) }
            else -> content()
        }
    }
}

@Composable
private fun PremiumCard(isPremium: Boolean, onEvent: (DownloadsEvent) -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(if (isPremium) R.string.premium_active else R.string.premium_required_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(if (isPremium) R.string.downloads_premium_ready else R.string.downloads_premium_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isPremium) Button(onClick = { onEvent(DownloadsEvent.UpgradeTapped) }) { Text(stringResource(R.string.upgrade)) }
        }
    }
}

@Composable
private fun PremiumRequiredCard(code: String, onEvent: (DownloadsEvent) -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.premium_required_title), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.error_code_format, code), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { onEvent(DownloadsEvent.DismissError) }) { Text(stringResource(R.string.dismiss)) }
        }
    }
}

@Composable
private fun EmptyDownloads(onEvent: (DownloadsEvent) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
            Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Text(stringResource(R.string.downloads_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { onEvent(DownloadsEvent.DownloadTapped) }) { Text(stringResource(R.string.try_download)) }
        }
    }
}

@Composable
private fun LoadingState() = Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorState(onRetry: () -> Unit) = Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
    TextButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry)) }
}

@Preview(name = "Downloads - Premium", showBackground = true)
@Composable
private fun DownloadsPremiumPreview() {
    AriSamTheme {
        DownloadsScreen(
            state = DownloadsUiState(isPremium = true),
            itemCount = 3,
            isLoading = false,
            hasError = false,
            onRetry = {},
            onEvent = {},
        ) {
            items(3) { index ->
                ListItem(
                    headlineContent = { Text("Downloaded track ${index + 1}") },
                    supportingContent = { Text("Ari Sam • READY") },
                    leadingContent = { Icon(Icons.Rounded.Download, null) },
                )
            }
        }
    }
}

@Preview(name = "Downloads - Upgrade", showBackground = true)
@Composable
private fun DownloadsUpgradePreview() {
    AriSamTheme {
        DownloadsScreen(
            state = DownloadsUiState(errorCode = DownloadsViewModel.PremiumRequired),
            itemCount = 0,
            isLoading = false,
            hasError = false,
            onRetry = {},
            onEvent = {},
            content = {},
        )
    }
}
