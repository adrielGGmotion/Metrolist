package com.metrolist.music.playback

import android.content.Context
import android.os.Looper
import androidx.media3.common.*
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ShuffleOrder
import com.metrolist.music.extensions.setOffloadEnabled
import kotlinx.coroutines.*
import timber.log.Timber

@UnstableApi
class CrossfadePlayer(
    private val context: Context,
    private val mediaSourceFactory: MediaSource.Factory,
    private val renderersFactory: RenderersFactory
) : Player {

    // --- Internal Players ---
    private val playerA: ExoPlayer = buildExoPlayer()
    private val playerB: ExoPlayer = buildExoPlayer()
    private var currentPlayer: ExoPlayer = playerA
    private var nextPlayer: ExoPlayer = playerB

    private fun swapPlayers() {
        val temp = currentPlayer
        currentPlayer = nextPlayer
        Timber.d("Swapped players. Current is now ${if (currentPlayer === playerA) "A" else "B"}")
        nextPlayer = temp
    }

    // --- State Management ---
    private val listeners = mutableSetOf<Player.Listener>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var crossfadeJob: Job? = null
    var crossfadeConfig: CrossfadeConfig = CrossfadeConfig()
        private set

    // --- Player State Cache ---
    // This is the source of truth for external listeners.

    private val internalListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player !== currentPlayer) {
                Timber.v("Ignoring events from nextPlayer.")
                return
            }
            Timber.v("Forwarding events from currentPlayer: $events")
            listeners.forEach { it.onEvents(this@CrossfadePlayer, events) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val playerLabel = if (currentPlayer === playerA) "A" else "B"
            Timber.d("onMediaItemTransition on player $playerLabel. Reason: $reason. Crossfade enabled: ${crossfadeConfig.isEnabled}")
            if (crossfadeConfig.isEnabled && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                if (crossfadeJob?.isActive != true) {
                    Timber.d("Internal player transitioned, starting crossfade.")
                    startCrossfade(true)
                } else {
                    Timber.d("Internal player transitioned, but crossfade is already active. Ignoring.")
                }
            } else {
                Timber.d("Forwarding onMediaItemTransition to listeners.")
                listeners.forEach { it.onMediaItemTransition(mediaItem, reason) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (currentPlayer.playbackState == Player.STATE_ENDED && crossfadeConfig.isEnabled) {
                // Don't propagate the STATE_ENDED if we are about to crossfade
                return
            }
            listeners.forEach { it.onPlaybackStateChanged(playbackState) }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady) {
                startPositionMonitor()
            } else {
                stopPositionMonitor()
                crossfadeJob?.cancel()
            }
            listeners.forEach { it.onPlayWhenReadyChanged(playWhenReady, reason) }
        }
    }

    private var positionMonitorJob: Job? = null

    init {
        playerA.addListener(internalListener)
        playerB.addListener(internalListener)
    }

    private fun buildExoPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setRenderersFactory(renderersFactory)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false
            )
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
    }

    // --- Custom Methods & Logic ---
    fun setCrossfadeConfig(config: CrossfadeConfig) {
        this.crossfadeConfig = config
        if (config.isEnabled && playWhenReady) {
            startPositionMonitor()
        } else {
            stopPositionMonitor()
        }
    }

    private fun startPositionMonitor() {
        stopPositionMonitor()
        if (!crossfadeConfig.isEnabled) return

        positionMonitorJob = coroutineScope.launch {
            while (isActive) {
                val duration = currentPlayer.duration
                if (duration != C.TIME_UNSET) {
                    val position = currentPlayer.currentPosition
                    val remaining = duration - position

                    if (remaining <= crossfadeConfig.duration) {
                        startCrossfade(false)
                        break // Monitor will be restarted after successful transition
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopPositionMonitor() {
        positionMonitorJob?.cancel()
        positionMonitorJob = null
    }

    private fun startCrossfade(isManualTransition: Boolean) {
        if (crossfadeJob?.isActive == true) {
            Timber.w("startCrossfade called but crossfadeJob is already active.")
            return
        }
        if (currentPlayer.nextMediaItemIndex == C.INDEX_UNSET) {
            Timber.w("startCrossfade called but there is no next media item.")
            return
        }
        Timber.i("Starting crossfade. Manual transition: $isManualTransition")

        crossfadeJob = coroutineScope.launch {
            // 1. Prepare the next player
            Timber.d("Preparing nextPlayer for crossfade.")
            nextPlayer.seekTo(currentPlayer.nextMediaItemIndex, 0)
            nextPlayer.volume = 0f
            nextPlayer.playWhenReady = true
            nextPlayer.prepare()

            // 2. Start fading
            Timber.d("Fading volume over ${crossfadeConfig.duration}ms.")
            val duration = crossfadeConfig.duration.toLong()
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val progress = (elapsedTime.toFloat() / duration).coerceIn(0f, 1f)
                val newVolume = crossfadeConfig.curve.transform(progress)

                nextPlayer.volume = newVolume
                currentPlayer.volume = 1f - newVolume

                if (progress >= 1f) {
                    break
                }
                delay(16) // ~60fps
            }

            // 3. Finalize the switch
            Timber.d("Crossfade fade complete. Stopping old player.")
            val oldPlayer = currentPlayer
            oldPlayer.stop()
            oldPlayer.volume = 1f // Reset volume for next use

            swapPlayers()

            // 4. Manually dispatch the transition event
            Timber.i("Manually dispatching onMediaItemTransition to listeners.")
            val currentMediaItem = currentPlayer.currentMediaItem
            listeners.forEach {
                it.onMediaItemTransition(currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            }

            // 5. Restart the monitor for the new player
            if (playWhenReady) {
                Timber.d("Restarting position monitor for new player.")
                startPositionMonitor()
            }
        }
    }

    // --- Player Interface Implementation ---

    // Listener Management
    override fun addListener(listener: Player.Listener) { listeners.add(listener) }
    override fun removeListener(listener: Player.Listener) { listeners.remove(listener) }

    // Playlist Management (centralized)
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
        playerA.setMediaItems(mediaItems, startIndex, startPositionMs)
        playerB.setMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        playerA.setMediaItems(mediaItems, resetPosition)
        playerB.setMediaItems(mediaItems, resetPosition)
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        playerA.addMediaItems(index, mediaItems)
        playerB.addMediaItems(index, mediaItems)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        playerA.removeMediaItems(fromIndex, toIndex)
        playerB.removeMediaItems(fromIndex, toIndex)
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        playerA.moveMediaItems(fromIndex, toIndex, newIndex)
        playerB.moveMediaItems(fromIndex, toIndex, newIndex)
    }

    override fun clearMediaItems() {
        playerA.clearMediaItems()
        playerB.clearMediaItems()
    }

    // Playback Control
    override fun prepare() { currentPlayer.prepare() }
    override fun play() { playWhenReady = true }
    override fun pause() { playWhenReady = false }
    override fun stop() {
        currentPlayer.stop()
        nextPlayer.stop()
        crossfadeJob?.cancel()
        stopPositionMonitor()
    }
    override fun release() {
        playerA.release()
        playerB.release()
        coroutineScope.cancel()
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        crossfadeJob?.cancel()
        currentPlayer.seekTo(mediaItemIndex, positionMs)
        // Also move the next player to the same position to keep it in sync
        nextPlayer.seekTo(mediaItemIndex, positionMs)
        nextPlayer.stop() // Stop it from playing immediately
        if (playWhenReady) startPositionMonitor()
    }

    // State Getters
    override fun getPlaybackState(): Int = currentPlayer.playbackState
    override fun getPlayWhenReady(): Boolean = currentPlayer.playWhenReady
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        currentPlayer.playWhenReady = playWhenReady
        // This will trigger the onPlayWhenReadyChanged listener, which handles the monitor.
    }

    override fun getCurrentTimeline(): Timeline = currentPlayer.currentTimeline
    override fun getCurrentMediaItemIndex(): Int = currentPlayer.currentMediaItemIndex
    override fun getDuration(): Long = currentPlayer.duration

    // Simple Delegations (to current player)
    override fun getApplicationLooper(): Looper = currentPlayer.applicationLooper
    override fun isCommandAvailable(command: Int): Boolean = currentPlayer.isCommandAvailable(command)
    override fun canAdvertiseSession(): Boolean = currentPlayer.canAdvertiseSession()
    override fun getAvailableCommands(): Player.Commands = currentPlayer.availableCommands
    override fun getPlaybackSuppressionReason(): Int = currentPlayer.playbackSuppressionReason
    override fun isPlaying(): Boolean = currentPlayer.isPlaying
    override fun getPlayerError(): PlaybackException? = currentPlayer.playerError
    override fun isLoading(): Boolean = currentPlayer.isLoading
    override fun seekToDefaultPosition() = seekTo(currentMediaItemIndex, C.TIME_UNSET)
    override fun seekToDefaultPosition(mediaItemIndex: Int) = seekTo(mediaItemIndex, C.TIME_UNSET)
    override fun seekTo(positionMs: Long) = seekTo(currentMediaItemIndex, positionMs)
    override fun getSeekBackIncrement(): Long = currentPlayer.seekBackIncrement
    override fun seekBack() = currentPlayer.seekBack()
    override fun getSeekForwardIncrement(): Long = currentPlayer.seekForwardIncrement
    override fun seekForward() = currentPlayer.seekForward()
    override fun hasPreviousMediaItem(): Boolean = currentPlayer.hasPreviousMediaItem()
    override fun seekToPrevious() = currentPlayer.seekToPrevious()
    override fun seekToPreviousMediaItem() = currentPlayer.seekToPreviousMediaItem()
    override fun getMaxSeekToPreviousPosition(): Long = currentPlayer.maxSeekToPreviousPosition
    override fun hasNextMediaItem(): Boolean = currentPlayer.hasNextMediaItem()
    override fun seekToNext() = currentPlayer.seekToNext()
    override fun seekToNextMediaItem() = currentPlayer.seekToNextMediaItem()
    override fun getPlaybackParameters(): PlaybackParameters = currentPlayer.playbackParameters
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) { currentPlayer.playbackParameters = playbackParameters }
    override fun setPlaybackSpeed(speed: Float) { currentPlayer.setPlaybackSpeed(speed) }
    override fun getCurrentTracks(): Tracks = currentPlayer.currentTracks
    override fun getTrackSelectionParameters(): TrackSelectionParameters = currentPlayer.trackSelectionParameters
    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) { currentPlayer.trackSelectionParameters = parameters }
    override fun getMediaMetadata(): MediaMetadata = currentPlayer.mediaMetadata
    override fun getPlaylistMetadata(): MediaMetadata = currentPlayer.playlistMetadata
    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        playerA.playlistMetadata = mediaMetadata
        playerB.playlistMetadata = mediaMetadata
    }
    override fun getCurrentPeriodIndex(): Int = currentPlayer.currentPeriodIndex
    override fun getNextMediaItemIndex(): Int = currentPlayer.nextMediaItemIndex
    override fun getPreviousMediaItemIndex(): Int = currentPlayer.previousMediaItemIndex
    override fun getCurrentMediaItem(): MediaItem? = currentPlayer.currentMediaItem
    override fun getMediaItemCount(): Int = currentPlayer.mediaItemCount
    override fun getMediaItemAt(index: Int): MediaItem = currentPlayer.getMediaItemAt(index)
    override fun getCurrentPosition(): Long = currentPlayer.currentPosition
    override fun getBufferedPosition(): Long = currentPlayer.bufferedPosition
    override fun getBufferedPercentage(): Int = currentPlayer.bufferedPercentage
    override fun getTotalBufferedDuration(): Long = currentPlayer.totalBufferedDuration
    override fun isCurrentMediaItemDynamic(): Boolean = currentPlayer.isCurrentMediaItemDynamic
    override fun isCurrentMediaItemLive(): Boolean = currentPlayer.isCurrentMediaItemLive
    override fun getCurrentLiveOffset(): Long = currentPlayer.currentLiveOffset
    override fun getContentDuration(): Long = currentPlayer.contentDuration
    override fun getContentPosition(): Long = currentPlayer.contentPosition
    override fun getContentBufferedPosition(): Long = currentPlayer.contentBufferedPosition
    override fun isPlayingAd(): Boolean = currentPlayer.isPlayingAd
    override fun getCurrentAdGroupIndex(): Int = currentPlayer.currentAdGroupIndex
    override fun getCurrentAdIndexInAdGroup(): Int = currentPlayer.currentAdIndexInAdGroup
    override fun isCurrentMediaItemSeekable(): Boolean = currentPlayer.isCurrentMediaItemSeekable
    override fun getVideoSize(): VideoSize = currentPlayer.videoSize
    override fun getSurfaceSize(): androidx.media3.common.util.Size = currentPlayer.surfaceSize
    override fun clearVideoSurface() = currentPlayer.clearVideoSurface()
    override fun clearVideoSurface(surface: android.view.Surface?) = currentPlayer.clearVideoSurface(surface)
    override fun setVideoSurface(surface: android.view.Surface?) = currentPlayer.setVideoSurface(surface)
    override fun setVideoSurfaceHolder(surfaceHolder: android.view.SurfaceHolder?) = currentPlayer.setVideoSurfaceHolder(surfaceHolder)
    override fun clearVideoSurfaceHolder(surfaceHolder: android.view.SurfaceHolder?) = currentPlayer.clearVideoSurfaceHolder(surfaceHolder)
    override fun setVideoSurfaceView(surfaceView: android.view.SurfaceView?) = currentPlayer.setVideoSurfaceView(surfaceView)
    override fun clearVideoSurfaceView(surfaceView: android.view.SurfaceView?) = currentPlayer.clearVideoSurfaceView(surfaceView)
    override fun setVideoTextureView(textureView: android.view.TextureView?) = currentPlayer.setVideoTextureView(textureView)
    override fun clearVideoTextureView(textureView: android.view.TextureView?) = currentPlayer.clearVideoTextureView(textureView)
    override fun getAudioAttributes(): AudioAttributes = currentPlayer.audioAttributes
    override fun setVolume(volume: Float) { currentPlayer.volume = volume }
    override fun getVolume(): Float = currentPlayer.volume
    override fun getCurrentCues(): CueGroup = currentPlayer.currentCues
    override fun getDeviceInfo(): DeviceInfo = currentPlayer.deviceInfo
    override fun getDeviceVolume(): Int = currentPlayer.deviceVolume
    override fun isDeviceMuted(): Boolean = currentPlayer.isDeviceMuted
    override fun setDeviceVolume(volume: Int, flags: Int) = currentPlayer.setDeviceVolume(volume, flags)
    override fun increaseDeviceVolume(flags: Int) = currentPlayer.increaseDeviceVolume(flags)
    override fun decreaseDeviceVolume(flags: Int) = currentPlayer.decreaseDeviceVolume(flags)
    override fun setDeviceMuted(muted: Boolean, flags: Int) = currentPlayer.setDeviceMuted(muted, flags)

    // Delegations that apply to BOTH players
    override fun getRepeatMode(): Int = currentPlayer.repeatMode
    override fun setRepeatMode(repeatMode: Int) {
        playerA.repeatMode = repeatMode
        playerB.repeatMode = repeatMode
    }
    override fun getShuffleModeEnabled(): Boolean = currentPlayer.shuffleModeEnabled
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        playerA.shuffleModeEnabled = shuffleModeEnabled
        playerB.shuffleModeEnabled = shuffleModeEnabled
    }

    // --- Not implemented properly / TODOs ---
    override fun setMediaItem(mediaItem: MediaItem) = setMediaItems(mutableListOf(mediaItem), true)
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) = setMediaItems(mutableListOf(mediaItem), 0, startPositionMs)
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) = setMediaItems(mutableListOf(mediaItem), resetPosition)
    override fun addMediaItem(mediaItem: MediaItem) = addMediaItems(mediaItemCount, mutableListOf(mediaItem))
    override fun addMediaItem(index: Int, mediaItem: MediaItem) = addMediaItems(index, mutableListOf(mediaItem))
    override fun addMediaItems(mediaItems: MutableList<MediaItem>) = addMediaItems(mediaItemCount, mediaItems)
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = moveMediaItems(currentIndex, currentIndex + 1, newIndex)
    override fun removeMediaItem(index: Int) = removeMediaItems(index, index + 1)
    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        playerA.replaceMediaItem(index, mediaItem)
        playerB.replaceMediaItem(index, mediaItem)
    }
    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>) {
        playerA.replaceMediaItems(fromIndex, toIndex, mediaItems)
        playerB.replaceMediaItems(fromIndex, toIndex, mediaItems)
    }
    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) { playerA.setAudioAttributes(audioAttributes, handleAudioFocus); playerB.setAudioAttributes(audioAttributes, handleAudioFocus) }

    // Deprecated methods
    override fun setMediaItems(mediaItems: MutableList<MediaItem>) = setMediaItems(mediaItems, true)
    override fun seekToPreviousWindow() = seekToPreviousMediaItem()
    override fun hasNext(): Boolean = hasNextMediaItem()
    override fun hasNextWindow(): Boolean = hasNextMediaItem()
    override fun next() = seekToNextMediaItem()
    override fun seekToNextWindow() = seekToNextMediaItem()
    override fun getCurrentManifest(): Any? = currentPlayer.currentManifest
    override fun getCurrentWindowIndex(): Int = currentMediaItemIndex
    override fun getNextWindowIndex(): Int = nextMediaItemIndex
    override fun getPreviousWindowIndex(): Int = previousMediaItemIndex
    override fun isCurrentWindowDynamic(): Boolean = isCurrentMediaItemDynamic
    override fun isCurrentWindowLive(): Boolean = isCurrentMediaItemLive
    override fun isCurrentWindowSeekable(): Boolean = isCurrentMediaItemSeekable
    override fun setDeviceVolume(volume: Int) = setDeviceVolume(volume, 0)
    override fun increaseDeviceVolume() = increaseDeviceVolume(0)
    override fun decreaseDeviceVolume() = decreaseDeviceVolume(0)
    override fun setDeviceMuted(muted: Boolean) = setDeviceMuted(muted, 0)

    // --- Analytics & Other Proxies ---
    fun addAnalyticsListener(listener: AnalyticsListener) {
        playerA.addAnalyticsListener(listener)
        playerB.addAnalyticsListener(listener)
    }

    fun setOffloadEnabled(offloadEnabled: Boolean) {
        playerA.setOffloadEnabled(offloadEnabled)
        playerB.setOffloadEnabled(offloadEnabled)
    }

    var skipSilenceEnabled: Boolean
        get() = playerA.skipSilenceEnabled
        set(value) {
            playerA.skipSilenceEnabled = value
            playerB.skipSilenceEnabled = value
        }

    val audioSessionId: Int
        get() = currentPlayer.audioSessionId

    fun setShuffleOrder(shuffleOrder: ShuffleOrder) {
        playerA.setShuffleOrder(shuffleOrder)
        playerB.setShuffleOrder(shuffleOrder)
    }
}
