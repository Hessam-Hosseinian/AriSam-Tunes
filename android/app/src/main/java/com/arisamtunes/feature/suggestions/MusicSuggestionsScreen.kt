package com.arisamtunes.feature.suggestions

import com.arisamtunes.core.design.spacing.AriSamDimensions
import com.arisamtunes.core.design.colors.AriSamPalette
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playlistName = stringResource(R.string.music_suggestions_playlist_name)
    val playlistDescription = stringResource(R.string.music_suggestions_playlist_description)
    MusicSuggestionsScreen(
        state = state,
        onRetry = viewModel::refresh,
        onCreatePlaylist = { selectedSongIds ->
            viewModel.createPlaylist(
                selectedSongIds = selectedSongIds,
                name = playlistName,
                description = playlistDescription,
                onCreated = onContinue,
            )
        },
    )
}

@Composable
fun MusicSuggestionsScreen(
    state: MusicSuggestionsUiState,
    onRetry: () -> Unit,
    onCreatePlaylist: (Set<String>) -> Unit,
) {
    val selectionState = remember(state.songs) {
        SongSelectionState(state.songs.map(SongDto::id))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainer),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = AriSamDimensions.dp24, vertical = AriSamDimensions.dp18),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp6),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.music_suggestions_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(R.string.music_suggestions_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = AriSamDimensions.dp104, bottom = AriSamDimensions.dp112),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> MusicSuggestionsLoadingShimmer()
                state.hasError -> SuggestionError(onRetry)
                state.songs.isEmpty() -> Text(
                    text = stringResource(R.string.music_suggestions_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = AriSamDimensions.dp32),
                )
                else -> HexagonSongPlane(state.songs, selectionState)
            }
        }

        SuggestionActionBar(
            selectionState = selectionState,
            isCreatingPlaylist = state.isCreatingPlaylist,
            creationFailed = state.playlistCreationFailed,
            onCreatePlaylist = onCreatePlaylist,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun MusicSuggestionsLoadingShimmer() {
    val shimmer = rememberInfiniteTransition(label = "suggestionShimmer")
    val offset = shimmer.animateFloat(
        initialValue = -700f,
        targetValue = 1_600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_350, easing = LinearEasing),
        ),
        label = "suggestionShimmerOffset",
    )
    Box(
        Modifier.fillMaxSize().drawWithCache {
        val radius = HexagonRadius.toPx()
        val halfHeight = radius * cos(PI / HexagonVertices).toFloat()
        val hexagonSize = Size(radius * 2f, halfHeight * 2f)
        val hexagonPath = hexagonSize.createHexagonPath()
        val horizontalStep = hexagonSize.width * .75f
        val columns = ceil(size.width / horizontalStep).toInt() + 1
        val rows = ceil(size.height / hexagonSize.height).toInt() + 1
        val positions = buildList {
            repeat(rows) { row ->
                repeat(columns) { column ->
                    add(
                        Offset(
                            x = column * horizontalStep - radius * .5f,
                            y = row * hexagonSize.height + if (column % 2 == 1) halfHeight else 0f,
                        ),
                    )
                }
            }
        }
        onDrawBehind {
            val brush = Brush.linearGradient(
                colors = listOf(
                    AriSamPalette.white.copy(alpha = .04f),
                    AriSamPalette.sky300.copy(alpha = .22f),
                    AriSamPalette.white.copy(alpha = .04f),
                ),
                start = Offset(offset.value - 260f, offset.value - 260f),
                end = Offset(offset.value + 260f, offset.value + 260f),
            )
            positions.forEach { position ->
                withTransform({ translate(left = position.x, top = position.y) }) {
                    drawPath(path = hexagonPath, brush = brush)
                }
            }
        }
    },
    )
}

@Composable
private fun SuggestionActionBar(
    selectionState: SongSelectionState,
    isCreatingPlaylist: Boolean,
    creationFailed: Boolean,
    onCreatePlaylist: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = selectionState.selectedCount
    Column(
        modifier = modifier.padding(horizontal = AriSamDimensions.dp24, vertical = AriSamDimensions.dp14),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp7),
    ) {
        if (creationFailed) {
            Text(
                text = stringResource(R.string.music_suggestions_playlist_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = { onCreatePlaylist(selectionState.selectedIds()) },
            enabled = selectedCount > 0 && !isCreatingPlaylist,
            modifier = Modifier.fillMaxWidth().height(AriSamDimensions.dp54),
            shape = RoundedCornerShape(AriSamDimensions.dp18),
            colors = ButtonDefaults.buttonColors(),
        ) {
            if (isCreatingPlaylist) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AriSamDimensions.dp22),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = AriSamDimensions.dp2,
                )
                Text(
                    text = stringResource(R.string.music_suggestions_creating_playlist),
                    modifier = Modifier.padding(start = AriSamDimensions.dp10),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = if (selectedCount == 0) {
                        stringResource(R.string.music_suggestions_select_prompt)
                    } else {
                        stringResource(R.string.music_suggestions_create_playlist, selectedCount)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Stable
private class SongSelectionState(songIds: List<String>) {
    private val selections = songIds.distinct().associateWith { mutableStateOf(false) }

    var selectedCount by mutableIntStateOf(0)
        private set

    fun selected(songId: String): State<Boolean> = checkNotNull(selections[songId])

    fun toggle(songId: String) {
        val selection = checkNotNull(selections[songId])
        selection.value = !selection.value
        selectedCount += if (selection.value) 1 else -1
    }

    fun selectedIds(): Set<String> = selections.asSequence()
        .filter { (_, selected) -> selected.value }
        .mapTo(linkedSetOf()) { (songId, _) -> songId }
}

@Composable
private fun HexagonSongPlane(
    songs: List<SongDto>,
    selectionState: SongSelectionState,
) {
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
        val completedSongAnimations = remember(songs) { mutableSetOf<String>() }

        MinaBox(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = AriSamDimensions.dp16, vertical = AriSamDimensions.dp10),
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
                val isSelected by selectionState.selected(song.id)
                HexagonSong(
                    song = song,
                    isSelected = isSelected,
                    hasCompletedAnimation = song.id in completedSongAnimations,
                    onAnimationCompleted = { completedSongAnimations += song.id },
                    onClick = { selectionState.toggle(song.id) },
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
    isSelected: Boolean,
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
    val selectionScale = animateFloatAsState(
        targetValue = if (isSelected) .9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "hexagonSelectionScale",
    )
    val borderColor = animateColorAsState(
        targetValue = if (isSelected) SelectedColor else UnselectedBorderColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "hexagonBorderColor",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(AriSamDimensions.dp3)
            .graphicsLayer {
                scaleX = scale.value * selectionScale.value
                scaleY = scale.value * selectionScale.value
                rotationZ = rotation.value
                this.shape = shape
                clip = true
            }
            .drawWithCache {
                val borderPath = size.createHexagonPath()
                val borderWidth = (if (isSelected) AriSamDimensions.dp5 else AriSamDimensions.dp3).toPx()
                onDrawBehind {
                    drawPath(
                        path = borderPath,
                        color = borderColor.value,
                        style = Stroke(width = borderWidth),
                    )
                }
            }
            .selectable(selected = isSelected, onClick = onClick),
    ) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = song.title,
            error = painterResource(R.drawable.arisam_app_icon_dark),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to AriSamPalette.transparent,
                        .46f to AriSamPalette.suggestionInk.copy(alpha = .12f),
                        1f to AriSamPalette.suggestionInk.copy(alpha = .96f),
                    ),
                ),
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SelectedColor.copy(alpha = .16f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = AriSamDimensions.dp17)
                    .size(AriSamDimensions.dp30)
                    .background(SelectedColor, RoundedCornerShape(AriSamDimensions.dp10)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.music_suggestions_selected),
                    tint = AriSamPalette.white,
                    modifier = Modifier.size(AriSamDimensions.dp19),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(.72f)
                .padding(bottom = AriSamDimensions.dp18),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = song.title,
                color = AriSamPalette.white,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = song.artistName,
                color = AriSamPalette.white.copy(alpha = .72f),
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
        verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp14),
        modifier = Modifier.padding(horizontal = AriSamDimensions.dp32),
    ) {
        Text(
            text = stringResource(R.string.music_suggestions_error),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private val HexagonRadius = AriSamDimensions.dp70
private val SelectedColor = AriSamPalette.sky500
private val UnselectedBorderColor = AriSamPalette.sky300.copy(alpha = .78f)
