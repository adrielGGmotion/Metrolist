/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow

/**
 * Manages dual ExoPlayer instances for seamless audio crossfade transitions.
 * 
 * The core pattern is "Swap & Promote":
 * 1. Primary player plays current song until crossfadeDuration before end
 * 2. Secondary player preloads and starts playing next song (fading in)
 * 3. When primary reaches end, stop it BEFORE auto-advance
 * 4. Swap roles: secondary becomes primary
 * 
 * This avoids ExoPlayer's internal track transition which causes audio glitches.
 */
@OptIn(UnstableApi::class)
class CrossfadeManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val playerBuilder: () -> ExoPlayer,
) {
    companion object {
        private const val TAG = "CrossfadeManager"
        private const val POSITION_UPDATE_INTERVAL_MS = 50L
        private const val EARLY_STOP_THRESHOLD_MS = 100L // Stop primary this much before end
        private const val MIN_SONG_DURATION_FOR_CROSSFADE_MS = 5000L // Minimum 5 seconds
    }

    /**
     * Crossfade state machine
     */
    enum class CrossfadeState {
        /** No crossfade active, normal playback */
        IDLE,
        /** Secondary player is preparing */
        PREPARING,
        /** Crossfade in progress - volumes are animating */
        FADING,
        /** Crossfade complete, about to swap players */
        COMPLETING
    }

    // Dual players
    private var _playerA: ExoPlayer? = null
    private var _playerB: ExoPlayer? = null
    
    private val playerA: ExoPlayer
        get() = _playerA ?: playerBuilder().also { _playerA = it }
    
    private val playerB: ExoPlayer
        get() = _playerB ?: playerBuilder().also { _playerB = it }

    // Which player is currently the "main" one
    private var isPrimaryA = true
    
    val primaryPlayer: ExoPlayer
        get() = if (isPrimaryA) playerA else playerB
    
    private val secondaryPlayer: ExoPlayer
        get() = if (isPrimaryA) playerB else playerA

    // State
    private val _crossfadeState = MutableStateFlow(CrossfadeState.IDLE)
    val crossfadeState: StateFlow<CrossfadeState> = _crossfadeState.asStateFlow()
    
    private val _isCrossfading = MutableStateFlow(false)
    val isCrossfading: StateFlow<Boolean> = _isCrossfading.asStateFlow()

    // Configuration
    private var crossfadeEnabled = false
    private var crossfadeDurationMs = 5000L

    // Animation
    private val handler = Handler(Looper.getMainLooper())
    private var positionMonitorRunnable: Runnable? = null
    private var fadeAnimationRunnable: Runnable? = null
    private var fadeStartTime = 0L
    private var fadeJob: Job? = null
    
    // Track the next item we're fading into
    private var pendingNextItem: MediaItem? = null
    
    // Callback to notify MusicService when crossfade completes
    var onCrossfadeComplete: ((newPrimaryPlayer: ExoPlayer, previousIndex: Int) -> Unit)? = null
    
    // Callback to get the next media item from queue
    var getNextMediaItem: (() -> MediaItem?)? = null
    
    // Callback to check if crossfade should happen (e.g., not in repeat-one mode)
    var shouldCrossfade: (() -> Boolean)? = null

    /**
     * Initialize the manager with configuration
     */
    fun initialize(enabled: Boolean, durationSeconds: Int) {
        crossfadeEnabled = enabled
        crossfadeDurationMs = (durationSeconds * 1000L).coerceIn(1000L, 12000L)
        Log.d(TAG, "Initialized: enabled=$enabled, duration=${durationSeconds}s")
    }

    /**
     * Update crossfade settings dynamically
     */
    fun updateSettings(enabled: Boolean, durationSeconds: Int) {
        val wasEnabled = crossfadeEnabled
        crossfadeEnabled = enabled
        crossfadeDurationMs = (durationSeconds * 1000L).coerceIn(1000L, 12000L)
        
        if (wasEnabled && !enabled) {
            cancelCrossfade()
        }
        
        Log.d(TAG, "Settings updated: enabled=$enabled, duration=${durationSeconds}s")
    }

    /**
     * Start monitoring playback position for crossfade trigger
     */
    fun startPositionMonitoring() {
        stopPositionMonitoring()
        
        if (!crossfadeEnabled) return
        
        positionMonitorRunnable = object : Runnable {
            override fun run() {
                checkCrossfadeTrigger()
                handler.postDelayed(this, POSITION_UPDATE_INTERVAL_MS)
            }
        }
        handler.post(positionMonitorRunnable!!)
        Log.d(TAG, "Position monitoring started")
    }

    /**
     * Stop monitoring playback position
     */
    fun stopPositionMonitoring() {
        positionMonitorRunnable?.let { handler.removeCallbacks(it) }
        positionMonitorRunnable = null
    }

    /**
     * Check if we should trigger a crossfade
     */
    private fun checkCrossfadeTrigger() {
        if (!crossfadeEnabled) return
        if (_crossfadeState.value != CrossfadeState.IDLE) return
        if (shouldCrossfade?.invoke() == false) return

        val player = primaryPlayer
        if (!player.isPlaying) return
        
        val duration = player.duration
        if (duration == C.TIME_UNSET || duration <= 0) return
        
        // Don't crossfade if song is too short
        if (duration < crossfadeDurationMs + MIN_SONG_DURATION_FOR_CROSSFADE_MS) {
            return
        }

        val position = player.currentPosition
        val timeRemaining = duration - position
        
        // Start preparing when we're crossfadeDuration + 2 seconds from end
        val prepareThreshold = crossfadeDurationMs + 2000L
        
        if (timeRemaining <= prepareThreshold && timeRemaining > crossfadeDurationMs) {
            prepareSecondaryPlayer()
        } else if (timeRemaining <= crossfadeDurationMs && timeRemaining > EARLY_STOP_THRESHOLD_MS) {
            if (_crossfadeState.value == CrossfadeState.PREPARING || 
                _crossfadeState.value == CrossfadeState.IDLE) {
                startFadeAnimation()
            }
        }
    }

    /**
     * Prepare the secondary player with the next track
     */
    private fun prepareSecondaryPlayer() {
        val nextItem = getNextMediaItem?.invoke()
        if (nextItem == null) {
            Log.d(TAG, "No next item available for crossfade")
            return
        }
        
        pendingNextItem = nextItem
        _crossfadeState.value = CrossfadeState.PREPARING
        
        val secondary = secondaryPlayer
        secondary.stop()
        secondary.clearMediaItems()
        secondary.setMediaItem(nextItem)
        secondary.volume = 0f
        secondary.prepare()
        
        Log.d(TAG, "Secondary player preparing: ${nextItem.mediaId}")
    }

    /**
     * Start the fade animation
     */
    private fun startFadeAnimation() {
        if (_crossfadeState.value == CrossfadeState.FADING) return
        
        val secondary = secondaryPlayer
        if (secondary.playbackState != Player.STATE_READY) {
            // Secondary not ready yet, try to prepare it now
            if (pendingNextItem == null) {
                prepareSecondaryPlayer()
            }
            // Wait for it to be ready
            if (secondary.playbackState != Player.STATE_READY) {
                Log.d(TAG, "Secondary player not ready, waiting...")
                return
            }
        }
        
        _crossfadeState.value = CrossfadeState.FADING
        _isCrossfading.value = true
        fadeStartTime = System.currentTimeMillis()
        
        // Start secondary playback
        secondary.playWhenReady = true
        
        Log.d(TAG, "Fade animation started, duration: ${crossfadeDurationMs}ms")
        
        // Start fade animation loop
        fadeAnimationRunnable = object : Runnable {
            override fun run() {
                updateFadeVolumes()
            }
        }
        handler.post(fadeAnimationRunnable!!)
    }

    /**
     * Update volumes during fade animation using exponential curve
     */
    private fun updateFadeVolumes() {
        if (_crossfadeState.value != CrossfadeState.FADING) return
        
        val elapsed = System.currentTimeMillis() - fadeStartTime
        val progress = (elapsed.toFloat() / crossfadeDurationMs).coerceIn(0f, 1f)
        
        // Exponential fade curve for more natural sound
        val fadeOutVolume = 1f - exponentialCurve(progress)
        val fadeInVolume = exponentialCurve(progress)
        
        primaryPlayer.volume = fadeOutVolume
        secondaryPlayer.volume = fadeInVolume
        
        // Check if primary is about to end
        val primary = primaryPlayer
        val duration = primary.duration
        val position = primary.currentPosition
        val timeRemaining = if (duration > 0) duration - position else Long.MAX_VALUE
        
        if (progress >= 1f || timeRemaining <= EARLY_STOP_THRESHOLD_MS) {
            completeCrossfade()
        } else {
            handler.postDelayed(fadeAnimationRunnable!!, 16) // ~60fps
        }
    }

    /**
     * Exponential curve for volume fade (more natural sounding)
     */
    private fun exponentialCurve(progress: Float): Float {
        // Using x^2 for a smooth exponential feel
        return progress.pow(2)
    }

    /**
     * Complete the crossfade and swap players
     */
    private fun completeCrossfade() {
        _crossfadeState.value = CrossfadeState.COMPLETING
        
        fadeAnimationRunnable?.let { handler.removeCallbacks(it) }
        fadeAnimationRunnable = null
        
        val previousPrimary = primaryPlayer
        val previousIndex = previousPrimary.currentMediaItemIndex
        
        // Stop the old primary BEFORE it auto-advances
        previousPrimary.stop()
        previousPrimary.volume = 1f // Reset for next use
        
        // Swap the players
        isPrimaryA = !isPrimaryA
        
        // Ensure new primary is at full volume
        primaryPlayer.volume = 1f
        
        Log.d(TAG, "Crossfade complete, players swapped. New primary: ${if (isPrimaryA) "A" else "B"}")
        
        _crossfadeState.value = CrossfadeState.IDLE
        _isCrossfading.value = false
        pendingNextItem = null
        
        // Notify MusicService
        onCrossfadeComplete?.invoke(primaryPlayer, previousIndex)
    }

    /**
     * Cancel any active crossfade
     */
    fun cancelCrossfade() {
        if (_crossfadeState.value == CrossfadeState.IDLE) return
        
        Log.d(TAG, "Cancelling crossfade")
        
        fadeAnimationRunnable?.let { handler.removeCallbacks(it) }
        fadeAnimationRunnable = null
        
        // Reset volumes
        primaryPlayer.volume = 1f
        
        // Stop and reset secondary
        secondaryPlayer.stop()
        secondaryPlayer.clearMediaItems()
        secondaryPlayer.volume = 1f
        
        _crossfadeState.value = CrossfadeState.IDLE
        _isCrossfading.value = false
        pendingNextItem = null
    }

    /**
     * Pause both players during crossfade, or just primary if not crossfading
     */
    fun pause() {
        primaryPlayer.playWhenReady = false
        if (_isCrossfading.value) {
            secondaryPlayer.playWhenReady = false
        }
    }

    /**
     * Resume playback
     */
    fun resume() {
        primaryPlayer.playWhenReady = true
        if (_isCrossfading.value) {
            secondaryPlayer.playWhenReady = true
        }
    }

    /**
     * Handle user skip - cancel crossfade if active
     */
    fun onUserSkip() {
        cancelCrossfade()
    }

    /**
     * Handle seek - cancel crossfade if active
     */
    fun onSeek() {
        cancelCrossfade()
    }

    /**
     * Release all resources
     */
    fun release() {
        stopPositionMonitoring()
        cancelCrossfade()
        
        _playerA?.release()
        _playerB?.release()
        _playerA = null
        _playerB = null
        
        Log.d(TAG, "Released")
    }

    /**
     * Check if crossfade is currently enabled
     */
    fun isEnabled(): Boolean = crossfadeEnabled
}
