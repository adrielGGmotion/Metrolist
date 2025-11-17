package com.metrolist.music.playback

import android.content.Context
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ShuffleOrder
import com.metrolist.music.extensions.setOffloadEnabled
import kotlinx.coroutines.*

class CrossfadePlayer(
    private val context: Context,
    private val mediaSourceFactory: MediaSource.Factory,
    private val renderersFactory: RenderersFactory
) : Player {

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

    private val playerA: ExoPlayer = buildExoPlayer()
    private val playerB: ExoPlayer = buildExoPlayer()
    private var activePlayer: ExoPlayer = playerA
    private fun getInactivePlayer() = if (activePlayer === playerA) playerB else playerA

    var crossfadeConfig: CrossfadeConfig = CrossfadeConfig()
        private set
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionMonitorJob: Job? = null
    private var crossfadeJob: Job? = null

    private val listeners = mutableSetOf<Player.Listener>()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            listeners.forEach { it.onEvents(this@CrossfadePlayer, events) }
        }
    }

    init {
        playerA.addListener(playerListener)
        playerB.addListener(playerListener)
    }

    // Custom Methods
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
        get() = activePlayer.audioSessionId

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
        if (!crossfadeConfig.isEnabled || !playWhenReady) return

        positionMonitorJob = scope.launch {
            while (isActive) {
                val duration = activePlayer.duration
                if (duration == C.TIME_UNSET) {
                    delay(500)
                    continue
                }

                val position = activePlayer.currentPosition
                val remaining = duration - position
                val crossfadeDuration = crossfadeConfig.duration

                var triggerCrossfade = remaining <= crossfadeDuration

                if (crossfadeConfig.isAutomatic && remaining < crossfadeDuration + 5000) {
                    val isSilent = false // Placeholder for actual silence detection
                    if (isSilent) {
                        triggerCrossfade = true
                    }
                }

                if (triggerCrossfade) {
                    startCrossfade()
                    break
                }
                delay(250)
            }
        }
    }

    private fun stopPositionMonitor() {
        positionMonitorJob?.cancel()
        positionMonitorJob = null
    }

    private fun startCrossfade() {
        if (crossfadeJob?.isActive == true) return
        val nextPlayer = getInactivePlayer()
        val currentPlayer = activePlayer

        val nextIndex = currentPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return

        val nextItem = currentPlayer.getMediaItemAt(nextIndex)
        nextPlayer.setMediaItem(nextItem)
        nextPlayer.volume = 0f
        nextPlayer.prepare()
        nextPlayer.play()

        crossfadeJob = scope.launch {
            val duration = crossfadeConfig.duration.toLong()
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val progress = (elapsedTime.toFloat() / duration).coerceIn(0f, 1f)
                val fadeInVolume = crossfadeConfig.curve.transform(progress)

                nextPlayer.volume = fadeInVolume
                currentPlayer.volume = 1f - fadeInVolume

                if (progress >= 1f) {
                    currentPlayer.stop()
                    activePlayer = nextPlayer

                    listeners.forEach { it.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) }
                    listeners.forEach { it.onMediaItemTransition(nextPlayer.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) }

                    startPositionMonitor()
                    break
                }
                delay(16) // ~60fps
            }
        }
    }

    fun setShuffleOrder(shuffleOrder: ShuffleOrder) {
        playerA.setShuffleOrder(shuffleOrder)
        playerB.setShuffleOrder(shuffleOrder)
    }

    // Player Interface Implementation
    override fun getApplicationLooper(): Looper = activePlayer.applicationLooper
    override fun addListener(listener: Player.Listener) { listeners.add(listener) }
    override fun removeListener(listener: Player.Listener) { listeners.remove(listener) }
    override fun setMediaItems(mediaItems: MutableList<MediaItem>) = activePlayer.setMediaItems(mediaItems)
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) = activePlayer.setMediaItems(mediaItems, resetPosition)
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) = activePlayer.setMediaItems(mediaItems, startIndex, startPositionMs)
    override fun setMediaItem(mediaItem: MediaItem) = activePlayer.setMediaItem(mediaItem)
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) = activePlayer.setMediaItem(mediaItem, startPositionMs)
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) = activePlayer.setMediaItem(mediaItem, resetPosition)
    override fun addMediaItem(mediaItem: MediaItem) = activePlayer.addMediaItem(mediaItem)
    override fun addMediaItem(index: Int, mediaItem: MediaItem) = activePlayer.addMediaItem(index, mediaItem)
    override fun addMediaItems(mediaItems: MutableList<MediaItem>) = activePlayer.addMediaItems(mediaItems)
    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) = activePlayer.addMediaItems(index, mediaItems)
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = activePlayer.moveMediaItem(currentIndex, newIndex)
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) = activePlayer.moveMediaItems(fromIndex, toIndex, newIndex)
    override fun removeMediaItem(index: Int) = activePlayer.removeMediaItem(index)
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) = activePlayer.removeMediaItems(fromIndex, toIndex)
    override fun clearMediaItems() = activePlayer.clearMediaItems()
    override fun isCommandAvailable(command: Int): Boolean = activePlayer.isCommandAvailable(command)
    override fun canAdvertiseSession(): Boolean = activePlayer.canAdvertiseSession()
    override fun getAvailableCommands(): Player.Commands = activePlayer.availableCommands
    override fun prepare() = activePlayer.prepare()
    override fun getPlaybackState(): Int = activePlayer.playbackState
    override fun getPlaybackSuppressionReason(): Int = activePlayer.playbackSuppressionReason
    override fun isPlaying(): Boolean = activePlayer.isPlaying
    override fun getPlayerError(): androidx.media3.common.PlaybackException? = activePlayer.playerError

    override fun play() {
        activePlayer.play()
        startPositionMonitor()
    }

    override fun pause() {
        activePlayer.pause()
        stopPositionMonitor()
    }

    override fun stop() {
        activePlayer.stop()
        getInactivePlayer().stop()
        stopPositionMonitor()
        crossfadeJob?.cancel()
    }

    override fun release() {
        playerA.release()
        playerB.release()
        scope.cancel()
    }

    override fun getPlayWhenReady(): Boolean = activePlayer.playWhenReady
    override fun setPlayWhenReady(playWhenReady: Boolean) { activePlayer.playWhenReady = playWhenReady }
    override fun getRepeatMode(): Int = activePlayer.repeatMode
    override fun setRepeatMode(repeatMode: Int) { activePlayer.repeatMode = repeatMode }
    override fun getShuffleModeEnabled(): Boolean = activePlayer.shuffleModeEnabled
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) { activePlayer.shuffleModeEnabled = shuffleModeEnabled }
    override fun isLoading(): Boolean = activePlayer.isLoading
    override fun seekToDefaultPosition() = activePlayer.seekToDefaultPosition()
    override fun seekToDefaultPosition(mediaItemIndex: Int) = activePlayer.seekToDefaultPosition(mediaItemIndex)
    override fun seekTo(positionMs: Long) = activePlayer.seekTo(positionMs)
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) = activePlayer.seekTo(mediaItemIndex, positionMs)
    override fun getSeekBackIncrement(): Long = activePlayer.seekBackIncrement
    override fun seekBack() = activePlayer.seekBack()
    override fun getSeekForwardIncrement(): Long = activePlayer.seekForwardIncrement
    override fun seekForward() = activePlayer.seekForward()
    override fun hasPreviousMediaItem(): Boolean = activePlayer.hasPreviousMediaItem()
    override fun seekToPrevious() = activePlayer.seekToPrevious()
    override fun seekToPreviousMediaItem() = activePlayer.seekToPreviousMediaItem()
    override fun getMaxSeekToPreviousPosition(): Long = activePlayer.maxSeekToPreviousPosition
    override fun hasNextMediaItem(): Boolean = activePlayer.hasNextMediaItem()
    override fun seekToNext() = activePlayer.seekToNext()
    override fun seekToNextMediaItem() = activePlayer.seekToNextMediaItem()
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) { activePlayer.playbackParameters = playbackParameters }
    override fun setPlaybackSpeed(speed: Float) { activePlayer.setPlaybackSpeed(speed) }
    override fun getPlaybackParameters(): PlaybackParameters = activePlayer.playbackParameters
    override fun getCurrentTracks(): Tracks = activePlayer.currentTracks
    override fun getTrackSelectionParameters(): TrackSelectionParameters = activePlayer.trackSelectionParameters
    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) { activePlayer.trackSelectionParameters = parameters }
    override fun getMediaMetadata(): MediaMetadata = activePlayer.mediaMetadata
    override fun getPlaylistMetadata(): MediaMetadata = activePlayer.playlistMetadata
    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) { activePlayer.playlistMetadata = mediaMetadata }
    override fun getCurrentTimeline(): Timeline = activePlayer.currentTimeline
    override fun getCurrentPeriodIndex(): Int = activePlayer.currentPeriodIndex
    override fun getCurrentMediaItemIndex(): Int = activePlayer.currentMediaItemIndex
    override fun getNextMediaItemIndex(): Int = activePlayer.nextMediaItemIndex
    override fun getPreviousMediaItemIndex(): Int = activePlayer.previousMediaItemIndex
    override fun getCurrentMediaItem(): MediaItem? = activePlayer.currentMediaItem
    override fun getMediaItemCount(): Int = activePlayer.mediaItemCount
    override fun getMediaItemAt(index: Int): MediaItem = activePlayer.getMediaItemAt(index)
    override fun getDuration(): Long = activePlayer.duration
    override fun getCurrentPosition(): Long = activePlayer.currentPosition
    override fun getBufferedPosition(): Long = activePlayer.bufferedPosition
    override fun getBufferedPercentage(): Int = activePlayer.bufferedPercentage
    override fun getTotalBufferedDuration(): Long = activePlayer.totalBufferedDuration
    override fun isCurrentMediaItemDynamic(): Boolean = activePlayer.isCurrentMediaItemDynamic
    override fun isCurrentMediaItemLive(): Boolean = activePlayer.isCurrentMediaItemLive
    override fun getCurrentLiveOffset(): Long = activePlayer.currentLiveOffset
    override fun getContentDuration(): Long = activePlayer.contentDuration
    override fun getContentPosition(): Long = activePlayer.contentPosition
    override fun getContentBufferedPosition(): Long = activePlayer.contentBufferedPosition
    override fun isPlayingAd(): Boolean = activePlayer.isPlayingAd
    override fun getCurrentAdGroupIndex(): Int = activePlayer.currentAdGroupIndex
    override fun getCurrentAdIndexInAdGroup(): Int = activePlayer.currentAdIndexInAdGroup
    override fun isCurrentMediaItemSeekable(): Boolean = activePlayer.isCurrentMediaItemSeekable
    override fun getVideoSize(): VideoSize = activePlayer.videoSize
    override fun getSurfaceSize(): androidx.media3.common.util.Size = activePlayer.surfaceSize
    override fun clearVideoSurface() = activePlayer.clearVideoSurface()
    override fun clearVideoSurface(surface: android.view.Surface?) = activePlayer.clearVideoSurface(surface)
    override fun setVideoSurface(surface: android.view.Surface?) = activePlayer.setVideoSurface(surface)
    override fun setVideoSurfaceHolder(surfaceHolder: android.view.SurfaceHolder?) = activePlayer.setVideoSurfaceHolder(surfaceHolder)
    override fun clearVideoSurfaceHolder(surfaceHolder: android.view.SurfaceHolder?) = activePlayer.clearVideoSurfaceHolder(surfaceHolder)
    override fun setVideoSurfaceView(surfaceView: android.view.SurfaceView?) = activePlayer.setVideoSurfaceView(surfaceView)
    override fun clearVideoSurfaceView(surfaceView: android.view.SurfaceView?) = activePlayer.clearVideoSurfaceView(surfaceView)
    override fun setVideoTextureView(textureView: android.view.TextureView?) = activePlayer.setVideoTextureView(textureView)
    override fun clearVideoTextureView(textureView: android.view.TextureView?) = activePlayer.clearVideoTextureView(textureView)
    override fun getAudioAttributes(): AudioAttributes = activePlayer.audioAttributes
    override fun setVolume(volume: Float) { activePlayer.volume = volume }
    override fun getVolume(): Float = activePlayer.volume
    override fun getCurrentCues(): CueGroup = activePlayer.currentCues
    override fun getDeviceInfo(): DeviceInfo = activePlayer.deviceInfo
    override fun getDeviceVolume(): Int = activePlayer.deviceVolume
    override fun isDeviceMuted(): Boolean = activePlayer.isDeviceMuted
    override fun setDeviceVolume(volume: Int, flags: Int) = activePlayer.setDeviceVolume(volume, flags)
    override fun increaseDeviceVolume(flags: Int) = activePlayer.increaseDeviceVolume(flags)
    override fun decreaseDeviceVolume(flags: Int) = activePlayer.decreaseDeviceVolume(flags)
    override fun setDeviceMuted(muted: Boolean, flags: Int) = activePlayer.setDeviceMuted(muted, flags)
    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) = activePlayer.replaceMediaItem(index, mediaItem)
    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>) = activePlayer.replaceMediaItems(fromIndex, toIndex, mediaItems)
    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) = activePlayer.setAudioAttributes(audioAttributes, handleAudioFocus)
    override fun seekToPreviousWindow() = activePlayer.seekToPreviousWindow()
    override fun hasNext(): Boolean = activePlayer.hasNext()
    override fun hasNextWindow(): Boolean = activePlayer.hasNextWindow()
    override fun next() = activePlayer.next()
    override fun seekToNextWindow() = activePlayer.seekToNextWindow()
    override fun getCurrentManifest(): Any? = activePlayer.currentManifest
    override fun getCurrentWindowIndex(): Int = activePlayer.currentWindowIndex
    override fun getNextWindowIndex(): Int = activePlayer.nextWindowIndex
    override fun getPreviousWindowIndex(): Int = activePlayer.previousWindowIndex
    override fun isCurrentWindowDynamic(): Boolean = activePlayer.isCurrentWindowDynamic
    override fun isCurrentWindowLive(): Boolean = activePlayer.isCurrentWindowLive
    override fun isCurrentWindowSeekable(): Boolean = activePlayer.isCurrentWindowSeekable
    @Deprecated("Use setDeviceVolume with a volume level and C.VIDEO_SCALING_MODE_SCALE_TO_FIT instead.")
    override fun setDeviceVolume(volume: Int) { activePlayer.deviceVolume = volume }
    override fun increaseDeviceVolume() = activePlayer.increaseDeviceVolume()
    override fun decreaseDeviceVolume() = activePlayer.decreaseDeviceVolume()
    @Deprecated("Use setDeviceMuted with a muted state and C.VIDEO_SCALING_MODE_SCALE_TO_FIT instead.")
    override fun setDeviceMuted(muted: Boolean) { activePlayer.isDeviceMuted = muted }
}