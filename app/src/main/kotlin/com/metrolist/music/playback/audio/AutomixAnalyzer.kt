/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import timber.log.Timber

/**
 * Singleton that manages BPM analysis for Automix.
 */
object AutomixAnalyzer {
    
    private var bpmProcessor: BpmDetectorAudioProcessor? = null
    
    var lastDetectedBpm: Float = 0f
        private set

    fun setProcessor(processor: BpmDetectorAudioProcessor) {
        bpmProcessor = processor
        processor.onBpmDetected = { bpm ->
            lastDetectedBpm = bpm
            Timber.tag("AutomixAnalyzer").d("Updated lastDetectedBpm = $bpm")
        }
    }

    fun startListening() {
        Timber.tag("AutomixAnalyzer").d("startListening()")
        bpmProcessor?.isAnalysisEnabled = true
    }

    fun stopListening() {
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
