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
    private var fadingOutPlayer: ExoPlayer? = null
    private var analyticsListener: AnalyticsListener? = null

    private fun swapPlayers() {
        val oldPlayer = currentPlayer
        val newPlayer = nextPlayer
        analyticsListener?.let {
            oldPlayer.removeAnalyticsListener(it)
            newPlayer.addAnalyticsListener(it)
        }
        currentPlayer = newPlayer
        nextPlayer = oldPlayer
        Timber.d("Swapped players. Current is now ${if (currentPlayer === playerA) "A" else "B"}")
    }

    // --- State Management ---
    private val listeners = mutableSetOf<Player.Listener>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var crossfadeJob: Job? = null
    private var isCrossfading = false
    var crossfadeConfig: CrossfadeConfig = CrossfadeConfig()
        private set

    private inner class InternalListener(private val player: ExoPlayer) : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (currentPlayer !== this.player) return
            listeners.forEach { it.onEvents(this@CrossfadePlayer, events) }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            listeners.forEach { it.onTimelineChanged(timeline, reason) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (currentPlayer !== this.player) return
            if (crossfadeConfig.isEnabled && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                if (!isCrossfading) {
                    startCrossfade(true)
                }
            } else {
                listeners.forEach { it.onMediaItemTransition(mediaItem, reason) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (currentPlayer !== this.player) return
            if (currentPlayer.playbackState == Player.STATE_ENDED &&
                crossfadeConfig.isEnabled &&
                currentPlayer.nextMediaItemIndex != C.INDEX_UNSET
            ) {
                return
            }
            listeners.forEach { it.onPlaybackStateChanged(playbackState) }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (currentPlayer !== this.player) return
            if (playWhenReady) {
                startPositionMonitor()
            } else {
                stopPositionMonitor()
                if (isCrossfading) {
                    forceStopCrossfade()
                }
            }
            listeners.forEach { it.onPlayWhenReadyChanged(playWhenReady, reason) }
        }
    }

    private var positionMonitorJob: Job? = null

    init {
        playerA.addListener(InternalListener(playerA))
        playerB.addListener(InternalListener(playerB))
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
    fun onSilenceProcessed(isSilent: Boolean) {
        if (!isSilent || !crossfadeConfig.isAutomatic || isCrossfading) return

        val duration = currentPlayer.duration
        if (duration == C.TIME_UNSET) return

        val position = currentPlayer.currentPosition
        val remaining = duration - position

        if (remaining < crossfadeConfig.triggerPosition) { // Only trigger if near the end
            startCrossfade(false)
        }
    }

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

                    if (remaining <= crossfadeConfig.triggerPosition) {
                        startCrossfade(false)
                        break
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
        if (isCrossfading) return
        if (currentPlayer.nextMediaItemIndex == C.INDEX_UNSET) return

        isCrossfading = true

        nextPlayer.seekTo(currentPlayer.nextMediaItemIndex, 0)
        nextPlayer.volume = 0f
        nextPlayer.playWhenReady = true
        nextPlayer.prepare()

        fadingOutPlayer = currentPlayer
        val fadingPlayer = fadingOutPlayer ?: return
        val oldTimeline = fadingPlayer.currentTimeline
        val oldWindow = Timeline.Window()
        val oldPeriod = Timeline.Period()
        if (!oldTimeline.isEmpty) {
            oldTimeline.getWindow(fadingPlayer.currentMediaItemIndex, oldWindow)
            oldTimeline.getPeriod(fadingPlayer.currentPeriodIndex, oldPeriod)
        }
        val oldPosition = Player.PositionInfo(
            oldWindow.uid,
            fadingPlayer.currentMediaItemIndex,
            fadingPlayer.currentMediaItem,
            oldPeriod.uid,
            fadingPlayer.currentPeriodIndex,
            fadingPlayer.currentPosition,
            fadingPlayer.contentPosition,
            fadingPlayer.currentAdGroupIndex,
            fadingPlayer.currentAdIndexInAdGroup
        )
        swapPlayers()
        val newTimeline = currentPlayer.currentTimeline
        val newWindow = Timeline.Window()
        val newPeriod = Timeline.Period()
        if (!newTimeline.isEmpty) {
            newTimeline.getWindow(currentPlayer.currentMediaItemIndex, newWindow)
            newTimeline.getPeriod(currentPlayer.currentPeriodIndex, newPeriod)
        }
        val newPosition = Player.PositionInfo(
            newWindow.uid,
            currentPlayer.currentMediaItemIndex,
            currentPlayer.currentMediaItem,
            newPeriod.uid,
            currentPlayer.currentPeriodIndex,
            currentPlayer.currentPosition,
            currentPlayer.contentPosition,
            currentPlayer.currentAdGroupIndex,
            currentPlayer.currentAdIndexInAdGroup
        )

        listeners.forEach {
            it.onMediaItemTransition(currentPlayer.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            it.onPositionDiscontinuity(oldPosition, newPosition, Player.DISCONTINUITY_REASON_AUTO_TRANSITION)
        }

        crossfadeJob = coroutineScope.launch {
            try {
                withContext(NonCancellable) {
                    val duration = crossfadeConfig.fadeDuration.toLong()
                    val startTime = System.currentTimeMillis()

                    while (isActive) {
                        val elapsedTime = System.currentTimeMillis() - startTime
                        val progress = (elapsedTime.toFloat() / duration).coerceIn(0f, 1f)
                        val fadeInVolume = crossfadeConfig.curve.interpolator.transform(progress)

                        currentPlayer.volume = fadeInVolume
                        fadingOutPlayer?.volume = 1f - fadeInVolume

                        if (progress >= 1f) {
                            break
                        }
                        delay(16)
                    }
                    fadingOutPlayer?.stop()
                    fadingOutPlayer?.volume = 1f
                }
            } finally {
                isCrossfading = false
                fadingOutPlayer = null
                if (playWhenReady) {
                    startPositionMonitor()
                }
            }
        }
    }

    private fun forceStopCrossfade() {
        if (!isCrossfading) return

        crossfadeJob?.cancel()
        fadingOutPlayer?.stop()
        fadingOutPlayer?.volume = 1f
        currentPlayer.volume = 1f

        isCrossfading = false
        fadingOutPlayer = null

        if (playWhenReady) {
            startPositionMonitor()
        }
        Timber.d("forceStopCrossfade: Crossfade stopped.")
    }

    // --- Player Interface Implementation ---

    override fun addListener(listener: Player.Listener) { listeners.add(listener) }
    override fun removeListener(listener: Player.Listener) { listeners.remove(listener) }

    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        forceStopCrossfade()
        val mutableMediaItems = mediaItems.toMutableList()
        playerA.setMediaItems(mutableMediaItems, startIndex, startPositionMs)
        playerB.setMediaItems(mutableMediaItems, startIndex, startPositionMs)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        forceStopCrossfade()
        val mutableMediaItems = mediaItems.toMutableList()
        playerA.setMediaItems(mutableMediaItems, resetPosition)
        playerB.setMediaItems(mutableMediaItems, resetPosition)
    }

    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
        forceStopCrossfade()
        val mutableMediaItems = mediaItems.toMutableList()
        playerA.addMediaItems(index, mutableMediaItems)
        playerB.addMediaItems(index, mutableMediaItems)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        forceStopCrossfade()
        playerA.removeMediaItems(fromIndex, toIndex)
        playerB.removeMediaItems(fromIndex, toIndex)
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        forceStopCrossfade()
        playerA.moveMediaItems(fromIndex, toIndex, newIndex)
        playerB.moveMediaItems(fromIndex, toIndex, newIndex)
    }

    override fun clearMediaItems() {
        forceStopCrossfade()
        playerA.clearMediaItems()
        playerB.clearMediaItems()
    }

    override fun prepare() {
        forceStopCrossfade()
        currentPlayer.prepare()
    }

    override fun play() {
        forceStopCrossfade()
        playWhenReady = true
    }

    override fun pause() {
        forceStopCrossfade()
        playWhenReady = false
    }

    override fun stop() {
        forceStopCrossfade()
        currentPlayer.stop()
        nextPlayer.stop()
        stopPositionMonitor()
    }
    override fun release() {
        playerA.release()
        playerB.release()
        coroutineScope.cancel()
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        forceStopCrossfade()
        currentPlayer.seekTo(mediaItemIndex, positionMs)
        nextPlayer.seekTo(mediaItemIndex, positionMs)
        nextPlayer.stop()
        if (playWhenReady) startPositionMonitor()
    }

    override fun getPlaybackState(): Int = currentPlayer.playbackState
    override fun getPlayWhenReady(): Boolean = currentPlayer.playWhenReady
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        currentPlayer.playWhenReady = playWhenReady
    }

    override fun getCurrentTimeline(): Timeline = currentPlayer.currentTimeline
    override fun getCurrentMediaItemIndex(): Int = currentPlayer.currentMediaItemIndex
    override fun getDuration(): Long = currentPlayer.duration

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

    override fun setMediaItem(mediaItem: MediaItem) = setMediaItems(mutableListOf(mediaItem), true)
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) = setMediaItems(mutableListOf(mediaItem), 0, startPositionMs)
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) = setMediaItems(listOf(mediaItem), resetPosition)
    override fun addMediaItem(mediaItem: MediaItem) = addMediaItems(mediaItemCount, listOf(mediaItem))
    override fun addMediaItem(index: Int, mediaItem: MediaItem) = addMediaItems(index, listOf(mediaItem))
    override fun addMediaItems(mediaItems: List<MediaItem>) = addMediaItems(mediaItemCount, mediaItems)
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = moveMediaItems(currentIndex, currentIndex + 1, newIndex)
    override fun removeMediaItem(index: Int) = removeMediaItems(index, index + 1)
    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        playerA.replaceMediaItem(index, mediaItem)
        playerB.replaceMediaItem(index, mediaItem)
    }
    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) {
        val mutableMediaItems = mediaItems.toMutableList()
        playerA.replaceMediaItems(fromIndex, toIndex, mutableMediaItems)
        playerB.replaceMediaItems(fromIndex, toIndex, mutableMediaItems)
    }
    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) { playerA.setAudioAttributes(audioAttributes, handleAudioFocus); playerB.setAudioAttributes(audioAttributes, handleAudioFocus) }

    @Deprecated("Use setMediaItems with a list of media items and a starting index instead.")
    override fun setMediaItems(mediaItems: List<MediaItem>) = setMediaItems(mediaItems, true)

    @Deprecated("Use seekToNextMediaItem instead.")
    override fun seekToNextWindow() = seekToNextMediaItem()

    @Deprecated("Use seekToPreviousMediaItem instead.")
    override fun seekToPreviousWindow() = seekToPreviousMediaItem()

    @Deprecated("Use hasNextMediaItem instead.")
    override fun hasNext(): Boolean = hasNextMediaItem()

    @Deprecated("Use hasNextMediaItem instead.")
    override fun hasNextWindow(): Boolean = hasNextMediaItem()

    @Deprecated("Use seekToNextMediaItem instead.")
    override fun next() = seekToNextMediaItem()

    override fun getCurrentManifest(): Any? = currentPlayer.currentManifest
    override fun getCurrentWindowIndex(): Int = currentMediaItemIndex
    override fun getNextWindowIndex(): Int = nextMediaItemIndex
    override fun getPreviousWindowIndex(): Int = previousMediaItemIndex
    override fun isCurrentWindowDynamic(): Boolean = isCurrentMediaItemDynamic
    override fun isCurrentWindowLive(): Boolean = isCurrentMediaItemLive
    override fun isCurrentWindowSeekable(): Boolean = isCurrentMediaItemSeekable

    @Deprecated("Use setDeviceVolume with a volume level and C.DEVICE_VOLUME_FLAGS_DEFAULT instead.")
    override fun setDeviceVolume(volume: Int) = setDeviceVolume(volume, 0)

    @Deprecated("Use increaseDeviceVolume with C.DEVICE_VOLUME_FLAGS_DEFAULT instead.")
    override fun increaseDeviceVolume() = increaseDeviceVolume(0)

    @Deprecated("Use decreaseDeviceVolume with C.DEVICE_VOLUME_FLAGS_DEFAULT instead.")
    override fun decreaseDeviceVolume() = decreaseDeviceVolume(0)

    @Deprecated("Use setDeviceMuted with a muted state and C.DEVICE_VOLUME_FLAGS_DEFAULT instead.")
    override fun setDeviceMuted(muted: Boolean) = setDeviceMuted(muted, 0)

    fun setAnalyticsListener(listener: AnalyticsListener) {
        this.analyticsListener = listener
        currentPlayer.addAnalyticsListener(listener)
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
