package com.arisamtunes.feature.player

import kotlin.math.absoluteValue
import kotlin.math.max

internal enum class RhythmGrade(val points: Int, val accuracyWeight: Float) {
    Perfect(300, 1f),
    Great(150, .72f),
    Good(50, .4f),
    Miss(0, 0f),
}

internal fun judgeRhythmTiming(deltaMillis: Long): RhythmGrade = when (deltaMillis.absoluteValue) {
    in 0L..65L -> RhythmGrade.Perfect
    in 66L..125L -> RhythmGrade.Great
    in 126L..205L -> RhythmGrade.Good
    else -> RhythmGrade.Miss
}

internal fun judgePulseTiming(deltaMillis: Long): RhythmGrade = when (deltaMillis) {
    in -55L..85L -> RhythmGrade.Perfect
    in -75L..155L -> RhythmGrade.Great
    in -90L..285L -> RhythmGrade.Good
    else -> RhythmGrade.Miss
}

internal data class RhythmPosition(val x: Float, val y: Float)

internal data class SpectralOnset(val lane: Int, val strength: Float)

internal data class RhythmBeatNote(
    val targetMillis: Long,
    val lane: Int,
    val strength: Float,
)

internal fun buildRhythmBeatMap(
    frames: List<List<Float>>,
    frameDurationMillis: Int,
): List<RhythmBeatNote> {
    if (frames.isEmpty() || frameDurationMillis <= 0) return emptyList()
    val detector = SpectralOnsetDetector()
    val notes = mutableListOf<RhythmBeatNote>()
    var lastGlobalNoteAt = Long.MIN_VALUE / 2
    frames.forEachIndexed { index, frame ->
        val timestamp = index.toLong() * frameDurationMillis + frameDurationMillis / 2L
        val levels = listOf(
            frame.take(9).averageOrZero(),
            frame.drop(9).take(12).averageOrZero(),
            frame.drop(21).averageOrZero(),
        )
        val onset = detector.update(timestamp, levels).maxByOrNull(SpectralOnset::strength)
        if (onset != null && timestamp - lastGlobalNoteAt >= MinimumPlayableGapMillis) {
            notes += RhythmBeatNote(timestamp, onset.lane, onset.strength)
            lastGlobalNoteAt = timestamp
        }
    }
    return notes
}

/** Detects attacks independently in bass, body and high-frequency energy. */
internal class SpectralOnsetDetector(private val laneCount: Int = 3) {
    private val energyFloor = FloatArray(laneCount) { .08f }
    private val fluxFloor = FloatArray(laneCount) { .012f }
    private val previousEnergy = FloatArray(laneCount)
    private val lastOnsetMillis = LongArray(laneCount) { Long.MIN_VALUE / 2 }
    private var initialized = false

    fun update(timestampMillis: Long, energies: List<Float>): List<SpectralOnset> {
        require(energies.size == laneCount)
        if (!initialized) {
            energies.forEachIndexed { index, energy ->
                previousEnergy[index] = energy
                energyFloor[index] = max(.04f, energy)
            }
            initialized = true
            return emptyList()
        }

        val candidates = buildList {
            energies.forEachIndexed { lane, rawEnergy ->
                val energy = rawEnergy.coerceIn(0f, 1f)
                val flux = (energy - previousEnergy[lane]).coerceAtLeast(0f)
                val threshold = max(MinimumFlux[lane], fluxFloor[lane] * FluxMultiplier[lane])
                val cooldownElapsed = timestampMillis - lastOnsetMillis[lane] >= CooldownMillis[lane]
                val hasAttack = flux >= threshold && energy > energyFloor[lane] * EnergyMultiplier[lane]
                if (cooldownElapsed && hasAttack) {
                    val strength = (flux / threshold).coerceIn(.35f, 1f)
                    add(SpectralOnset(lane, strength))
                    lastOnsetMillis[lane] = timestampMillis
                }

                val floorWeight = if (energy < energyFloor[lane]) .12f else .018f
                energyFloor[lane] += (energy - energyFloor[lane]) * floorWeight
                fluxFloor[lane] += (flux - fluxFloor[lane]) * .075f
                previousEnergy[lane] = energy
            }
        }
        return candidates.sortedByDescending(SpectralOnset::strength).take(MaxSimultaneousOnsets)
    }

    private companion object {
        val MinimumFlux = floatArrayOf(.022f, .020f, .017f)
        val FluxMultiplier = floatArrayOf(1.85f, 2.0f, 2.15f)
        val EnergyMultiplier = floatArrayOf(1.04f, 1.035f, 1.025f)
        val CooldownMillis = longArrayOf(210L, 170L, 125L)
        const val MaxSimultaneousOnsets = 2
    }
}

private fun List<Float>.averageOrZero(): Float = if (isEmpty()) 0f else average().toFloat()

private const val MinimumPlayableGapMillis = 180L

/** Stable, well-spaced target positions that stay clear of the game HUD. */
internal fun rhythmPosition(beatIndex: Long, songSeed: Int): RhythmPosition {
    val mixed = (beatIndex * 1_103_515_245L + songSeed.toLong() * 12_345L + 0x9E3779B9L)
    val xBits = ((mixed xor (mixed ushr 17)) and 0xFFFF).toFloat() / 65_535f
    val yBits = (((mixed * 48_271L) xor (mixed ushr 11)) and 0xFFFF).toFloat() / 65_535f
    return RhythmPosition(
        x = .16f + xBits * .68f,
        y = .25f + yBits * .50f,
    )
}
