/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import kotlin.math.sqrt

class BpmAudioProcessor : BaseAudioProcessor() {

    val currentBpm = MutableStateFlow<Double?>(null)
    val currentEnergy = MutableStateFlow<Double>(0.0)

    var isAnalysisEnabled = true

    private val silenceThreshold = 0.05

    private var tarsosFormat: TarsosDSPAudioFormat? = null
    private var onsetDetector: PercussionOnsetDetector? = null
    private var lastBeatTime = -1.0
    private val beatIntervals = DoubleArray(10)
    private var beatIntervalIndex = 0
    private var beatIntervalCount = 0

    private val energyHistory = DoubleArray(10)
    private var energyHistoryIndex = 0
    private var energyHistoryCount = 0

    private var sampleRate = 44100
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT

    private val processBufferSize = 1024
    private val overlap = 512
    private val hopSize = processBufferSize - overlap

    private var accumulator = FloatArray(4096)
    private var accumulatorSize = 0

    private val processFloatBuffer = FloatArray(processBufferSize)
    private var reusableAudioEvent: AudioEvent? = null

    private var inputCopyBuffer = ByteArray(4096)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        // TarsosDSP works internally with float samples, so we normalize inputs
        tarsosFormat = TarsosDSPAudioFormat(
            sampleRate.toFloat(),
            16, // internal bit depth for Tarsos reference, though we feed floats
            channelCount,
            true,
            false
        )

        onsetDetector = PercussionOnsetDetector(
            sampleRate.toFloat(),
            processBufferSize,
            overlap,
            { time, _ -> processBeat(time) }
        )

        reusableAudioEvent = AudioEvent(tarsosFormat)

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val remaining = inputBuffer.remaining()

        // Copy to output
        val outputBuffer = replaceOutputBuffer(remaining)

        if (inputCopyBuffer.size < remaining) {
            inputCopyBuffer = ByteArray(remaining * 2)
        }

        inputBuffer.get(inputCopyBuffer, 0, remaining)
        outputBuffer.put(inputCopyBuffer, 0, remaining)
        outputBuffer.flip()

        if (isAnalysisEnabled) {
            processAudio(inputCopyBuffer, remaining)
        }
    }

    private fun processAudio(bytes: ByteArray, length: Int) {
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val numSamples = length / (bytesPerSample * channelCount)

        if (accumulatorSize + numSamples > accumulator.size) {
             val newSize = (accumulatorSize + numSamples) * 2
             accumulator = accumulator.copyOf(newSize)
        }

        var byteIdx = 0
        for (i in 0 until numSamples) {
            var sum = 0.0f
            for (c in 0 until channelCount) {
                var sample = 0.0f
                if (encoding == C.ENCODING_PCM_FLOAT) {
                    // Float is little endian usually in Android
                    // Manual ByteBuffer extraction to avoid allocation
                    val b1 = bytes[byteIdx].toInt() and 0xFF
                    val b2 = bytes[byteIdx+1].toInt() and 0xFF
                    val b3 = bytes[byteIdx+2].toInt() and 0xFF
                    val b4 = bytes[byteIdx+3].toInt() and 0xFF
                    val intBits = (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
                    sample = Float.fromBits(intBits)
                } else {
                    // 16-bit
                    val low = bytes[byteIdx].toInt() and 0xFF
                    val high = bytes[byteIdx + 1].toInt()
                    sample = ((high shl 8) or low).toShort() / 32768.0f
                }
                sum += sample
                byteIdx += bytesPerSample
            }
            accumulator[accumulatorSize++] = sum / channelCount
        }

        // Energy Analysis
        var sumSquares = 0.0
        val startIdx = accumulatorSize - numSamples
        for (i in startIdx until accumulatorSize) {
            val s = accumulator[i]
            sumSquares += s * s
        }
        if (numSamples > 0) {
            val rms = sqrt(sumSquares / numSamples)
            updateEnergy(rms)
        }

        // Tarsos Processing
        while (accumulatorSize >= processBufferSize) {
            System.arraycopy(accumulator, 0, processFloatBuffer, 0, processBufferSize)

            reusableAudioEvent?.let { event ->
                event.floatBuffer = processFloatBuffer
                onsetDetector?.process(event)
            }

            val remaining = accumulatorSize - hopSize
            System.arraycopy(accumulator, hopSize, accumulator, 0, remaining)
            accumulatorSize = remaining
        }
    }

    private fun updateEnergy(rms: Double) {
        energyHistory[energyHistoryIndex] = rms
        energyHistoryIndex = (energyHistoryIndex + 1) % energyHistory.size
        if (energyHistoryCount < energyHistory.size) energyHistoryCount++

        var sum = 0.0
        for (i in 0 until energyHistoryCount) sum += energyHistory[i]

        currentEnergy.value = sum / energyHistoryCount
    }

    private fun processBeat(time: Double) {
        if (lastBeatTime > 0) {
            val interval = time - lastBeatTime
            if (interval > 0.3 && interval < 1.5) {
                beatIntervals[beatIntervalIndex] = interval
                beatIntervalIndex = (beatIntervalIndex + 1) % beatIntervals.size
                if (beatIntervalCount < beatIntervals.size) beatIntervalCount++

                var sum = 0.0
                for (i in 0 until beatIntervalCount) sum += beatIntervals[i]
                val avgInterval = sum / beatIntervalCount

                if (avgInterval > 0) {
                    val bpm = 60.0 / avgInterval
                    currentBpm.value = bpm
                }
            }
        }
        lastBeatTime = time
    }

    fun isReadyToMix(): Boolean {
        return currentEnergy.value < silenceThreshold
    }

    fun getBpm(): Double? {
        return currentBpm.value
    }

    override fun onReset() {
        currentEnergy.value = 0.0
        currentBpm.value = null
        beatIntervalCount = 0
        beatIntervalIndex = 0
        energyHistoryCount = 0
        energyHistoryIndex = 0
        lastBeatTime = -1.0
        accumulatorSize = 0
    }
}
