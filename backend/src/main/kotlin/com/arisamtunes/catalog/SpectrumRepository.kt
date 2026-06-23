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
        val windowSize = 512.coerceAtMost(samples.size)
        val frames = buildList {
            var offset = 0
            while (offset < samples.size && size < MaxFrames) {
                add(frameBands(samples, offset, windowSize))
                offset += samplesPerFrame
            }
        }
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
        val nyquist = SampleRate / 2f
        return List(Bands) { band ->
            val startHz = 35f * (nyquist / 35f).pow(band / Bands.toFloat())
            val endHz = 35f * (nyquist / 35f).pow((band + 1) / Bands.toFloat())
            val hz = (startHz + endHz) * 0.5f
            val magnitude = goertzel(samples, offset, windowSize, hz)
            val normalized = (ln(1f + magnitude * 18f) / ln(19f)).coerceIn(0f, 1f)
            if (band < 4) (normalized * 1.18f).coerceAtMost(1f) else normalized
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
        const val SampleRate = 8_000
        const val FrameDurationMs = 100
        const val Bands = 24
        const val MaxFrames = 1_800
        const val MaxDecodeSeconds = 180
    }
}
