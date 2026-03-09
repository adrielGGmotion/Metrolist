/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import com.metrolist.music.db.MusicDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Singleton that manages BPM analysis for Automix.
 */
object AutomixAnalyzer {

    private var bpmProcessor: BpmDetectorAudioProcessor? = null
    private var scope = CoroutineScope(Dispatchers.IO)
    private var currentSongId: String? = null
    private var analysisJob: Job? = null

    var lastDetectedBpm: Float = 0f
        private set

    fun setProcessor(processor: BpmDetectorAudioProcessor, database: MusicDatabase) {
        bpmProcessor = processor
        processor.onBpmDetected = { bpm ->
            lastDetectedBpm = bpm
            Timber.tag("AutomixAnalyzer").d("Updated lastDetectedBpm = $bpm")
            currentSongId?.let { songId ->
                scope.launch {
                    database.updateSongBpm(songId, bpm)
                }
            }
        }
    }

    fun startListening(mediaId: String?, database: MusicDatabase) {
        Timber.tag("AutomixAnalyzer").d("startListening(mediaId=$mediaId)")
        analysisJob?.cancel()
        currentSongId = mediaId
        if (mediaId == null) {
            bpmProcessor?.isAnalysisEnabled = false
            return
        }

        scope.launch {
            val cachedBpm = database.getSongBpm(mediaId)
            if (cachedBpm != null && cachedBpm > 0f) {
                Timber.tag("AutomixBpm").d("AutomixBpm: cache hit = $cachedBpm BPM for songId $mediaId")
                lastDetectedBpm = cachedBpm
                bpmProcessor?.isAnalysisEnabled = false
                bpmProcessor?.onBpmDetected?.invoke(cachedBpm)
            } else {
                bpmProcessor?.isAnalysisEnabled = true
                analysisJob = scope.launch {
                    delay(30000L)
                    bpmProcessor?.triggerAnalysis()
                }
            }
        }
    }

    fun stopListening() {
        analysisJob?.cancel()
        Timber.tag("AutomixAnalyzer").d("stopListening()")
        bpmProcessor?.isAnalysisEnabled = false
    }

    fun requestBpm(callback: (Float) -> Unit) {
        Timber.tag("AutomixAnalyzer").d("requestBpm()")
        val processor = bpmProcessor
        if (processor == null) {
            Timber.tag("AutomixAnalyzer").w("Processor not set")
            callback(0f)
            return
        }

        val originalCallback = processor.onBpmDetected
        processor.onBpmDetected = { bpm ->
            lastDetectedBpm = bpm
            originalCallback?.invoke(bpm)
            callback(bpm)
            // Restore the original callback after one-shot request
            processor.onBpmDetected = originalCallback
        }
        processor.triggerAnalysis()
    }
}
