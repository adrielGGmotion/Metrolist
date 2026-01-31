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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.metrolist.music.constants.AutomixEnabledKey
import com.metrolist.music.constants.CrossfadeDurationKey
import com.metrolist.music.constants.CrossfadeEnabledKey
import com.metrolist.music.constants.CrossfadeSkipSameAlbumKey
import com.metrolist.music.extensions.metadata
import com.metrolist.music.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.pow

/**
 * Manages crossfade transitions between tracks using two ExoPlayer instances.
 * 
 * The crossfade works by:
 * 1. Monitoring the primary player's remaining time
 * 2. When remaining time <= crossfade duration, starts the fade player with the next track
 * 3. Gradually fades out primary player while fading in fade player
 * 4. When complete, the fade player becomes the primary player
 */
@OptIn(UnstableApi::class)
class CrossfadeManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val mediaSourceFactory: MediaSource.Factory,
    private val audioAttributes: AudioAttributes,
) {
    companion object {
        private const val TAG = "CrossfadeManager"
        private const val FADE_UPDATE_INTERVAL_MS = 50L
        private const val DEFAULT_CROSSFADE_DURATION = 5 // seconds
        private const val MIN_CROSSFADE_DURATION = 1
        private const val MAX_CROSSFADE_DURATION = 12
    }

    // The secondary player used for crossfade
    private var fadePlayer: ExoPlayer? = null
    
    // Current crossfade state
    private val _isCrossfading = MutableStateFlow(false)
    val isCrossfading: StateFlow<Boolean> = _isCrossfading.asStateFlow()
    
    // The media item currently loading/playing in fade player
    private var fadeMediaItem: MediaItem? = null
    
    // Fade job for volume transitions
    private var fadeJob: Job? = null
    
    // Handler for precise timing
    private val handler = Handler(Looper.getMainLooper())
    
    // Callback when crossfade completes and players should be swapped
    var onCrossfadeComplete: ((ExoPlayer) -> Unit)? = null
    
    // Callback to get the next media item from the queue
    var getNextMediaItem: (() -> MediaItem?)? = null
    
    /**
     * Check if crossfade is enabled in settings
     */
    fun isEnabled(): Boolean {
        return dataStore.get(CrossfadeEnabledKey, false)
    }
    
    /**
     * Check if automix is enabled (automix implies crossfade)
     */
    fun isAutomixEnabled(): Boolean {
        return dataStore.get(AutomixEnabledKey, false)
    }
    
    /**
     * Get the configured crossfade duration in seconds
     */
    fun getCrossfadeDuration(): Int {
        return dataStore.get(CrossfadeDurationKey, DEFAULT_CROSSFADE_DURATION)
            .coerceIn(MIN_CROSSFADE_DURATION, MAX_CROSSFADE_DURATION)
    }
    
    /**
     * Get the configured crossfade duration in milliseconds
     */
    fun getCrossfadeDurationMs(): Long {
        return getCrossfadeDuration() * 1000L
    }
    
    /**
     * Check if crossfade should be applied for the given track transition
     */
    fun shouldCrossfade(
        currentItem: MediaItem?,
        nextItem: MediaItem?,
        repeatMode: Int,
    ): Boolean {
        if (!isEnabled() && !isAutomixEnabled()) return false
        if (currentItem == null || nextItem == null) return false
        
        // Don't crossfade when repeat-one is enabled
        if (repeatMode == Player.REPEAT_MODE_ONE) {
            Log.d(TAG, "Skipping crossfade: repeat-one mode")
            return false
        }
        
        // Check if we should skip crossfade for same album (gapless albums)
        if (dataStore.get(CrossfadeSkipSameAlbumKey, true)) {
            val currentAlbum = currentItem.metadata?.album?.id
            val nextAlbum = nextItem.metadata?.album?.id
            
            if (currentAlbum != null && currentAlbum == nextAlbum) {
                Log.d(TAG, "Skipping crossfade: same album (gapless)")
                return false
            }
        }
        
        return true
    }
    
    /**
     * Prepare the fade player with the next track.
     * Should be called slightly before the crossfade needs to start.
     */
    fun prepareFadePlayer(nextItem: MediaItem) {
        if (_isCrossfading.value) {
            Log.d(TAG, "Already crossfading, skipping prepare")
            return
        }
        
        Log.d(TAG, "Preparing fade player for: ${nextItem.mediaId}")
        
        // Create fade player if needed
        if (fadePlayer == null) {
            fadePlayer = createFadePlayer()
        }
        
        fadeMediaItem = nextItem
        fadePlayer?.apply {
            setMediaItem(nextItem)
            volume = 0f // Start silent
            prepare()
        }
    }
    
    /**
     * Start the crossfade transition.
     * 
     * @param primaryPlayer The currently playing ExoPlayer
     * @param durationMs Override crossfade duration (for automix beat-aligned transitions)
     */
    fun startCrossfade(
        primaryPlayer: ExoPlayer,
        durationMs: Long = getCrossfadeDurationMs(),
    ) {
        if (_isCrossfading.value) {
            Log.d(TAG, "Already crossfading")
            return
        }
        
        val fade = fadePlayer ?: run {
            Log.e(TAG, "Fade player not prepared")
            return
        }
        
        Log.d(TAG, "Starting crossfade with duration: ${durationMs}ms")
        _isCrossfading.value = true
        
        // Start the fade player
        fade.play()
        
        // Start volume transition
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val initialPrimaryVolume = primaryPlayer.volume
            
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                
                // Use logarithmic curve for more natural volume fade
                val fadeOutVolume = calculateFadeOutVolume(progress, initialPrimaryVolume)
                val fadeInVolume = calculateFadeInVolume(progress)
                
                handler.post {
                    try {
                        primaryPlayer.volume = fadeOutVolume
                        fade.volume = fadeInVolume
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting volume", e)
                    }
                }
                
                if (progress >= 1f) {
                    break
                }
                
                delay(FADE_UPDATE_INTERVAL_MS)
            }
            
            // Crossfade complete
            completeCrossfade(primaryPlayer)
        }
    }
    
    /**
     * Calculate fade-out volume using logarithmic curve for natural sound
     */
    private fun calculateFadeOutVolume(progress: Float, initialVolume: Float): Float {
        // Logarithmic fade out: starts slow, accelerates at the end
        // Using exponential decay: e^(-k*x) where k controls the curve steepness
        val k = 3.0f
        val factor = kotlin.math.exp((-k * progress).toDouble()).toFloat()
        return (initialVolume * factor).coerceIn(0f, 1f)
    }
    
    /**
     * Calculate fade-in volume using logarithmic curve for natural sound
     */
    private fun calculateFadeInVolume(progress: Float): Float {
        // Logarithmic fade in: starts fast, slows at the end
        // Using: 1 - e^(-k*x)
        val k = 3.0f
        val volume = 1f - kotlin.math.exp((-k * progress).toDouble()).toFloat()
        return volume.coerceIn(0f, 1f)
    }
    
    /**
     * Complete the crossfade - sync primary player to fade player's position.
     * 
     * Key insight: We must complete the handoff BEFORE Song 1 naturally ends,
     * otherwise ExoPlayer auto-advances and causes issues.
     */
    private fun completeCrossfade(primaryPlayer: ExoPlayer) {
        Log.d(TAG, "Crossfade complete, syncing primary player")
        
        val incomingPlayer = fadePlayer ?: return
        val fadePosition = incomingPlayer.currentPosition
        
        handler.post {
            try {
                // 1. Stop Song 1 on primary player (before it naturally ends!)
                //    This prevents ExoPlayer from auto-advancing
                primaryPlayer.pause()
                
                // 2. Advance primary to the next track (Song 2)
                if (primaryPlayer.hasNextMediaItem()) {
                    primaryPlayer.seekToNextMediaItem()
                }
                
                // 3. Sync position with where the fade player is
                primaryPlayer.seekTo(fadePosition)
                
                // 4. Restore volume and resume playback
                primaryPlayer.volume = 1.0f
                primaryPlayer.play()
                
                // 5. Stop the fade player
                incomingPlayer.stop()
                incomingPlayer.volume = 0f
                incomingPlayer.clearMediaItems()
                
                Log.d(TAG, "Primary player synced to position ${fadePosition}ms")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during crossfade completion", e)
            } finally {
                fadeMediaItem = null
                _isCrossfading.value = false
                
                // Notify completion
                onCrossfadeComplete?.invoke(primaryPlayer)
            }
        }
    }
    
    /**
     * Cancel any ongoing crossfade and restore primary player state
     */
    fun cancelCrossfade(primaryPlayer: ExoPlayer? = null) {
        fadeJob?.cancel()
        fadeJob = null
        
        fadePlayer?.apply {
            stop()
            clearMediaItems()
            volume = 0f
        }
        
        // Restore primary player volume if crossfade was in progress
        if (_isCrossfading.value && primaryPlayer != null) {
            primaryPlayer.volume = 1.0f
        }
        
        fadeMediaItem = null
        _isCrossfading.value = false
        
        Log.d(TAG, "Crossfade cancelled")
    }
    
    /**
     * Create the fade player with same configuration as primary
     */
    private fun createFadePlayer(): ExoPlayer {
        Log.d(TAG, "Creating fade player")
        
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setHandleAudioBecomingNoisy(false) // Primary player handles this
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, false) // Don't manage focus
            .build()
            .apply {
                volume = 0f
            }
    }
    
    /**
     * Get the fade player (for manual control if needed)
     */
    fun getFadePlayer(): ExoPlayer? = fadePlayer
    
    /**
     * Check if fade player is prepared and ready
     */
    fun isFadePlayerReady(): Boolean {
        return fadePlayer?.playbackState == Player.STATE_READY
    }
    
    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing CrossfadeManager")
        
        cancelCrossfade()
        
        fadePlayer?.release()
        fadePlayer = null
        
        onCrossfadeComplete = null
        getNextMediaItem = null
    }
}
