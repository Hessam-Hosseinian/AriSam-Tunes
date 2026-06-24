package com.arisamtunes.feature.player

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Receives decoded PCM from ExoPlayer's audio pipeline and turns it into 32
 * log-spaced FFT-like bands for the player visualizer.
 *
 * This intentionally does not use Android's system Visualizer API, because that
 * API requires RECORD_AUDIO on modern Android. The signal here is the actual
 * song PCM that ExoPlayer is about to send to AudioTrack.
 */
class RealtimeFftAudioBufferSink(
    private val onBands: (List<Float>) -> Unit,
) : TeeAudioProcessor.AudioBufferSink {
    @Volatile
    var lastBandUpdateMillis: Long = 0L
        private set

    private var sampleRateHz = DefaultSampleRateHz
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT
    private val sampleRing = FloatArray(WindowSize)
    private val fftReal = FloatArray(WindowSize)
    private val fftImag = FloatArray(WindowSize)
    private val smoothedBands = FloatArray(BandCount) { IdleLevel }
    private var ringWriteIndex = 0
    private var bufferedSamples = 0
    private var samplesSinceAnalysis = 0

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz.takeIf { it > 0 } ?: DefaultSampleRateHz
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding
        sampleRing.fill(0f)
        smoothedBands.fill(IdleLevel)
        ringWriteIndex = 0
        bufferedSamples = 0
        samplesSinceAnalysis = 0
        lastBandUpdateMillis = 0L
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val pcm = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        when (encoding) {
            C.ENCODING_PCM_16BIT -> readPcm16(pcm)
            C.ENCODING_PCM_FLOAT -> readPcmFloat(pcm)
            else -> Unit
        }
    }

    fun decayedBands(): List<Float> {
        for (index in smoothedBands.indices) {
            smoothedBands[index] = max(IdleLevel, smoothedBands[index] * 0.86f)
        }
        return smoothedBands.toList()
    }

    private fun readPcm16(buffer: ByteBuffer) {
        val bytesPerFrame = channelCount * ShortBytes
        while (buffer.remaining() >= bytesPerFrame) {
            var mono = 0f
            repeat(channelCount) {
                mono += buffer.short / Short.MAX_VALUE.toFloat()
            }
            appendSample(mono / channelCount)
        }
    }

    private fun readPcmFloat(buffer: ByteBuffer) {
        val bytesPerFrame = channelCount * FloatBytes
        while (buffer.remaining() >= bytesPerFrame) {
            var mono = 0f
            repeat(channelCount) {
                mono += buffer.float.coerceIn(-1f, 1f)
            }
            appendSample(mono / channelCount)
        }
    }

    private fun appendSample(sample: Float) {
        sampleRing[ringWriteIndex] = sample
        ringWriteIndex = (ringWriteIndex + 1) % WindowSize
        bufferedSamples = min(WindowSize, bufferedSamples + 1)
        samplesSinceAnalysis++

        val now = SystemClock.elapsedRealtime()
        if (
            bufferedSamples == WindowSize &&
            samplesSinceAnalysis >= AnalysisHopSamples &&
            now - lastBandUpdateMillis >= MinUpdateIntervalMillis
        ) {
            samplesSinceAnalysis = 0
            analyze(now)
        }
    }

    private fun analyze(now: Long) {
        var rms = 0f
        for (i in 0 until WindowSize) {
            val sample = sampleRing[(ringWriteIndex + i) % WindowSize]
            rms += sample * sample
        }
        rms = sqrt(rms / WindowSize)

        val nyquist = sampleRateHz / 2f
        val minFrequency = 40f
        val maxFrequency = min(16_000f, nyquist * 0.92f).coerceAtLeast(minFrequency + 1f)
        val logMin = log10(minFrequency)
        val logMax = log10(maxFrequency)

        for (i in 0 until WindowSize) {
            val sample = sampleRing[(ringWriteIndex + i) % WindowSize]
            val window = (0.5f - 0.5f * cos((2.0 * PI * i) / (WindowSize - 1))).toFloat()
            fftReal[i] = sample * window
            fftImag[i] = 0f
        }
        fft(fftReal, fftImag)

        for (band in 0 until BandCount) {
            val lowerFraction = band / BandCount.toFloat()
            val upperFraction = (band + 1) / BandCount.toFloat()
            val lowerFrequency = 10.0.pow((logMin + (logMax - logMin) * lowerFraction).toDouble()).toFloat()
            val upperFrequency = 10.0.pow((logMin + (logMax - logMin) * upperFraction).toDouble()).toFloat()
            val magnitude = averageMagnitude(lowerFrequency, upperFrequency)
            val fraction = band / (BandCount - 1f)
            val bassLift = 1f + (1f - fraction) * 0.72f
            val highTrim = 1f - fraction * 0.18f
            val normalized = (
                log10(1f + magnitude * 56f * bassLift) * highTrim +
                    log10(1f + rms * 18f) * 0.20f
                ).coerceIn(0f, 1f)

            val previous = smoothedBands[band]
            val attack = if (normalized > previous) 0.74f else 0.34f
            smoothedBands[band] = (previous + (normalized - previous) * attack).coerceIn(IdleLevel, 1f)
        }

        lastBandUpdateMillis = now
        onBands(smoothedBands.toList())
    }

    private fun averageMagnitude(lowerFrequency: Float, upperFrequency: Float): Float {
        val lowerBin = max(1, floor(lowerFrequency * WindowSize / sampleRateHz).toInt())
        val upperBin = min(WindowSize / 2 - 1, ceil(upperFrequency * WindowSize / sampleRateHz).toInt())
        if (upperBin < lowerBin) return 0f

        var sum = 0f
        var count = 0
        for (bin in lowerBin..upperBin) {
            val real = fftReal[bin]
            val imaginary = fftImag[bin]
            sum += sqrt(real * real + imaginary * imaginary)
            count++
        }
        return if (count == 0) 0f else sum / count / WindowSize
    }

    private fun fft(real: FloatArray, imaginary: FloatArray) {
        var swapIndex = 0
        for (index in 1 until WindowSize) {
            var bit = WindowSize shr 1
            while (swapIndex and bit != 0) {
                swapIndex = swapIndex xor bit
                bit = bit shr 1
            }
            swapIndex = swapIndex xor bit
            if (index < swapIndex) {
                val realTemp = real[index]
                real[index] = real[swapIndex]
                real[swapIndex] = realTemp

                val imaginaryTemp = imaginary[index]
                imaginary[index] = imaginary[swapIndex]
                imaginary[swapIndex] = imaginaryTemp
            }
        }

        var length = 2
        while (length <= WindowSize) {
            val angle = -2.0 * PI / length
            val stepReal = cos(angle).toFloat()
            val stepImaginary = sin(angle).toFloat()
            var start = 0
            while (start < WindowSize) {
                var currentReal = 1f
                var currentImaginary = 0f
                val halfLength = length / 2
                for (offset in 0 until halfLength) {
                    val evenIndex = start + offset
                    val oddIndex = evenIndex + halfLength

                    val oddReal = real[oddIndex] * currentReal - imaginary[oddIndex] * currentImaginary
                    val oddImaginary = real[oddIndex] * currentImaginary + imaginary[oddIndex] * currentReal

                    real[oddIndex] = real[evenIndex] - oddReal
                    imaginary[oddIndex] = imaginary[evenIndex] - oddImaginary
                    real[evenIndex] += oddReal
                    imaginary[evenIndex] += oddImaginary

                    val nextReal = currentReal * stepReal - currentImaginary * stepImaginary
                    currentImaginary = currentReal * stepImaginary + currentImaginary * stepReal
                    currentReal = nextReal
                }
                start += length
            }

            length = length shl 1
        }
    }

    private companion object {
        const val BandCount = 32
        const val WindowSize = 1024
        const val AnalysisHopSamples = 512
        const val MinUpdateIntervalMillis = 32L
        const val IdleLevel = 0.05f
        const val DefaultSampleRateHz = 44_100
        const val ShortBytes = 2
        const val FloatBytes = 4
    }
}
