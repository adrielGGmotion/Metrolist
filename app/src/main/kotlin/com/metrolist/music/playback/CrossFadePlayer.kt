/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.ShuffleOrder
import com.metrolist.music.constants.CrossfadeMode
import com.metrolist.music.extensions.setOffloadEnabled
import com.metrolist.music.playback.audio.BpmAudioProcessor
import com.metrolist.music.playback.audio.SilenceDetectorAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

data class PlayerContext(
    val player: ExoPlayer,
    val bpmProcessor: BpmAudioProcessor,
    val silenceProcessor: SilenceDetectorAudioProcessor
)

class CrossFadePlayer(
    private val playerFactory: () -> PlayerContext,
    private val scope: CoroutineScope
) : Player {

    private val TAG = "CrossFadePlayer"

    var currentPlayer: ExoPlayer
    private var nextPlayer: ExoPlayer? = null

    // Map to keep track of processors for each active player
    private val playerContexts = mutableMapOf<ExoPlayer, PlayerContext>()

    val crossfadeMode = MutableStateFlow(CrossfadeMode.OFF)
    val crossfadeDurationMs = MutableStateFlow(5000L)

    private var isCrossfading = false
    private var crossfadeJob: Job? = null

    private val listeners = CopyOnWriteArraySet<Player.Listener>()
    private val analyticsListeners = CopyOnWriteArraySet<AnalyticsListener>()

    val audioSessionId: Int
        get() = currentPlayer.audioSessionId

    var skipSilenceEnabled: Boolean
        get() = currentPlayer.skipSilenceEnabled
        set(value) {
            currentPlayer.skipSilenceEnabled = value
            nextPlayer?.skipSilenceEnabled = value
        }

    init {
        val ctx = playerFactory()
        currentPlayer = ctx.player
        playerContexts[currentPlayer] = ctx
        setupPlayerListeners(currentPlayer)
        startMonitoring()
    }

    fun getCurrentSilenceProcessor(): SilenceDetectorAudioProcessor? {
        return playerContexts[currentPlayer]?.silenceProcessor
    }

    fun getBpmProcessor(player: ExoPlayer): BpmAudioProcessor? {
        return playerContexts[player]?.bpmProcessor
    }

    private fun setupPlayerListeners(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                if (p == currentPlayer) {
                     listeners.forEach { it.onEvents(this@CrossFadePlayer, events) }
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                 if (player == currentPlayer) listeners.forEach { it.onPlaybackStateChanged(playbackState) }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                 if (player == currentPlayer) listeners.forEach { it.onIsPlayingChanged(isPlaying) }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                 if (player == currentPlayer) listeners.forEach { it.onMediaItemTransition(mediaItem, reason) }
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (player == currentPlayer) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                        cancelCrossfade()
                    }
                    listeners.forEach { it.onPositionDiscontinuity(oldPosition, newPosition, reason) }
                }
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (player == currentPlayer) listeners.forEach { it.onTimelineChanged(timeline, reason) }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (player == currentPlayer) listeners.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                if (player == currentPlayer) listeners.forEach { it.onRepeatModeChanged(repeatMode) }
            }
            override fun onPlayerError(error: PlaybackException) {
                if (player == currentPlayer) listeners.forEach { it.onPlayerError(error) }
            }
            override fun onPlayerErrorChanged(error: PlaybackException?) {
                if (player == currentPlayer) listeners.forEach { it.onPlayerErrorChanged(error) }
            }
        })
    }

    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                delay(500)
                checkCrossfadeTrigger()
            }
        }
    }

    private fun checkCrossfadeTrigger() {
        if (isCrossfading || !currentPlayer.isPlaying) return
        if (crossfadeMode.value == CrossfadeMode.OFF) return

        val duration = currentPlayer.duration
        val position = currentPlayer.currentPosition

        if (duration == C.TIME_UNSET) return

        val timeRemaining = duration - position
        val triggerTime = crossfadeDurationMs.value

        // Optimize Analysis: Only run when nearing end of song
        val processor = playerContexts[currentPlayer]?.bpmProcessor
        if (processor != null) {
            // Enable analysis only in the last 45 seconds or if crossfade is impending
            processor.isAnalysisEnabled = timeRemaining < 45000
        }

        // Basic trigger
        var shouldTrigger = timeRemaining <= triggerTime && timeRemaining > 0

        // Automix Logic
        if (crossfadeMode.value == CrossfadeMode.AUTOMIX && !shouldTrigger) {
            // Check if we are near end (last 30s) AND energy dropped (outro)
            if (timeRemaining < 30000 && processor != null && processor.isAnalysisEnabled) {
                if (processor.isReadyToMix()) {
                    Log.d(TAG, "Automix: Smart mix point detected (Low Energy/Outro)")
                    shouldTrigger = true
                }
            }
        }

        if (shouldTrigger) {
            startCrossfade()
        }
    }

    private fun startCrossfade() {
        if (isCrossfading) return
        if (!currentPlayer.hasNextMediaItem()) return

        Log.d(TAG, "Starting crossfade...")
        isCrossfading = true

        val ctx = playerFactory()
        val nextP = ctx.player
        playerContexts[nextP] = ctx
        nextPlayer = nextP

        // Transfer state to next player
        nextP.repeatMode = currentPlayer.repeatMode
        nextP.shuffleModeEnabled = currentPlayer.shuffleModeEnabled
        nextP.skipSilenceEnabled = currentPlayer.skipSilenceEnabled

        // Setup queue respecting shuffle order
        val timeline = currentPlayer.currentTimeline
        val currentIdx = currentPlayer.currentMediaItemIndex
        val shuffleModeEnabled = currentPlayer.shuffleModeEnabled
        val repeatMode = currentPlayer.repeatMode

        // Find next index
        val nextIdx = timeline.getNextWindowIndex(currentIdx, repeatMode, shuffleModeEnabled)

        if (nextIdx == C.INDEX_UNSET) {
            isCrossfading = false
            return
        }

        val items = mutableListOf<MediaItem>()

        // Add *current* item as history context (allows seeking back slightly or restarting)
        items.add(timeline.getWindow(currentIdx, Timeline.Window()).mediaItem)

        var idx = nextIdx
        var count = 0
        // Increased limit to prevent queue truncation on standard playlists
        // A truly infinite queue would require a shared MediaSource manager, but 300 covers most sessions
        val maxItemsToQueue = 300

        while (idx != C.INDEX_UNSET && count < maxItemsToQueue) {
            items.add(timeline.getWindow(idx, Timeline.Window()).mediaItem)
            idx = timeline.getNextWindowIndex(idx, repeatMode, shuffleModeEnabled)
            count++
        }

        nextP.setMediaItems(items)
        // Seek to index 1 (the actual next song)
        nextP.seekTo(1, 0)

        nextP.prepare()
        nextP.playWhenReady = true
        nextP.volume = 0f

        // Animation
        crossfadeJob = scope.launch {
            // Automix: Attempt BPM Sync before fading
            if (crossfadeMode.value == CrossfadeMode.AUTOMIX) {
                // Wait briefly for analysis to kick in on next player (it's playing at vol 0)
                delay(2000)

                val currentBpm = playerContexts[currentPlayer]?.bpmProcessor?.getBpm()
                val nextBpm = playerContexts[nextP]?.bpmProcessor?.getBpm()

                if (currentBpm != null && nextBpm != null && currentBpm > 0 && nextBpm > 0) {
                     val ratio = (currentBpm / nextBpm).toFloat()
                     // Limit ratio to avoid extreme pitch shifts (e.g. +/- 20%)
                     if (ratio in 0.8f..1.2f) {
                         nextP.setPlaybackSpeed(ratio)
                         Log.d(TAG, "Automix: Synced BPM $nextBpm -> $currentBpm (Speed: $ratio)")
                     }
                }
            }

            val duration = crossfadeDurationMs.value
            val steps = 20
            val stepTime = duration / steps

            for (i in 1..steps) {
                val progress = i / steps.toFloat()
                currentPlayer.volume = 1f - progress
                nextP.volume = progress
                delay(stepTime)
            }

            swapPlayers(nextP)
        }
    }

    private fun swapPlayers(newPlayer: ExoPlayer) {
        Log.d(TAG, "Swapping players")
        val oldPlayer = currentPlayer

        currentPlayer = newPlayer
        setupPlayerListeners(newPlayer)

        // Re-attach analytics listeners to the new player
        analyticsListeners.forEach {
            oldPlayer.removeAnalyticsListener(it)
            newPlayer.addAnalyticsListener(it)
        }

        // Notify listeners of AudioSessionId change so effects can be re-applied
        val newSessionId = newPlayer.audioSessionId
        listeners.forEach { it.onAudioSessionIdChanged(newSessionId) }

        // Synthesize onEvents for AudioSessionID change because MusicService relies on onEvents, not just the specific callback
        val eventsBuilder = androidx.media3.common.FlagSet.Builder()
        eventsBuilder.add(Player.EVENT_AUDIO_SESSION_ID)
        eventsBuilder.add(Player.EVENT_MEDIA_ITEM_TRANSITION)
        eventsBuilder.add(Player.EVENT_TIMELINE_CHANGED)
        eventsBuilder.add(Player.EVENT_MEDIA_METADATA_CHANGED)

        val events = Player.Events(eventsBuilder.build())

        // Notify listeners of the new state (Metadata, Timeline, MediaItem) immediately
        // This ensures the UI updates to show the "New" song as current, rather than the old one
        listeners.forEach {
            it.onTimelineChanged(newPlayer.currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
            it.onMediaItemTransition(newPlayer.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            it.onEvents(this, events)
        }

        oldPlayer.stop()
        oldPlayer.release()
        playerContexts.remove(oldPlayer)

        nextPlayer = null
        isCrossfading = false
    }

    private fun cancelCrossfade() {
        if (isCrossfading) {
            crossfadeJob?.cancel()
            isCrossfading = false

            nextPlayer?.let {
                it.stop()
                it.release()
                playerContexts.remove(it)
            }
            nextPlayer = null

            currentPlayer.volume = 1f
        }
    }

    // --- Player Implementation ---
    override fun getApplicationLooper(): Looper = currentPlayer.applicationLooper
    override fun addListener(listener: Player.Listener) { listeners.add(listener) }
    override fun removeListener(listener: Player.Listener) { listeners.remove(listener) }
    override fun setMediaItems(mediaItems: List<MediaItem>) = currentPlayer.setMediaItems(mediaItems)
    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) = currentPlayer.setMediaItems(mediaItems, resetPosition)
    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) = currentPlayer.setMediaItems(mediaItems, startIndex, startPositionMs)
    override fun addMediaItems(mediaItems: List<MediaItem>) = currentPlayer.addMediaItems(mediaItems)
    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) = currentPlayer.addMediaItems(index, mediaItems)
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = currentPlayer.moveMediaItem(currentIndex, newIndex)
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) = currentPlayer.moveMediaItems(fromIndex, toIndex, newIndex)
    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) = currentPlayer.replaceMediaItems(fromIndex, toIndex, mediaItems)
    override fun removeMediaItem(index: Int) = currentPlayer.removeMediaItem(index)
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) = currentPlayer.removeMediaItems(fromIndex, toIndex)
    override fun clearMediaItems() = currentPlayer.clearMediaItems()
    override fun isCommandAvailable(command: Int): Boolean = currentPlayer.isCommandAvailable(command)
    override fun canAdvertiseSession(): Boolean = currentPlayer.canAdvertiseSession()
    override fun getAvailableCommands(): Player.Commands = currentPlayer.availableCommands
    override fun prepare() = currentPlayer.prepare()
    override fun getPlaybackState(): Int = currentPlayer.playbackState
    override fun getPlaybackSuppressionReason(): Int = currentPlayer.playbackSuppressionReason
    override fun isPlaying(): Boolean = currentPlayer.isPlaying
    override fun getPlayerError(): PlaybackException? = currentPlayer.playerError
    override fun play() = currentPlayer.play()
    override fun pause() {
        currentPlayer.pause()
        nextPlayer?.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        currentPlayer.playWhenReady = playWhenReady
        nextPlayer?.playWhenReady = playWhenReady
    }
    override fun getPlayWhenReady(): Boolean = currentPlayer.playWhenReady
    override fun setRepeatMode(repeatMode: Int) { currentPlayer.repeatMode = repeatMode }
    override fun getRepeatMode(): Int = currentPlayer.repeatMode
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) { currentPlayer.shuffleModeEnabled = shuffleModeEnabled }
    override fun getShuffleModeEnabled(): Boolean = currentPlayer.shuffleModeEnabled
    override fun isLoading(): Boolean = currentPlayer.isLoading
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        cancelCrossfade()
        currentPlayer.seekTo(mediaItemIndex, positionMs)
    }
    override fun seekTo(positionMs: Long) {
        cancelCrossfade()
        currentPlayer.seekTo(positionMs)
    }
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) { currentPlayer.playbackParameters = playbackParameters }
    override fun getPlaybackParameters(): PlaybackParameters = currentPlayer.playbackParameters
    override fun stop() {
        cancelCrossfade()
        currentPlayer.stop()
    }
    override fun release() {
        cancelCrossfade()
        currentPlayer.release()
    }
    override fun getCurrentTracks(): Tracks = currentPlayer.currentTracks
    override fun getTrackSelectionParameters(): TrackSelectionParameters = currentPlayer.trackSelectionParameters
    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) { currentPlayer.trackSelectionParameters = parameters }
    override fun getMediaMetadata(): MediaMetadata = currentPlayer.mediaMetadata
    override fun getPlaylistMetadata(): MediaMetadata = currentPlayer.playlistMetadata
    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) { currentPlayer.playlistMetadata = mediaMetadata }
    override fun getCurrentTimeline(): Timeline = currentPlayer.currentTimeline
    override fun getCurrentMediaItemIndex(): Int = currentPlayer.currentMediaItemIndex
    override fun getCurrentPeriodIndex(): Int = currentPlayer.currentPeriodIndex
    override fun getCurrentMediaItem(): MediaItem? = currentPlayer.currentMediaItem
    override fun getMediaItemCount(): Int = currentPlayer.mediaItemCount
    override fun getMediaItemAt(index: Int): MediaItem = currentPlayer.getMediaItemAt(index)
    override fun getDuration(): Long = currentPlayer.duration
    override fun getCurrentPosition(): Long = currentPlayer.currentPosition
    override fun getBufferedPosition(): Long = currentPlayer.bufferedPosition
    override fun getTotalBufferedDuration(): Long = currentPlayer.totalBufferedDuration
    override fun isPlayingAd(): Boolean = currentPlayer.isPlayingAd
    override fun getCurrentAdGroupIndex(): Int = currentPlayer.currentAdGroupIndex
    override fun getCurrentAdIndexInAdGroup(): Int = currentPlayer.currentAdIndexInAdGroup
    override fun getContentPosition(): Long = currentPlayer.contentPosition
    override fun getContentBufferedPosition(): Long = currentPlayer.contentBufferedPosition
    override fun getAudioAttributes(): AudioAttributes = currentPlayer.audioAttributes
    override fun setVolume(volume: Float) { currentPlayer.volume = volume }
    override fun getVolume(): Float = currentPlayer.volume
    override fun clearVideoSurface() = currentPlayer.clearVideoSurface()
    override fun clearVideoSurface(surface: Surface?) = currentPlayer.clearVideoSurface(surface)
    override fun setVideoSurface(surface: Surface?) = currentPlayer.setVideoSurface(surface)
    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) = currentPlayer.setVideoSurfaceHolder(surfaceHolder)
    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) = currentPlayer.clearVideoSurfaceHolder(surfaceHolder)
    override fun setVideoSurfaceView(surfaceView: SurfaceView?) = currentPlayer.setVideoSurfaceView(surfaceView)
    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) = currentPlayer.clearVideoSurfaceView(surfaceView)
    override fun setVideoTextureView(textureView: TextureView?) = currentPlayer.setVideoTextureView(textureView)
    override fun clearVideoTextureView(textureView: TextureView?) = currentPlayer.clearVideoTextureView(textureView)
    override fun getVideoSize(): VideoSize = currentPlayer.videoSize
    override fun getSurfaceSize(): Size = currentPlayer.surfaceSize
    override fun getCurrentCues(): CueGroup = currentPlayer.currentCues
    override fun getDeviceInfo(): DeviceInfo = currentPlayer.deviceInfo
    override fun getDeviceVolume(): Int = currentPlayer.deviceVolume
    override fun isDeviceMuted(): Boolean = currentPlayer.isDeviceMuted
    override fun setDeviceVolume(volume: Int) = currentPlayer.setDeviceVolume(volume)
    override fun setDeviceMuted(muted: Boolean) = currentPlayer.setDeviceMuted(muted)
    override fun increaseDeviceVolume() = currentPlayer.increaseDeviceVolume()
    override fun decreaseDeviceVolume() = currentPlayer.decreaseDeviceVolume()
    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) = currentPlayer.setAudioAttributes(audioAttributes, handleAudioFocus)

    // Missing methods implementation
    override fun setDeviceVolume(volume: Int, flags: Int) = currentPlayer.setDeviceVolume(volume, flags)
    override fun setDeviceMuted(muted: Boolean, flags: Int) = currentPlayer.setDeviceMuted(muted, flags)
    override fun increaseDeviceVolume(flags: Int) = currentPlayer.increaseDeviceVolume(flags)
    override fun decreaseDeviceVolume(flags: Int) = currentPlayer.decreaseDeviceVolume(flags)
    override fun setMediaItem(mediaItem: MediaItem) = currentPlayer.setMediaItem(mediaItem)
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) = currentPlayer.setMediaItem(mediaItem, startPositionMs)
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) = currentPlayer.setMediaItem(mediaItem, resetPosition)
    override fun addMediaItem(mediaItem: MediaItem) = currentPlayer.addMediaItem(mediaItem)
    override fun addMediaItem(index: Int, mediaItem: MediaItem) = currentPlayer.addMediaItem(index, mediaItem)
    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) = currentPlayer.replaceMediaItem(index, mediaItem)
    override fun seekToDefaultPosition() = currentPlayer.seekToDefaultPosition()
    override fun seekToDefaultPosition(mediaItemIndex: Int) = currentPlayer.seekToDefaultPosition(mediaItemIndex)
    override fun getSeekBackIncrement(): Long = currentPlayer.seekBackIncrement
    override fun seekBack() = currentPlayer.seekBack()
    override fun getSeekForwardIncrement(): Long = currentPlayer.seekForwardIncrement
    override fun seekForward() = currentPlayer.seekForward()
    override fun hasPreviousMediaItem(): Boolean = currentPlayer.hasPreviousMediaItem()
    override fun seekToPreviousWindow() = currentPlayer.seekToPreviousWindow()
    override fun seekToPreviousMediaItem() = currentPlayer.seekToPreviousMediaItem()
    override fun getMaxSeekToPreviousPosition(): Long = currentPlayer.maxSeekToPreviousPosition
    override fun seekToPrevious() = currentPlayer.seekToPrevious()
    override fun hasNext(): Boolean = currentPlayer.hasNext()
    override fun hasNextWindow(): Boolean = currentPlayer.hasNextWindow()
    override fun hasNextMediaItem(): Boolean = currentPlayer.hasNextMediaItem()
    override fun next() = currentPlayer.next()
    override fun seekToNextWindow() = currentPlayer.seekToNextWindow()
    override fun seekToNextMediaItem() = currentPlayer.seekToNextMediaItem()
    override fun seekToNext() = currentPlayer.seekToNext()
    override fun setPlaybackSpeed(speed: Float) = currentPlayer.setPlaybackSpeed(speed)
    override fun getCurrentManifest(): Any? = currentPlayer.currentManifest
    override fun getCurrentWindowIndex(): Int = currentPlayer.currentWindowIndex
    override fun getNextWindowIndex(): Int = currentPlayer.nextWindowIndex
    override fun getNextMediaItemIndex(): Int = currentPlayer.nextMediaItemIndex
    override fun getPreviousWindowIndex(): Int = currentPlayer.previousWindowIndex
    override fun getPreviousMediaItemIndex(): Int = currentPlayer.previousMediaItemIndex
    override fun getBufferedPercentage(): Int = currentPlayer.bufferedPercentage
    override fun isCurrentWindowDynamic(): Boolean = currentPlayer.isCurrentWindowDynamic
    override fun isCurrentMediaItemDynamic(): Boolean = currentPlayer.isCurrentMediaItemDynamic
    override fun isCurrentWindowLive(): Boolean = currentPlayer.isCurrentWindowLive
    override fun isCurrentMediaItemLive(): Boolean = currentPlayer.isCurrentMediaItemLive
    override fun getCurrentLiveOffset(): Long = currentPlayer.currentLiveOffset
    override fun isCurrentWindowSeekable(): Boolean = currentPlayer.isCurrentWindowSeekable
    override fun isCurrentMediaItemSeekable(): Boolean = currentPlayer.isCurrentMediaItemSeekable
    override fun getContentDuration(): Long = currentPlayer.contentDuration

    // Exposed ExoPlayer methods
    fun addAnalyticsListener(listener: AnalyticsListener) {
        analyticsListeners.add(listener)
        currentPlayer.addAnalyticsListener(listener)
    }
    fun removeAnalyticsListener(listener: AnalyticsListener) {
        analyticsListeners.remove(listener)
        currentPlayer.removeAnalyticsListener(listener)
    }
    fun setHandleAudioBecomingNoisy(handleAudioBecomingNoisy: Boolean) = currentPlayer.setHandleAudioBecomingNoisy(handleAudioBecomingNoisy)
    fun setWakeMode(wakeMode: Int) = currentPlayer.setWakeMode(wakeMode)
    fun setOffloadEnabled(offload: Boolean) = currentPlayer.setOffloadEnabled(offload)
    fun setShuffleOrder(shuffleOrder: ShuffleOrder) = currentPlayer.setShuffleOrder(shuffleOrder)

    // Helper to access internal state if needed
    fun getCurrentExoPlayer(): ExoPlayer = currentPlayer
}
