package com.arisamtunes.feature.suggestions

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.data.catalog.SongDto
import eu.wewox.minabox.MinaBox
import eu.wewox.minabox.MinaBoxItem
import eu.wewox.minabox.MinaBoxState
import kotlin.math.ceil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun MusicSuggestionsRoute(
    onContinue: () -> Unit,
    viewModel: MusicSuggestionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    MusicSuggestionsScreen(
        state = state,
        onRetry = viewModel::refresh,
        onContinue = onContinue,
    )
}

@Composable
fun MusicSuggestionsScreen(
    state: MusicSuggestionsUiState,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF06141D), Color(0xFF0A2635), Color(0xFF071922)),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF7DD3FC),
                )
                Text(
                    text = stringResource(R.string.music_suggestions_title),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(R.string.music_suggestions_subtitle),
                color = Color.White.copy(alpha = .68f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 104.dp, bottom = 88.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(color = Color(0xFF7DD3FC))
                state.hasError -> SuggestionError(onRetry)
                state.songs.isEmpty() -> Text(
                    text = stringResource(R.string.music_suggestions_empty),
                    color = Color.White.copy(alpha = .72f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
                else -> HexagonSongPlane(state.songs)
            }
        }

        Button(
            onClick = onContinue,
            enabled = !state.isLoading,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = stringResource(R.string.music_suggestions_continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HexagonSongPlane(songs: List<SongDto>) {
    val halfHeight = HexagonRadius * cos(PI / HexagonVertices).toFloat()
    val itemSize = with(LocalDensity.current) {
        Size(
            width = HexagonRadius.toPx() * 2f,
            height = halfHeight.toPx() * 2f,
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()
        val horizontalStep = itemSize.width * .75f
        val minimumColumns = ceil(viewportWidth / horizontalStep).toInt() + WrapBufferCells
        val columns = minimumColumns.coerceAtLeast(MinimumHexagonColumns).toEven()
        val minimumRows = ceil(viewportHeight / itemSize.height).toInt() + WrapBufferCells
        val rows = maxOf(
            minimumRows,
            ceil(songs.size.toFloat() / columns).toInt(),
        )
        val cellsPerTile = columns * rows
        val tileWidth = columns * horizontalStep
        val tileHeight = rows * itemSize.height
        val state = remember(songs.size, tileWidth, tileHeight) {
            MinaBoxState {
                Offset(tileWidth * CenterTileIndex, tileHeight * CenterTileIndex)
            }.apply {
                configureWrapping(
                    horizontalPeriod = tileWidth,
                    verticalPeriod = tileHeight,
                    centerTileIndex = CenterTileIndex,
                )
            }
        }
        val scope = rememberCoroutineScope()
        val completedSongAnimations = remember(songs) { mutableSetOf<String>() }

        MinaBox(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            items(
                count = cellsPerTile * WrapTileCount * WrapTileCount,
                key = { index ->
                    val cell = index % cellsPerTile
                    val copy = index / cellsPerTile
                    "$copy:$cell:${songs[cell % songs.size].id}"
                },
                layoutInfo = { index ->
                    val cell = index % cellsPerTile
                    val copy = index / cellsPerTile
                    val tileColumn = copy % WrapTileCount
                    val tileRow = copy / WrapTileCount
                    val column = cell % columns
                    val row = cell / columns
                    MinaBoxItem(
                        x = tileColumn * tileWidth + column * horizontalStep,
                        y = tileRow * tileHeight +
                            (if (column % 2 == 1) itemSize.height * .5f else 0f) +
                            row * itemSize.height,
                        width = itemSize.width,
                        height = itemSize.height,
                    )
                },
            ) { index ->
                val song = songs[(index % cellsPerTile) % songs.size]
                HexagonSong(
                    song = song,
                    hasCompletedAnimation = song.id in completedSongAnimations,
                    onAnimationCompleted = { completedSongAnimations += song.id },
                    onClick = { scope.launch { state.animateTo(index) } },
                )
            }
        }
    }
}

/**
 * Adapted from MinaBoxAdvancedScreen in oleksandrbalan/minabox; modified for AriSam Tunes song art.
 * The upstream Apache-2.0 license is included at third_party/minabox/LICENSE.md.
 */
@Composable
private fun HexagonSong(
    song: SongDto,
    hasCompletedAnimation: Boolean,
    onAnimationCompleted: () -> Unit,
    onClick: () -> Unit,
) {
    val rotation = remember { Animatable(if (hasCompletedAnimation) 0f else -15f) }
    val scale = remember { Animatable(if (hasCompletedAnimation) 1f else .5f) }
    LaunchedEffect(Unit) {
        if (!hasCompletedAnimation) {
            delay(100)
            coroutineScope {
                launch { scale.animateTo(1f) }
                launch { rotation.animateTo(0f) }
            }
            onAnimationCompleted()
        }
    }
    val shape = remember { GenericShape { size, _ -> addPath(size.createHexagonPath()) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation.value
                this.shape = shape
                clip = true
            }
            .drawWithCache {
                val borderPath = size.createHexagonPath()
                val borderColor = Color(0xFF7DD3FC).copy(alpha = .78f)
                val borderWidth = 3.dp.toPx()
                onDrawBehind {
                    drawPath(
                        path = borderPath,
                        color = borderColor,
                        style = Stroke(width = borderWidth),
                    )
                }
            }
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        .46f to Color(0xFF06141D).copy(alpha = .12f),
                        1f to Color(0xFF06141D).copy(alpha = .96f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(.72f)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = song.title,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = song.artistName,
                color = Color.White.copy(alpha = .72f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun Int.toEven(): Int = if (this % 2 == 0) this else this + 1

@Composable
private fun SuggestionError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.music_suggestions_error),
            color = Color.White.copy(alpha = .72f),
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

private fun Size.createHexagonPath(): Path = Path().apply {
    val radius = width / 2f

    fun point(angle: Double) {
        lineTo(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat(),
        )
    }

    moveTo(0f, center.y)
    point(-2f * PI / 3f)
    point(-1f * PI / 3f)
    lineTo(width, center.y)
    point(1f * PI / 3f)
    point(2f * PI / 3f)
    close()
}

private const val MinimumHexagonColumns = 4
private const val HexagonVertices = 6
private const val WrapBufferCells = 2
private const val WrapTileCount = 3
private const val CenterTileIndex = WrapTileCount / 2
private val HexagonRadius = 70.dp
