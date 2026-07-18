package com.arisamtunes.feature.player

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arisamtunes.R
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val AudioOutputLeadMillis = 85L
private const val NoteTravelTimeMillis = 1_100L
private const val EventEarlyWindowMillis = 90L
private const val EventLateWindowMillis = 285L
private const val PadYFraction = .56f

private val PulseColors = listOf(
    Color(0xFF24F2C9),
    Color(0xFFFF3E9D),
    Color(0xFFFFE24B),
)

private val PadXFractions = floatArrayOf(.18f, .50f, .82f)

private data class PulseEvent(
    val id: Long,
    val lane: Int,
    val targetMillis: Long,
    val strength: Float,
)

private data class SpectrumFrame(
    val targetMillis: Long,
    val levels: List<Float>,
)

private data class PulseBurst(
    val id: Long,
    val lane: Int,
    val startedAtMillis: Long,
    val color: Color,
)

@Composable
fun RhythmGameRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val latestState by rememberUpdatedState(state)
    val song = state.crossfadeSong ?: state.currentSong
    if (song == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    BackHandler(onBack = onBack)
    val haptics = LocalHapticFeedback.current
    val events = remember(song.id) { mutableStateListOf<PulseEvent>() }
    val bursts = remember(song.id) { mutableStateListOf<PulseBurst>() }
    val delayedSpectrum = remember(song.id) { ArrayDeque<SpectrumFrame>() }
    var visualLevels by remember(song.id) { mutableStateOf(listOf(.06f, .06f, .06f)) }
    var score by remember(song.id) { mutableIntStateOf(0) }
    var combo by remember(song.id) { mutableIntStateOf(0) }
    var bestCombo by remember(song.id) { mutableIntStateOf(0) }
    var judgedCount by remember(song.id) { mutableIntStateOf(0) }
    var accuracyTotal by remember(song.id) { mutableFloatStateOf(0f) }
    var lastGrade by remember(song.id) { mutableStateOf<RhythmGrade?>(null) }
    var gradeShownAt by remember(song.id) { mutableLongStateOf(0L) }
    var renderMillis by remember(song.id) { mutableLongStateOf(state.progressMillis) }
    var anchorProgress by remember(song.id) { mutableLongStateOf(state.progressMillis) }
    var anchorRealtime by remember(song.id) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var beatMap by remember(song.id) { mutableStateOf<List<RhythmBeatNote>?>(null) }
    val latestBeatMap by rememberUpdatedState(beatMap)
    var nextBeatMapIndex by remember(song.id) { mutableIntStateOf(0) }
    var lastRenderedPosition by remember(song.id) { mutableLongStateOf(state.progressMillis) }

    LaunchedEffect(song.id) {
        val spectrum = viewModel.songSpectrum(song.id)
        beatMap = spectrum?.let { buildRhythmBeatMap(it.frames, it.frameDurationMs) }.orEmpty()
    }

    LaunchedEffect(beatMap) {
        val map = beatMap ?: return@LaunchedEffect
        events.clear()
        nextBeatMapIndex = map.indexOfFirst { it.targetMillis >= renderMillis + NoteTravelTimeMillis }
            .takeIf { it >= 0 }
            ?: map.size
        lastRenderedPosition = renderMillis
    }

    LaunchedEffect(state.progressMillis, state.isPlaying) {
        anchorProgress = state.progressMillis
        anchorRealtime = SystemClock.elapsedRealtime()
    }

    // Events and their visual energy share the exact same delayed audio timestamp.
    LaunchedEffect(state.visualizerBands, song.id) {
        if (!state.isPlaying) return@LaunchedEffect
        val levels = state.visualizerBands.toLaneLevels()
        val signalTime = renderMillis
        val outputTime = signalTime + AudioOutputLeadMillis
        delayedSpectrum.addLast(SpectrumFrame(outputTime, levels))
        while (delayedSpectrum.size > 16) delayedSpectrum.removeFirst()
    }

    LaunchedEffect(song.id) {
        while (true) {
            withFrameMillis {
                val playbackState = latestState
                val realtime = SystemClock.elapsedRealtime()
                renderMillis = anchorProgress + if (playbackState.isPlaying) realtime - anchorRealtime else 0L

                while (delayedSpectrum.isNotEmpty() && delayedSpectrum.first.targetMillis <= renderMillis) {
                    visualLevels = delayedSpectrum.removeFirst().levels
                }
                if (playbackState.isPlaying) {
                    val map = latestBeatMap
                    if (map != null) {
                        if (abs(renderMillis - lastRenderedPosition) > 650L) {
                            events.clear()
                            nextBeatMapIndex = map.indexOfFirst {
                                it.targetMillis >= renderMillis + NoteTravelTimeMillis
                            }.takeIf { it >= 0 } ?: map.size
                        }
                        while (
                            nextBeatMapIndex < map.size &&
                            map[nextBeatMapIndex].targetMillis - renderMillis <= NoteTravelTimeMillis
                        ) {
                            val note = map[nextBeatMapIndex]
                            if (note.targetMillis >= renderMillis - EventLateWindowMillis) {
                                events += PulseEvent(
                                    id = nextBeatMapIndex.toLong(),
                                    lane = note.lane,
                                    targetMillis = note.targetMillis,
                                    strength = note.strength,
                                )
                            }
                            nextBeatMapIndex += 1
                        }
                    }
                    val missed = events.filter { renderMillis - it.targetMillis > EventLateWindowMillis }
                    if (missed.isNotEmpty()) {
                        events.removeAll(missed.toSet())
                        combo = 0
                        judgedCount += missed.size
                        lastGrade = RhythmGrade.Miss
                        gradeShownAt = renderMillis
                    }
                }
                lastRenderedPosition = renderMillis
                bursts.removeAll { renderMillis - it.startedAtMillis > 620L }
            }
        }
    }

    val accuracy = if (judgedCount == 0) 100 else ((accuracyTotal / judgedCount) * 100).toInt().coerceIn(0, 100)
    val gradeVisible = lastGrade != null && renderMillis - gradeShownAt < 620L
    val gameDescription = stringResource(R.string.rhythm_game_hint)

    Box(Modifier.fillMaxSize().background(Color(0xFF070711))) {
        PulsePlayField(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = gameDescription }
                .pointerInput(song.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val tap = down.position
                        val padRadius = 86.dp.toPx()
                        val lane = PadXFractions.indices.minBy { laneIndex ->
                            val dx = tap.x - PadXFractions[laneIndex] * size.width
                            val dy = tap.y - PadYFraction * size.height
                            dx * dx + dy * dy
                        }
                        val insidePad = run {
                            val dx = tap.x - PadXFractions[lane] * size.width
                            val dy = tap.y - PadYFraction * size.height
                            sqrt(dx * dx + dy * dy) <= padRadius
                        }
                        val now = renderMillis
                        val event = if (insidePad) {
                            events
                                .filter {
                                    it.lane == lane &&
                                        now - it.targetMillis in -EventEarlyWindowMillis..EventLateWindowMillis
                                }
                                .minByOrNull { abs(now - it.targetMillis) }
                        } else null

                        if (event != null) {
                            val grade = judgePulseTiming(now - event.targetMillis)
                            events.remove(event)
                            score += grade.points * (1 + combo / 12)
                            combo += 1
                            bestCombo = max(bestCombo, combo)
                            judgedCount += 1
                            accuracyTotal += grade.accuracyWeight
                            lastGrade = grade
                            gradeShownAt = now
                            bursts += PulseBurst(event.id, event.lane, now, PulseColors[event.lane])
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (insidePad) {
                            combo = 0
                            lastGrade = RhythmGrade.Miss
                            gradeShownAt = now
                        }
                        down.consume()
                    }
                },
            levels = visualLevels,
            events = events,
            bursts = bursts,
            renderMillis = renderMillis,
            isPlaying = state.isPlaying,
        )

        PadLabels(Modifier.align(Alignment.Center).offset(y = 51.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    RoundGameButton(onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.rhythm_game_title),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            fontSize = 15.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(PulseColors[0]))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                stringResource(R.string.rhythm_game_live),
                                color = PulseColors[0].copy(alpha = .78f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 9.sp,
                            )
                        }
                    }
                    RoundGameButton(viewModel::togglePlayPause) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                            tint = Color.White,
                        )
                    }
                }
                Text(
                    song.title,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = .58f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameStat(stringResource(R.string.rhythm_game_score), score.toString().padStart(6, '0'), PulseColors[0], Modifier.weight(1.35f))
                    GameStat(stringResource(R.string.rhythm_game_combo), "${combo}×", PulseColors[1], Modifier.weight(1f))
                    GameStat(stringResource(R.string.rhythm_game_accuracy), "$accuracy%", PulseColors[2], Modifier.weight(1f))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(
                    visible = gradeVisible,
                    enter = fadeIn() + scaleIn(initialScale = .72f),
                    exit = fadeOut() + scaleOut(targetScale = 1.18f),
                ) {
                    val grade = lastGrade ?: RhythmGrade.Miss
                    Text(
                        stringResource(grade.labelResource()),
                        color = grade.color(),
                        fontSize = if (grade == RhythmGrade.Perfect) 26.sp else 21.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Surface(
                    color = Color(0xFF11111F).copy(alpha = .88f),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Waves, null, tint = PulseColors[1], modifier = Modifier.size(16.dp))
                        Text(
                            when {
                                !state.isPlaying -> stringResource(R.string.rhythm_game_paused)
                                beatMap == null -> stringResource(R.string.rhythm_game_mapping)
                                beatMap.isNullOrEmpty() -> stringResource(R.string.rhythm_game_syncing)
                                else -> stringResource(R.string.rhythm_game_ready)
                            },
                            color = Color.White.copy(alpha = .7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (bestCombo >= 10) {
                            Text("• ${stringResource(R.string.rhythm_game_best, bestCombo)}", color = PulseColors[1], fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !state.isPlaying,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Surface(
                onClick = viewModel::togglePlayPause,
                color = Color(0xFF11111F).copy(alpha = .95f),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PulseColors[0].copy(alpha = .34f)),
            ) {
                Column(
                    Modifier.padding(horizontal = 34.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = PulseColors[0], modifier = Modifier.size(36.dp))
                    Text(stringResource(R.string.rhythm_game_resume), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PulsePlayField(
    modifier: Modifier,
    levels: List<Float>,
    events: List<PulseEvent>,
    bursts: List<PulseBurst>,
    renderMillis: Long,
    isPlaying: Boolean,
) {
    Canvas(modifier) {
        drawRect(
            Brush.radialGradient(
                listOf(PulseColors[1].copy(alpha = .10f + levels[1] * .08f), Color(0xFF070711)),
                center = Offset(size.width * .5f, size.height * .52f),
                radius = size.maxDimension * .74f,
            ),
        )

        val gridStep = 42.dp.toPx()
        var y = gridStep * .5f
        while (y < size.height) {
            var x = gridStep * .5f
            while (x < size.width) {
                drawCircle(Color.White.copy(alpha = .025f), 1.1.dp.toPx(), Offset(x, y))
                x += gridStep
            }
            y += gridStep
        }

        PadXFractions.indices.forEach { lane ->
            val center = Offset(size.width * PadXFractions[lane], size.height * PadYFraction)
            val color = PulseColors[lane]
            val level = levels.getOrElse(lane) { .05f }.coerceIn(0f, 1f)
            val incoming = events
                .filter { it.lane == lane && it.targetMillis - renderMillis >= -EventLateWindowMillis }
                .minByOrNull { abs(it.targetMillis - renderMillis) }
            val ringProgress = incoming?.let { event ->
                (1f - (event.targetMillis - renderMillis).toFloat() / NoteTravelTimeMillis).coerceIn(0f, 1f)
            } ?: 0f
            val radius = (47f + level * 9f).dp.toPx()
            drawCircle(color.copy(alpha = .035f + level * .09f), radius * 1.65f, center)
            drawCircle(color.copy(alpha = .08f + level * .12f), radius * 1.28f, center)
            drawCircle(Color(0xFF10101E), radius, center)
            drawCircle(
                Brush.radialGradient(
                    listOf(color.copy(alpha = .18f + level * .38f), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius,
                center,
            )
            drawCircle(color.copy(alpha = .42f + level * .45f), radius, center, style = Stroke((1.5f + level * 2f).dp.toPx()))
            drawCircle(
                color = Color.White.copy(alpha = .08f),
                radius = radius + 7.dp.toPx(),
                center = center,
                style = Stroke(3.dp.toPx()),
            )
            if (ringProgress > 0f) {
                drawArc(
                    color = color.copy(alpha = .62f + ringProgress * .38f),
                    startAngle = -90f,
                    sweepAngle = ringProgress * 360f,
                    useCenter = false,
                    topLeft = center - Offset(radius + 7.dp.toPx(), radius + 7.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size((radius + 7.dp.toPx()) * 2f, (radius + 7.dp.toPx()) * 2f),
                    style = Stroke((3f + ringProgress * 1.5f).dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawLine(
                color.copy(alpha = .07f),
                start = Offset(center.x, size.height * .22f),
                end = Offset(center.x, center.y - radius * 1.25f),
                strokeWidth = 1.dp.toPx(),
            )
        }

        events.forEach { event ->
            val remaining = event.targetMillis - renderMillis
            if (remaining <= NoteTravelTimeMillis) {
                val travel = (1f - remaining.toFloat() / NoteTravelTimeMillis).coerceIn(0f, 1f)
                val easedTravel = travel * travel * (3f - 2f * travel)
                val target = Offset(size.width * PadXFractions[event.lane], size.height * PadYFraction)
                val startY = size.height * .22f
                val noteCenter = Offset(target.x, startY + (target.y - startY) * easedTravel)
                val color = PulseColors[event.lane]
                val noteRadius = (12f + event.strength * 5f).dp.toPx()
                drawLine(
                    color.copy(alpha = .12f + travel * .20f),
                    start = Offset(noteCenter.x, startY),
                    end = noteCenter,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(color.copy(alpha = .10f), noteRadius * 2.4f, noteCenter)
                drawCircle(color.copy(alpha = .24f), noteRadius * 1.55f, noteCenter)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White, color, color.copy(alpha = .76f)),
                        center = noteCenter - Offset(noteRadius * .22f, noteRadius * .25f),
                        radius = noteRadius * 1.2f,
                    ),
                    noteRadius,
                    noteCenter,
                )
                drawCircle(Color.White.copy(alpha = .82f), noteRadius, noteCenter, style = Stroke(1.5.dp.toPx()))

                if (remaining <= 0L) {
                    val age = (-remaining).toFloat()
                    val hitProgress = (age / EventLateWindowMillis).coerceIn(0f, 1f)
                    val flash = (1f - hitProgress) * event.strength
                    drawCircle(color.copy(alpha = flash * .34f), 51.dp.toPx(), target)
                    repeat(2) { ring ->
                        drawCircle(
                            color.copy(alpha = flash * (.62f - ring * .18f)),
                            (54 + hitProgress * 62 + ring * 12).dp.toPx(),
                            target,
                            style = Stroke((3f - ring * .7f).dp.toPx()),
                        )
                    }
                }
            }
        }

        bursts.forEach { burst ->
            val age = ((renderMillis - burst.startedAtMillis).toFloat() / 620f).coerceIn(0f, 1f)
            val origin = Offset(size.width * PadXFractions[burst.lane], size.height * PadYFraction)
            repeat(12) { particle ->
                val angle = particle * (Math.PI.toFloat() * 2f / 12f) + burst.id * .23f
                val distance = age * 94.dp.toPx()
                drawCircle(
                    burst.color.copy(alpha = 1f - age),
                    (4f * (1f - age)).dp.toPx().coerceAtLeast(.4f),
                    origin + Offset(cos(angle) * distance, sin(angle) * distance),
                )
            }
        }

        val bottomY = size.height - 5.dp.toPx()
        repeat(36) { index ->
            val fraction = index / 35f
            val lanePosition = fraction * 2f
            val left = lanePosition.toInt().coerceIn(0, 2)
            val right = (left + 1).coerceAtMost(2)
            val blend = lanePosition - left
            val level = levels[left] * (1f - blend) + levels[right] * blend
            val barHeight = (5f + level * 42f).dp.toPx()
            val x = size.width * fraction
            drawLine(
                color = PulseColors[left].copy(alpha = .12f + level * .24f),
                start = Offset(x, bottomY),
                end = Offset(x, bottomY - barHeight),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        if (!isPlaying) drawRect(Color(0xFF070711).copy(alpha = .58f))
    }
}

@Composable
private fun PadLabels(modifier: Modifier = Modifier) {
    val labels = listOf(
        stringResource(R.string.rhythm_game_bass),
        stringResource(R.string.rhythm_game_body),
        stringResource(R.string.rhythm_game_air),
    )
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        labels.forEachIndexed { index, label ->
            Text(
                label,
                modifier = Modifier.weight(1f),
                color = PulseColors[index].copy(alpha = .8f),
                textAlign = TextAlign.Center,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .8.sp,
            )
        }
    }
}

@Composable
private fun RoundGameButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = .07f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .1f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun GameStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = .045f),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .16f)),
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = color.copy(alpha = .72f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

private fun List<Float>.toLaneLevels(): List<Float> = listOf(
    take(9).averageOr(.04f),
    drop(9).take(12).averageOr(.04f),
    drop(21).averageOr(.04f),
)

private fun List<Float>.averageOr(fallback: Float): Float = if (isEmpty()) fallback else average().toFloat()

private fun RhythmGrade.labelResource(): Int = when (this) {
    RhythmGrade.Perfect -> R.string.rhythm_game_perfect
    RhythmGrade.Great -> R.string.rhythm_game_great
    RhythmGrade.Good -> R.string.rhythm_game_good
    RhythmGrade.Miss -> R.string.rhythm_game_miss
}

private fun RhythmGrade.color(): Color = when (this) {
    RhythmGrade.Perfect -> PulseColors[0]
    RhythmGrade.Great -> Color(0xFF9A7BFF)
    RhythmGrade.Good -> PulseColors[2]
    RhythmGrade.Miss -> Color(0xFFFF657A)
}
