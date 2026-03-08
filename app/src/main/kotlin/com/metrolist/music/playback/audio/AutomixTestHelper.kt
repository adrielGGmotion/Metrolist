/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

/**
 * Helper to run BPM detection self-tests.
 */
object AutomixTestHelper {

    suspend fun runBpmSelfTest() {
        val sampleRate = 44100
        val channelCount = 2
        val durationSeconds = 30
        val bpm = 120.0
        val intervalSeconds = 60.0 / bpm // 0.5s for 120 BPM

        Timber.tag("AutomixTest").d("Starting BPM self-test (expected 120.0 BPM)")

        // Generate synthetic click track
        val frameCount = sampleRate * durationSeconds
        val bufferSize = frameCount * channelCount * 2
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until frameCount) {
            val time = i.toDouble() / sampleRate
            // Click every intervalSeconds
            val isClick = (time % intervalSeconds) < 0.01 // 10ms click
            val sampleValue = if (isClick) {
                // 1kHz tone for the click
                (sin(2.0 * Math.PI * 1000.0 * time) * 32767.0).toInt().toShort()
            } else {
                0.toShort()
            }
            
            repeat(channelCount) {
                byteBuffer.putShort(sampleValue)
            }
        }
        byteBuffer.flip()

        val processor = BpmDetectorAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(sampleRate, channelCount, C.ENCODING_PCM_16BIT))
        processor.isAnalysisEnabled = true
        
        val deferredBpm = CompletableDeferred<Float>()
        processor.onBpmDetected = { detectedBpm ->
            deferredBpm.complete(detectedBpm)
        }

        // Feed the processor
        processor.queueInput(byteBuffer)
        processor.triggerAnalysis()

        val resultBpm = withTimeoutOrNull(5000) {
            deferredBpm.await()
        } ?: 0f

        Timber.tag("AutomixTest").i("AutomixTest: self-test result = %.1f BPM (expected 120.0)", resultBpm)
    }
}
