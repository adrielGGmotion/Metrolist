/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.beatroot.Agent
import be.tarsos.dsp.beatroot.AgentList
import be.tarsos.dsp.beatroot.Event
import be.tarsos.dsp.beatroot.EventList
import be.tarsos.dsp.beatroot.Induction
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pass-through audio processor that accumulates samples into a rolling buffer
 * and can trigger BPM analysis using TarsosDSP.
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
                // Convert PCM 16-bit to FloatArray
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

                // TarsosDSP BPM detection manually using onset detector and Induction
                val onsetList = EventList()
                val bufferSize = 512
                val detector = ComplexOnsetDetector(bufferSize, 0.5)
                detector.setHandler { time, salience ->
                    val event = Event(time, time, -1.0, 0, 0, -1.0, -1.0, 0)
                    event.salience = salience
                    onsetList.add(event)
                }
                
                val dispatcher = AudioDispatcherFactory.fromFloatArray(floatBuffer, currentSampleRate, bufferSize, 0)
                dispatcher.addAudioProcessor(detector)
                dispatcher.run()
                
                val agents = Induction.beatInduction(onsetList)
                val best = agents?.bestAgent()
                val detectedBpm = if (best != null && best.beatInterval > 0) (60.0 / best.beatInterval).toFloat() else 0f
                
                if (detectedBpm > 0) {
                    Timber.tag("AutomixBpm").i("AutomixBpm: detected = %.1f BPM", detectedBpm)
                    onBpmDetected?.invoke(detectedBpm)
                } else {
                    Timber.tag("AutomixBpm").w("AutomixBpm: could not detect BPM")
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
