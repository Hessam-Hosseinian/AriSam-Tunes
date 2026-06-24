package com.arisamtunes.catalog

import com.arisamtunes.plugins.DatabaseProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isRegularFile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SpectrumRepository(
    private val musicRoot: Path = Path.of(System.getenv("MUSIC_DATA_FOLDER") ?: "music_data"),
) {
    private val cache = ConcurrentHashMap<UUID, SongSpectrumResponse>()

    fun spectrum(songId: UUID): SongSpectrumResponse? = cache[songId] ?: sourcePath(songId)?.let { path ->
        buildSpectrum(songId, path).also { cache[songId] = it }
    }

    private fun sourcePath(songId: UUID): Path? = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement("SELECT source_relative_path FROM songs WHERE id = ?").use { s ->
            s.setObject(1, songId)
            s.executeQuery().use { results ->
                if (!results.next()) return null
                val relative = results.getString("source_relative_path") ?: return null
                val root = musicRoot.toAbsolutePath().normalize()
                root.resolve(relative).normalize().takeIf { it.startsWith(root) && it.isRegularFile() }
            }
        }
    }

    private fun buildSpectrum(songId: UUID, source: Path): SongSpectrumResponse {
        val samples = decodeSamples(source)
        if (samples.isEmpty()) return SongSpectrumResponse(songId.toString(), Bands, FrameDurationMs, emptyList())
        val samplesPerFrame = (SampleRate * FrameDurationMs) / 1000
        val windowSize = WindowSize.coerceAtMost(samples.size)
        val rawFrames = buildList {
            var offset = 0
            while (offset < samples.size && size < MaxFrames) {
                add(frameBands(samples, offset, windowSize))
                offset += samplesPerFrame
            }
        }
        val frames = smoothFrames(normalizeFrames(rawFrames))
        return SongSpectrumResponse(songId.toString(), Bands, FrameDurationMs, frames)
    }

    private fun decodeSamples(source: Path): FloatArray {
        val process = ProcessBuilder(
            "ffmpeg",
            "-v", "error",
            "-i", source.toString(),
            "-t", MaxDecodeSeconds.toString(),
            "-ac", "1",
            "-ar", SampleRate.toString(),
            "-f", "s16le",
            "-",
        ).redirectErrorStream(true).start()
        val bytes = process.inputStream.readBytes()
        process.waitFor()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 2) { buffer.short / Short.MAX_VALUE.toFloat() }
    }

    private fun frameBands(samples: FloatArray, offset: Int, windowSize: Int): List<Float> {
        val nyquist = SampleRate / 2f * 0.92f
        return List(Bands) { band ->
            val startHz = MinHz * (nyquist / MinHz).pow(band / Bands.toFloat())
            val endHz = MinHz * (nyquist / MinHz).pow((band + 1) / Bands.toFloat())
            val low = goertzel(samples, offset, windowSize, startHz * 0.72f + endHz * 0.28f)
            val mid = goertzel(samples, offset, windowSize, sqrt(startHz * endHz))
            val high = goertzel(samples, offset, windowSize, startHz * 0.25f + endHz * 0.75f)
            val magnitude = low * 0.25f + mid * 0.50f + high * 0.25f
            val normalized = (ln(1f + magnitude * 34f) / ln(35f)).coerceIn(0f, 1f)
            val psychoacousticBoost = when {
                band < 5 -> 1.28f
                band < 14 -> 1.08f
                else -> 0.92f
            }
            (normalized * psychoacousticBoost).coerceIn(0f, 1f)
        }
    }

    private fun normalizeFrames(frames: List<List<Float>>): List<List<Float>> {
        if (frames.isEmpty()) return frames
        val bandPeaks = List(Bands) { band ->
            frames.mapNotNull { it.getOrNull(band) }
                .sorted()
                .let { sorted -> sorted[(sorted.lastIndex * 0.92f).toInt().coerceIn(0, sorted.lastIndex)] }
                .coerceAtLeast(0.03f)
        }
        return frames.map { frame ->
            frame.mapIndexed { band, value ->
                val normalized = value / bandPeaks[band]
                val curved = normalized.pow(0.72f)
                val floor = if (band < 6) 0.07f else 0.04f
                (floor + curved * (1f - floor)).coerceIn(0f, 1f)
            }
        }
    }

    private fun smoothFrames(frames: List<List<Float>>): List<List<Float>> {
        if (frames.isEmpty()) return frames
        var previous = List(Bands) { 0.06f }
        return frames.map { frame ->
            val smoothed = frame.mapIndexed { band, target ->
                val attack = if (target > previous[band]) 0.62f else 0.28f
                (previous[band] * (1f - attack) + target * attack).coerceIn(0f, 1f)
            }
            previous = smoothed
            smoothed
        }
    }

    private fun goertzel(samples: FloatArray, offset: Int, size: Int, targetHz: Float): Float {
        val k = (0.5f + (size * targetHz / SampleRate)).toInt()
        val omega = 2.0 * PI * k / size
        val coeff = 2.0 * cos(omega)
        var q0: Double
        var q1 = 0.0
        var q2 = 0.0
        for (i in 0 until size) {
            val sample = samples.getOrNull(offset + i) ?: 0f
            val hann = 0.5 - 0.5 * cos((2.0 * PI * i) / (size - 1).coerceAtLeast(1))
            q0 = coeff * q1 - q2 + sample * hann
            q2 = q1
            q1 = q0
        }
        val real = q1 - q2 * cos(omega)
        val imag = q2 * sin(omega)
        return (sqrt(real * real + imag * imag) / size).toFloat()
    }

    private companion object {
        const val SampleRate = 16_000
        const val FrameDurationMs = 60
        const val WindowSize = 1_024
        const val Bands = 32
        const val MaxFrames = 3_000
        const val MaxDecodeSeconds = 180
        const val MinHz = 40f
    }
}
