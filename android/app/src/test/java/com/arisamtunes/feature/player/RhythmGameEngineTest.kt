package com.arisamtunes.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmGameEngineTest {
    @Test
    fun timingWindowsAreSymmetricAndBounded() {
        assertEquals(RhythmGrade.Perfect, judgeRhythmTiming(-65))
        assertEquals(RhythmGrade.Perfect, judgeRhythmTiming(65))
        assertEquals(RhythmGrade.Great, judgeRhythmTiming(66))
        assertEquals(RhythmGrade.Good, judgeRhythmTiming(205))
        assertEquals(RhythmGrade.Miss, judgeRhythmTiming(206))
    }

    @Test
    fun pulseTimingAllowsNaturalReactionAfterTheSound() {
        assertEquals(RhythmGrade.Perfect, judgePulseTiming(80L))
        assertEquals(RhythmGrade.Great, judgePulseTiming(140L))
        assertEquals(RhythmGrade.Good, judgePulseTiming(260L))
        assertEquals(RhythmGrade.Miss, judgePulseTiming(300L))
    }

    @Test
    fun positionsAreStableAndStayInsidePlayField() {
        val first = rhythmPosition(42, 991)
        assertEquals(first, rhythmPosition(42, 991))
        assertTrue(first.x in 0.16f..0.84f)
        assertTrue(first.y in 0.25f..0.75f)
    }

    @Test
    fun spectralDetectorEmitsTheLaneThatActuallySpikes() {
        val detector = SpectralOnsetDetector()
        detector.update(1_000L, listOf(.10f, .10f, .10f))
        val onsets = detector.update(1_050L, listOf(.62f, .11f, .10f))

        assertEquals(1, onsets.size)
        assertEquals(0, onsets.single().lane)
    }

    @Test
    fun spectralDetectorRejectsSteadyEnergyAndHonorsCooldown() {
        val detector = SpectralOnsetDetector()
        detector.update(1_000L, listOf(.10f, .10f, .10f))
        val first = detector.update(1_050L, listOf(.10f, .55f, .10f))
        detector.update(1_090L, listOf(.10f, .12f, .10f))
        val tooSoon = detector.update(1_120L, listOf(.10f, .65f, .10f))

        assertEquals(1, first.size)
        assertTrue(tooSoon.none { it.lane == 1 })
    }

    @Test
    fun beatMapKeepsExactFrameTimestampAndPlayableSpacing() {
        fun frame(bass: Float, body: Float, air: Float) =
            List(9) { bass } + List(12) { body } + List(11) { air }
        val frames = listOf(
            frame(.10f, .10f, .10f),
            frame(.72f, .10f, .10f),
            frame(.11f, .10f, .10f),
            frame(.80f, .10f, .10f),
            frame(.10f, .10f, .10f),
            frame(.10f, .76f, .10f),
        )

        val notes = buildRhythmBeatMap(frames, frameDurationMillis = 60)

        assertEquals(2, notes.size)
        assertEquals(90L, notes[0].targetMillis)
        assertEquals(0, notes[0].lane)
        assertEquals(330L, notes[1].targetMillis)
        assertEquals(1, notes[1].lane)
    }
}
