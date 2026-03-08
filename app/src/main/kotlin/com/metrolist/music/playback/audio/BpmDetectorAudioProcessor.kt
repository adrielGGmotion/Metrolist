/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Pass-through audio processor that accumulates samples into a rolling buffer
 * and can trigger BPM analysis using a pure Kotlin autocorrelation-based detector.
 */
@UnstableApi
class BpmDetectorAudioProcessor : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    @Volatile
    var isAnalysisEnabled: Boolean = false

    var onBpmDetected: ((Float) -> Unit)? = null

    // 30 seconds of audio at 44100 Hz, 2 channels, 16-bit
    // Max buffer size in bytes: 44100 * 2 * 2 * 30 = 10,584,000
    private var rollingBuffer: ByteBuffer? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        // Allocate rolling buffer for 30 seconds
        val bufferSize = sampleRate * channelCount * 2 * 30
        if (rollingBuffer == null || rollingBuffer?.capacity() != bufferSize) {
            rollingBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.LITTLE_ENDIAN)
        }

        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            outputBuffer = EMPTY_BUFFER
            return
        }

        if (isAnalysisEnabled && rollingBuffer != null) {
            val remaining = inputBuffer.remaining()
            val rb = rollingBuffer!!
            
            if (remaining >= rb.capacity()) {
                // Input is larger than our whole buffer, just take the end
                rb.clear()
                val offset = inputBuffer.limit() - rb.capacity()
                val oldPos = inputBuffer.position()
                inputBuffer.position(oldPos + offset)
                rb.put(inputBuffer)
                inputBuffer.position(oldPos)
            } else {
                if (rb.remaining() < remaining) {
                    // Shift existing data to make room
                    val shift = remaining - rb.remaining()
                    val newDataSize = rb.position() - shift
                    if (newDataSize > 0) {
                        val temp = rb.duplicate()
                        temp.position(shift)
                        temp.limit(rb.position())
                        rb.clear()
                        rb.put(temp)
                    } else {
                        rb.clear()
                    }
                }
                rb.put(inputBuffer.duplicate())
            }
        }

        val size = inputBuffer.remaining()
        val out = replaceOutputBuffer(size)
        out.put(inputBuffer)
        out.flip()
    }

    fun triggerAnalysis() {
        val rb = rollingBuffer?.duplicate() ?: return
        rb.flip()
        if (rb.limit() == 0) {
            Timber.tag("AutomixBpm").w("No audio buffered for analysis")
            return
        }

        val currentSampleRate = sampleRate
        val currentChannelCount = channelCount

        scope.launch {
            try {
                // 1. Convert PCM 16-bit to FloatArray (mono)
                val frameCount = rb.limit() / 2 / currentChannelCount
                val floatBuffer = FloatArray(frameCount)
                rb.order(ByteOrder.LITTLE_ENDIAN)
                
                for (i in 0 until frameCount) {
                    var sum = 0f
                    for (c in 0 until currentChannelCount) {
                        sum += rb.short / 32768f
                    }
                    floatBuffer[i] = sum / currentChannelCount
                }

                // 2. Apply low-pass filter (~200Hz) to isolate bass/kick
                val cutoff = 200.0
                val dt = 1.0 / currentSampleRate
                val alpha = (2.0 * kotlin.math.PI * dt * cutoff) / (1.0 + 2.0 * kotlin.math.PI * dt * cutoff)
                var lastVal = 0f
                for (i in floatBuffer.indices) {
                    val filtered = lastVal + (alpha.toFloat() * (floatBuffer[i] - lastVal))
                    floatBuffer[i] = filtered
                    lastVal = filtered
                }

                // 3. Compute onset strength signal (RMS energy frames + positive derivative)
                val frameSize = (currentSampleRate * 0.01).toInt() // ~10ms
                val onsetSignals = FloatArray(frameCount / frameSize)
                var lastRms = 0f
                
                for (i in onsetSignals.indices) {
                    val start = i * frameSize
                    val end = start + frameSize
                    var sumSq = 0f
                    for (j in start until end) {
                        sumSq += floatBuffer[j] * floatBuffer[j]
                    }
                    val rms = sqrt(sumSq / frameSize)
                    onsetSignals[i] = max(0f, rms - lastRms)
                    lastRms = rms
                }
                
                if (onsetSignals.size < 10) {
                    Timber.tag("AutomixBpm").w("Not enough onset data")
                    return@launch
                }

                // 4. Autocorrelation over 60–180 BPM
                val onsetSampleRate = currentSampleRate.toFloat() / frameSize
                val minLag = (onsetSampleRate * 60f / 180f).toInt() // 180 BPM
                val maxLag = (onsetSampleRate * 60f / 60f).toInt() // 60 BPM
                
                var bestLag = -1
                var maxCorr = 0f
                
                // Normalize onset signal to improve autocorrelation
                val avgOnset = onsetSignals.average().toFloat()
                val normalizedOnsets = FloatArray(onsetSignals.size) { onsetSignals[it] - avgOnset }

                for (lag in minLag..maxLag) {
                    if (lag >= normalizedOnsets.size) break
                    
                    var corr = 0f
                    var energy = 0f
                    for (i in lag until normalizedOnsets.size) {
                        corr += normalizedOnsets[i] * normalizedOnsets[i - lag]
                        energy += normalizedOnsets[i] * normalizedOnsets[i]
                    }
                    
                    val normalizedCorr = if (energy > 0) corr / energy else 0f
                    if (normalizedCorr > maxCorr) {
                        maxCorr = normalizedCorr
                        bestLag = lag
                    }
                }

                // 5. Confidence check and Callback
                if (bestLag != -1 && maxCorr >= 0.2f) {
                    val detectedBpm = 60f * onsetSampleRate / bestLag
                    Timber.tag("AutomixBpm").i("AutomixBpm: detected = %.1f BPM (confidence=%.2f)", detectedBpm, maxCorr)
                    onBpmDetected?.invoke(detectedBpm)
                } else {
                    Timber.tag("AutomixBpm").w("AutomixBpm: unreliable detection (confidence=%.2f)", maxCorr)
                }
            } catch (e: Exception) {
                Timber.tag("AutomixBpm").e(e, "Error during BPM analysis")
            }
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER

    @Deprecated("Deprecated in AudioProcessor")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        rollingBuffer?.clear()
    }

    @Deprecated("Deprecated in AudioProcessor")
    override fun reset() {
        flush()
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        return outputBuffer
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}
