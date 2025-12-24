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
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.RenderersFactory
import com.metrolist.music.extensions.setOffloadEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

class CrossfadeManager(
    context: Context,
    mediaSourceFactory: MediaSource.Factory,
    renderersFactory: RenderersFactory
) : Player {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var crossfadeJob: Job? = null
    var crossfadeDuration: Int = 0
    var isCrossfading = false

    private val listeners = CopyOnWriteArrayList<Player.Listener>()
    private var analyticsListener: AnalyticsListener? = null

    private val internalListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player === currentPlayer) {
                listeners.forEach {
                    it.onEvents(this@CrossfadeManager, events)
                }
            }
        }
    }

    private val player1: ExoPlayer
    private val player2: ExoPlayer
    private var currentPlayer: ExoPlayer

    init {
        val playerBuilder = {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setRenderersFactory(renderersFactory)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false,
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
        }
        player1 = playerBuilder().build()
        player2 = playerBuilder().build()
        currentPlayer = player1

        player1.addListener(internalListener)
        player2.addListener(internalListener)
    }

    // Custom methods & Overrides with custom logic
    fun setAnalyticsListener(listener: AnalyticsListener) {
        this.analyticsListener = listener
        currentPlayer.addAnalyticsListener(listener)
    }

    fun clearAnalyticsListener() {
        analyticsListener?.let {
            player1.removeAnalyticsListener(it)
            player2.removeAnalyticsListener(it)
        }
        this.analyticsListener = null
    }

    var skipSilenceEnabled: Boolean
        get() = currentPlayer.skipSilenceEnabled
        set(value) {
            player1.skipSilenceEnabled = value
            player2.skipSilenceEnabled = value
        }

    fun setShuffleOrder(shuffleOrder: ShuffleOrder) {
        player1.setShuffleOrder(shuffleOrder)
        player2.setShuffleOrder(shuffleOrder)
    }

    fun setOffloadEnabled(enabled: Boolean) {
        player1.setOffloadEnabled(enabled)
        player2.setOffloadEnabled(enabled)
    }

    val audioSessionId: Int
        get() = currentPlayer.audioSessionId

    override fun addListener(listener: Player.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)
    }

    override fun play() {
        crossfadeJob?.cancel()
        currentPlayer.play()
        if (crossfadeDuration > 0) {
            crossfadeJob = scope.launch { startCrossfade() }
        }
    }

    override fun pause() {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.pause()
        }
        currentPlayer.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            play()
        } else {
            pause()
        }
    }

    override fun stop() {
        crossfadeJob?.cancel()
        player1.stop()
        player2.stop()
    }

    override fun release() {
        crossfadeJob?.cancel()
        listeners.clear()
        player1.release()
        player2.release()
    }

    // Playlist manipulation methods (apply to both players)
    override fun setMediaItems(mediaItems: List<MediaItem>) {
        val mutableMediaItems = mediaItems.toMutableList()
        player1.setMediaItems(mutableMediaItems)
        player2.setMediaItems(mutableMediaItems)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        val mutableMediaItems = mediaItems.toMutableList()
        player1.setMediaItems(mutableMediaItems, resetPosition)
        player2.setMediaItems(mutableMediaItems, resetPosition)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        val mutableMediaItems = mediaItems.toMutableList()
        player1.setMediaItems(mutableMediaItems, startIndex, startPositionMs)
        player2.setMediaItems(mutableMediaItems, startIndex, startPositionMs)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        player1.addMediaItem(mediaItem)
        player2.addMediaItem(mediaItem)
    }

    override fun addMediaItems(mediaItems: List<MediaItem>) {
        val mutableMediaItems = mediaItems.toMutableList()
        player1.addMediaItems(mutableMediaItems)
        player2.addMediaItems(mutableMediaItems)
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        player1.addMediaItem(index, mediaItem)
        player2.addMediaItem(index, mediaItem)
    }

    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
        val mutableMediaItems = mediaItems.toMutableList()
        player1.addMediaItems(index, mutableMediaItems)
        player2.addMediaItems(index, mutableMediaItems)
    }

    override fun removeMediaItem(index: Int) {
        player1.removeMediaItem(index)
        player2.removeMediaItem(index)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        player1.removeMediaItems(fromIndex, toIndex)
        player2.removeMediaItems(fromIndex, toIndex)
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        player1.moveMediaItem(currentIndex, newIndex)
        player2.moveMediaItem(currentIndex, newIndex)
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        player1.moveMediaItems(fromIndex, toIndex, newIndex)
        player2.moveMediaItems(fromIndex, toIndex, newIndex)
    }

    override fun clearMediaItems() {
        player1.clearMediaItems()
        player2.clearMediaItems()
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        player1.replaceMediaItem(index, mediaItem)
        player2.replaceMediaItem(index, mediaItem)
    }

    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) {
        val mutableMediaItems = mediaItems.toMutableList()
        player1.replaceMediaItems(fromIndex, toIndex, mutableMediaItems)
        player2.replaceMediaItems(fromIndex, toIndex, mutableMediaItems)
    }

    // Pure delegation to currentPlayer
    override fun getApplicationLooper(): Looper = currentPlayer.applicationLooper
    override fun setMediaItem(mediaItem: MediaItem) = currentPlayer.setMediaItem(mediaItem)
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) = currentPlayer.setMediaItem(mediaItem, startPositionMs)
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) = currentPlayer.setMediaItem(mediaItem, resetPosition)
    override fun isCommandAvailable(command: Int): Boolean = currentPlayer.isCommandAvailable(command)
    override fun canAdvertiseSession(): Boolean = currentPlayer.canAdvertiseSession()
    override fun prepare() = currentPlayer.prepare()
    override fun getPlaybackState(): Int = currentPlayer.playbackState
    override fun getPlaybackSuppressionReason(): Int = currentPlayer.playbackSuppressionReason
    override fun isPlaying(): Boolean = currentPlayer.isPlaying
    override fun getPlayerError(): androidx.media3.common.PlaybackException? = currentPlayer.playerError
    override fun getPlayWhenReady(): Boolean = currentPlayer.playWhenReady
    override fun getRepeatMode(): Int = currentPlayer.repeatMode
    override fun setRepeatMode(repeatMode: Int) {
        player1.repeatMode = repeatMode
        player2.repeatMode = repeatMode
    }
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        player1.shuffleModeEnabled = shuffleModeEnabled
        player2.shuffleModeEnabled = shuffleModeEnabled
    }
    override fun getShuffleModeEnabled(): Boolean = currentPlayer.shuffleModeEnabled
    override fun isLoading(): Boolean = currentPlayer.isLoading
    override fun seekToDefaultPosition() {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekToDefaultPosition()
    }
    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekToDefaultPosition(mediaItemIndex)
    }
    override fun seekTo(positionMs: Long) {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekTo(positionMs)
    }
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekTo(mediaItemIndex, positionMs)
    }
    override fun getSeekBackIncrement(): Long = currentPlayer.seekBackIncrement
    override fun seekBack() {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekBack()
    }
    override fun getSeekForwardIncrement(): Long = currentPlayer.seekForwardIncrement
    override fun seekForward() {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekForward()
    }
    override fun hasPreviousMediaItem(): Boolean = currentPlayer.hasPreviousMediaItem()
    override fun seekToPreviousMediaItem() {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekToPreviousMediaItem()
    }
    override fun getMaxSeekToPreviousPosition(): Long = currentPlayer.maxSeekToPreviousPosition
    override fun seekToPrevious() {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekToPrevious()
    }
    override fun hasNextMediaItem(): Boolean = currentPlayer.hasNextMediaItem()
    override fun seekToNextMediaItem() {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekToNextMediaItem()
    }
    override fun seekToNext() {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            val nextPlayer = if (currentPlayer === player1) player2 else player1
            nextPlayer.stop()
        }
        currentPlayer.seekToNext()
    }
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        currentPlayer.playbackParameters = playbackParameters
    }
    override fun setPlaybackSpeed(speed: Float) {
        currentPlayer.setPlaybackSpeed(speed)
    }
    override fun getPlaybackParameters(): PlaybackParameters = currentPlayer.playbackParameters
    override fun getCurrentTracks(): androidx.media3.common.Tracks = currentPlayer.currentTracks
    override fun getTrackSelectionParameters(): TrackSelectionParameters = currentPlayer.trackSelectionParameters
    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        currentPlayer.trackSelectionParameters = parameters
    }
    override fun getMediaMetadata(): MediaMetadata = currentPlayer.mediaMetadata
    override fun getPlaylistMetadata(): MediaMetadata = currentPlayer.playlistMetadata
    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        currentPlayer.playlistMetadata = mediaMetadata
    }
    override fun getCurrentManifest(): Any? = currentPlayer.currentManifest
    override fun getCurrentTimeline(): Timeline = currentPlayer.currentTimeline
    override fun getCurrentPeriodIndex(): Int = currentPlayer.currentPeriodIndex
    override fun getCurrentMediaItemIndex(): Int = currentPlayer.currentMediaItemIndex
    override fun getNextMediaItemIndex(): Int = currentPlayer.nextMediaItemIndex
    override fun getPreviousMediaItemIndex(): Int = currentPlayer.previousMediaItemIndex
    override fun getCurrentMediaItem(): MediaItem? = currentPlayer.currentMediaItem
    override fun getMediaItemCount(): Int = currentPlayer.mediaItemCount
    override fun getMediaItemAt(index: Int): MediaItem = currentPlayer.getMediaItemAt(index)
    override fun getDuration(): Long = currentPlayer.duration
    override fun getCurrentPosition(): Long = currentPlayer.currentPosition
    override fun getBufferedPosition(): Long = currentPlayer.bufferedPosition
    override fun getBufferedPercentage(): Int = currentPlayer.bufferedPercentage
    override fun getTotalBufferedDuration(): Long = currentPlayer.totalBufferedDuration
    override fun isCurrentMediaItemDynamic(): Boolean = currentPlayer.isCurrentMediaItemDynamic
    override fun isCurrentMediaItemLive(): Boolean = currentPlayer.isCurrentMediaItemLive
    override fun getCurrentLiveOffset(): Long = currentPlayer.currentLiveOffset
    override fun isCurrentMediaItemSeekable(): Boolean = currentPlayer.isCurrentMediaItemSeekable
    override fun isPlayingAd(): Boolean = currentPlayer.isPlayingAd
    override fun getCurrentAdGroupIndex(): Int = currentPlayer.currentAdGroupIndex
    override fun getCurrentAdIndexInAdGroup(): Int = currentPlayer.currentAdIndexInAdGroup
    override fun getContentDuration(): Long = currentPlayer.contentDuration
    override fun getContentPosition(): Long = currentPlayer.contentPosition
    override fun getContentBufferedPosition(): Long = currentPlayer.contentBufferedPosition
    override fun getAudioAttributes(): AudioAttributes = currentPlayer.audioAttributes
    override fun setVolume(volume: Float) {
        currentPlayer.volume = volume
    }
    override fun getVolume(): Float = currentPlayer.volume
    override fun clearVideoSurface() = currentPlayer.clearVideoSurface()
    override fun clearVideoSurface(surface: android.view.Surface?) = currentPlayer.clearVideoSurface(surface)
    override fun setVideoSurface(surface: android.view.Surface?) = currentPlayer.setVideoSurface(surface)
    override fun setVideoSurfaceHolder(surfaceHolder: android.view.SurfaceHolder?) = currentPlayer.setVideoSurfaceHolder(surfaceHolder)
    override fun clearVideoSurfaceHolder(surfaceHolder: android.view.SurfaceHolder?) = currentPlayer.clearVideoSurfaceHolder(surfaceHolder)
    override fun setVideoSurfaceView(surfaceView: android.view.SurfaceView?) = currentPlayer.setVideoSurfaceView(surfaceView)
    override fun clearVideoSurfaceView(surfaceView: android.view.SurfaceView?) = currentPlayer.clearVideoSurfaceView(surfaceView)
    override fun setVideoTextureView(textureView: android.view.TextureView?) = currentPlayer.setVideoTextureView(textureView)
    override fun clearVideoTextureView(textureView: android.view.TextureView?) = currentPlayer.clearVideoTextureView(textureView)
    override fun getVideoSize(): VideoSize = currentPlayer.videoSize
    override fun getSurfaceSize(): androidx.media3.common.util.Size = currentPlayer.surfaceSize
    override fun getCurrentCues(): CueGroup = currentPlayer.currentCues
    override fun getDeviceInfo(): DeviceInfo = currentPlayer.deviceInfo
    override fun getDeviceVolume(): Int = currentPlayer.deviceVolume
    override fun isDeviceMuted(): Boolean = currentPlayer.isDeviceMuted
    override fun setDeviceVolume(volume: Int) {
        currentPlayer.deviceVolume = volume
    }
    override fun increaseDeviceVolume() = currentPlayer.increaseDeviceVolume()
    override fun decreaseDeviceVolume() = currentPlayer.decreaseDeviceVolume()
    override fun setDeviceMuted(muted: Boolean) {
        currentPlayer.isDeviceMuted = muted
    }
    override fun getAvailableCommands(): Player.Commands = currentPlayer.availableCommands
    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) = currentPlayer.setAudioAttributes(audioAttributes, handleAudioFocus)
    override fun setDeviceVolume(volume: Int, flags: Int) = currentPlayer.setDeviceVolume(volume, flags)
    override fun increaseDeviceVolume(flags: Int) = currentPlayer.increaseDeviceVolume(flags)
    override fun decreaseDeviceVolume(flags: Int) = currentPlayer.decreaseDeviceVolume(flags)
    override fun setDeviceMuted(muted: Boolean, flags: Int) = currentPlayer.setDeviceMuted(muted, flags)

    // Deprecated
    @Deprecated("Use seekToPreviousMediaItem instead")
    override fun seekToPreviousWindow() = currentPlayer.seekToPreviousWindow()
    @Deprecated("Use hasNextMediaItem instead")
    override fun hasNext(): Boolean = currentPlayer.hasNext()
    @Deprecated("Use hasNextMediaItem instead")
    override fun hasNextWindow(): Boolean = currentPlayer.hasNextWindow()
    @Deprecated("Use seekToNextMediaItem instead")
    override fun next() = currentPlayer.next()
    @Deprecated("Use seekToNextMediaItem instead")
    override fun seekToNextWindow() = currentPlayer.seekToNextWindow()
    @Deprecated("Use currentMediaItemIndex instead")
    override fun getCurrentWindowIndex(): Int = currentPlayer.currentWindowIndex
    @Deprecated("Use nextMediaItemIndex instead")
    override fun getNextWindowIndex(): Int = currentPlayer.nextWindowIndex
    @Deprecated("Use previousMediaItemIndex instead")
    override fun getPreviousWindowIndex(): Int = currentPlayer.previousWindowIndex
    @Deprecated("Use isCurrentMediaItemDynamic instead")
    override fun isCurrentWindowDynamic(): Boolean = currentPlayer.isCurrentWindowDynamic
    @Deprecated("Use isCurrentMediaItemLive instead")
    override fun isCurrentWindowLive(): Boolean = currentPlayer.isCurrentWindowLive
    @Deprecated("Use isCurrentMediaItemSeekable instead")
    override fun isCurrentWindowSeekable(): Boolean = currentPlayer.isCurrentWindowSeekable

    private suspend fun startCrossfade() {
        while (scope.isActive) {
            val duration = currentPlayer.duration
            if (duration <= 0) {
                delay(1000) // Check again in a second
                continue
            }

            val position = currentPlayer.currentPosition
            val delayMs = duration - position - (crossfadeDuration * 1000)
            if (delayMs > 0) {
                delay(delayMs)
            }

            if (!isCrossfading && scope.isActive) {
                val nextPlayer = if (currentPlayer === player1) player2 else player1
                if (currentPlayer.nextMediaItemIndex != C.INDEX_UNSET) {
                    // Deduplication Check
                    val currentItem = currentPlayer.currentMediaItem
                    val nextItem = currentPlayer.getMediaItemAt(currentPlayer.nextMediaItemIndex)
                    if (currentItem?.mediaId == nextItem.mediaId && repeatMode != Player.REPEAT_MODE_ONE) {
                        delay(1000) // Wait and re-evaluate
                        continue
                    }

                    try {
                        isCrossfading = true
                        nextPlayer.volume = 0f
                        nextPlayer.seekTo(currentPlayer.nextMediaItemIndex, 0)
                        nextPlayer.prepare()
                        nextPlayer.play()

                        val steps = 20
                        val stepDuration = (crossfadeDuration * 1000) / steps
                        for (i in 1..steps) {
                            val volume = 1f - (i.toFloat() / steps)
                            currentPlayer.volume = max(0f, volume)
                            nextPlayer.volume = max(0f, 1f - volume)
                            delay(stepDuration.toLong())
                        }

                        analyticsListener?.let {
                            currentPlayer.removeAnalyticsListener(it)
                        }
                        currentPlayer.stop()
                        currentPlayer.clearMediaItems()
                        currentPlayer.volume = 1f
                        currentPlayer = nextPlayer
                        analyticsListener?.let {
                            currentPlayer.addAnalyticsListener(it)
                        }
                        listeners.forEach {
                            it.onTimelineChanged(currentPlayer.currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
                            it.onMediaMetadataChanged(currentPlayer.mediaMetadata)
                            it.onIsPlayingChanged(true)
                        }
                    } finally {
                        isCrossfading = false
                    }
                }
            }
            // Wait a bit before checking the new track
            delay(1000)
        }
    }
}
