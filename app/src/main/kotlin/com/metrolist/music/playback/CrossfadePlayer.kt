package com.metrolist.music.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.PriorityTaskManager
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Clock
import androidx.media3.common.util.Size
import androidx.media3.common.Effect
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.exoplayer.trackselection.TrackSelectionArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsCollector
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.image.ImageOutput
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.exoplayer.video.spherical.CameraMotionListener
import com.metrolist.music.utils.BpmAnalyzer
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs

@OptIn(UnstableApi::class)
class CrossfadePlayer(
    private val context: Context,
    private val playerFactory: () -> ExoPlayer
) : ExoPlayer {

    private var currentPlayer: ExoPlayer = playerFactory()
    private var nextPlayer: ExoPlayer? = null
    private var fadingPlayer: ExoPlayer? = null

    // Configuration
    var crossfadeEnabled: Boolean = false
    var automixEnabled: Boolean = false
    var crossfadeDurationMs: Long = 4000
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val listeners = CopyOnWriteArraySet<Listener>()
    private val analyticsListeners = CopyOnWriteArraySet<AnalyticsListener>()

    private var checkProgressJob: Job? = null
    private var isCrossfading = false
    private var isReleased = false

    private val forwardingListener = object : Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            listeners.forEach { it.onTimelineChanged(timeline, reason) }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            listeners.forEach { it.onMediaItemTransition(mediaItem, reason) }
        }
        override fun onTracksChanged(tracks: Tracks) {
            listeners.forEach { it.onTracksChanged(tracks) }
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            listeners.forEach { it.onMediaMetadataChanged(mediaMetadata) }
        }
        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
            listeners.forEach { it.onPlaylistMetadataChanged(mediaMetadata) }
        }
        override fun onIsLoadingChanged(isLoading: Boolean) {
            listeners.forEach { it.onIsLoadingChanged(isLoading) }
        }
        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            listeners.forEach { it.onAvailableCommandsChanged(availableCommands) }
        }
        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
            listeners.forEach { it.onTrackSelectionParametersChanged(parameters) }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            listeners.forEach { it.onPlaybackStateChanged(playbackState) }
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            listeners.forEach { it.onPlayWhenReadyChanged(playWhenReady, reason) }
        }
        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            listeners.forEach { it.onPlaybackSuppressionReasonChanged(playbackSuppressionReason) }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listeners.forEach { it.onIsPlayingChanged(isPlaying) }
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            listeners.forEach { it.onRepeatModeChanged(repeatMode) }
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            listeners.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
        }
        override fun onPlayerError(error: PlaybackException) {
            listeners.forEach { it.onPlayerError(error) }
        }
        override fun onPlayerErrorChanged(error: PlaybackException?) {
            listeners.forEach { it.onPlayerErrorChanged(error) }
        }
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            listeners.forEach { it.onPositionDiscontinuity(oldPosition, newPosition, reason) }
        }
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            listeners.forEach { it.onPlaybackParametersChanged(playbackParameters) }
        }
        override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
            listeners.forEach { it.onSeekBackIncrementChanged(seekBackIncrementMs) }
        }
        override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
            listeners.forEach { it.onSeekForwardIncrementChanged(seekForwardIncrementMs) }
        }
        override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) {
            listeners.forEach { it.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs) }
        }
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            listeners.forEach { it.onAudioSessionIdChanged(audioSessionId) }
        }
        override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
            listeners.forEach { it.onAudioAttributesChanged(audioAttributes) }
        }
        override fun onVolumeChanged(volume: Float) {
            listeners.forEach { it.onVolumeChanged(volume) }
        }
        override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
            listeners.forEach { it.onSkipSilenceEnabledChanged(skipSilenceEnabled) }
        }
        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            listeners.forEach { it.onDeviceInfoChanged(deviceInfo) }
        }
        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
            listeners.forEach { it.onDeviceVolumeChanged(volume, muted) }
        }
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            listeners.forEach { it.onVideoSizeChanged(videoSize) }
        }
        override fun onSurfaceSizeChanged(width: Int, height: Int) {
            listeners.forEach { it.onSurfaceSizeChanged(width, height) }
        }
        override fun onRenderedFirstFrame() {
            listeners.forEach { it.onRenderedFirstFrame() }
        }
        override fun onCues(cueGroup: CueGroup) {
            listeners.forEach { it.onCues(cueGroup) }
        }
        override fun onMetadata(metadata: androidx.media3.common.Metadata) {
            listeners.forEach { it.onMetadata(metadata) }
        }
    }
    
    init {
        currentPlayer.addListener(forwardingListener)
        startProgressCheck()
    }

    private fun startProgressCheck() {
        checkProgressJob?.cancel()
        checkProgressJob = scope.launch {
            while (isActive) {
                checkCrossfade()
                delay(500)
            }
        }
    }

    private fun checkCrossfade() {
        if (!crossfadeEnabled && !automixEnabled) return
        if (isCrossfading || !currentPlayer.isPlaying) return

        val duration = currentPlayer.duration
        val position = currentPlayer.currentPosition
        
        if (duration != androidx.media3.common.C.TIME_UNSET && duration > 0) {
            val timeRemaining = duration - position
            // Trigger slightly earlier to allow buffer
            if (timeRemaining <= crossfadeDurationMs && timeRemaining > 0) {
                // Check if we have a next item
                if (currentPlayer.hasNextMediaItem()) {
                    performCrossfade()
                }
            }
        }
    }

    private fun performCrossfade() {
        if (isCrossfading) return
        isCrossfading = true
        
        // 1. Create Next Player
        val newPlayer = playerFactory()
        
        // 2. Setup Playlist for Next Player
        // We copy from next index onwards
        val timeline = currentPlayer.currentTimeline
        val index = currentPlayer.currentMediaItemIndex
        val nextIndex = index + 1
        
        if (nextIndex < timeline.windowCount) {
             val items = mutableListOf<MediaItem>()
             for (i in nextIndex until timeline.windowCount) {
                 items.add(timeline.getWindow(i, Timeline.Window()).mediaItem)
             }
             newPlayer.setMediaItems(items)
             newPlayer.prepare()
             
             // Sync state
             newPlayer.volume = 0f
             newPlayer.repeatMode = currentPlayer.repeatMode
             newPlayer.shuffleModeEnabled = currentPlayer.shuffleModeEnabled
             
             // Automix Logic (BPM)
             if (automixEnabled) {
                 scope.launch {
                     // Note: Ideally we analyze the files. 
                     // For now, we simulate a speed match if we had BPMs.
                     // In a real implementation:
                     // val bpm1 = BpmAnalyzer.analyze(currentFile)
                     // val bpm2 = BpmAnalyzer.analyze(nextFile)
                     // if (bpm1 != null && bpm2 != null) newPlayer.setPlaybackSpeed(bpm1/bpm2)
                 }
             }

             newPlayer.play()
             
             fadingPlayer = currentPlayer
             nextPlayer = newPlayer
             
             // Start fade animation
             scope.launch {
                 val fadeSteps = 20
                 val stepDuration = crossfadeDurationMs / fadeSteps
                 val volumeStep = 1.0f / fadeSteps
                 
                 for (i in 1..fadeSteps) {
                     val vol = i * volumeStep
                     nextPlayer?.volume = vol
                     fadingPlayer?.volume = 1.0f - vol
                     delay(stepDuration)
                 }
                 
                 finishCrossfade()
             }
        } else {
            isCrossfading = false
        }
    }
    
    private fun finishCrossfade() {
        // Swap
        fadingPlayer?.pause()
        fadingPlayer?.removeListener(forwardingListener)
        fadingPlayer?.release()
        fadingPlayer = null
        
        // The new player is now the current player
        val newCurrent = nextPlayer ?: return
        currentPlayer = newCurrent
        nextPlayer = null
        
        currentPlayer.volume = 1.0f
        currentPlayer.addListener(forwardingListener)
        
        // We need to re-attach analytics listeners to the new player
        analyticsListeners.forEach { currentPlayer.addAnalyticsListener(it) }
        
        isCrossfading = false
    }

    // --- ExoPlayer Implementation ---

    override fun addListener(listener: Listener) {
        listeners.add(listener)
        // We forward events manually, so we don't add to currentPlayer
    }

    override fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    // Delegates to currentPlayer
    override fun getApplicationLooper(): Looper = currentPlayer.applicationLooper
    override fun getPlayerError(): androidx.media3.exoplayer.ExoPlaybackException? = currentPlayer.playerError
    override fun play() = currentPlayer.play()
    override fun pause() = currentPlayer.pause()
    override fun setPlayWhenReady(playWhenReady: Boolean) { currentPlayer.playWhenReady = playWhenReady }
    override fun getPlayWhenReady(): Boolean = currentPlayer.playWhenReady
    override fun setRepeatMode(repeatMode: Int) { currentPlayer.repeatMode = repeatMode; nextPlayer?.repeatMode = repeatMode }
    override fun getRepeatMode(): Int = currentPlayer.repeatMode
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) { currentPlayer.shuffleModeEnabled = shuffleModeEnabled; nextPlayer?.shuffleModeEnabled = shuffleModeEnabled }
    override fun getShuffleModeEnabled(): Boolean = currentPlayer.shuffleModeEnabled
    override fun isLoading(): Boolean = currentPlayer.isLoading
    override fun seekToDefaultPosition() = currentPlayer.seekToDefaultPosition()
    override fun seekToDefaultPosition(mediaItemIndex: Int) = currentPlayer.seekToDefaultPosition(mediaItemIndex)
    override fun seekTo(positionMs: Long) = currentPlayer.seekTo(positionMs)
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) = currentPlayer.seekTo(mediaItemIndex, positionMs)
    override fun getSeekBackIncrement(): Long = currentPlayer.seekBackIncrement
    override fun getSeekForwardIncrement(): Long = currentPlayer.seekForwardIncrement
    override fun getMaxSeekToPreviousPosition(): Long = currentPlayer.maxSeekToPreviousPosition
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) { currentPlayer.playbackParameters = playbackParameters }
    override fun getPlaybackParameters(): PlaybackParameters = currentPlayer.playbackParameters
    override fun stop() { currentPlayer.stop(); nextPlayer?.stop() }
    override fun release() { 
        isReleased = true
        currentPlayer.release()
        nextPlayer?.release()
        scope.cancel() 
    }
    override fun isReleased(): Boolean = isReleased

    override fun getCurrentTracks(): Tracks = currentPlayer.currentTracks
    override fun getTrackSelectionParameters(): TrackSelectionParameters = currentPlayer.trackSelectionParameters
    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) { currentPlayer.trackSelectionParameters = parameters }
    override fun getMediaMetadata(): MediaMetadata = currentPlayer.mediaMetadata
    override fun getPlaylistMetadata(): MediaMetadata = currentPlayer.playlistMetadata
    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) { currentPlayer.playlistMetadata = mediaMetadata }
    override fun getCurrentTimeline(): Timeline = currentPlayer.currentTimeline
    override fun getCurrentMediaItemIndex(): Int = currentPlayer.currentMediaItemIndex
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
    override fun setVideoSurface(surface: Surface?) { currentPlayer.setVideoSurface(surface) }
    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) { currentPlayer.setVideoSurfaceHolder(surfaceHolder) }
    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) { currentPlayer.clearVideoSurfaceHolder(surfaceHolder) }
    override fun setVideoSurfaceView(surfaceView: SurfaceView?) { currentPlayer.setVideoSurfaceView(surfaceView) }
    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) { currentPlayer.clearVideoSurfaceView(surfaceView) }
    override fun setVideoTextureView(textureView: TextureView?) { currentPlayer.setVideoTextureView(textureView) }
    override fun clearVideoTextureView(textureView: TextureView?) { currentPlayer.clearVideoTextureView(textureView) }
    override fun getVideoSize(): VideoSize = currentPlayer.videoSize
    override fun getSurfaceSize(): Size = currentPlayer.surfaceSize
    override fun getCurrentCues(): CueGroup = currentPlayer.currentCues
    override fun getDeviceInfo(): DeviceInfo = currentPlayer.deviceInfo
    override fun getDeviceVolume(): Int = currentPlayer.deviceVolume
    override fun isDeviceMuted(): Boolean = currentPlayer.isDeviceMuted
    override fun setDeviceVolume(volume: Int) { currentPlayer.deviceVolume = volume }
    override fun setDeviceMuted(muted: Boolean) { currentPlayer.isDeviceMuted = muted }
    override fun setDeviceVolume(volume: Int, flags: Int) { currentPlayer.setDeviceVolume(volume, flags) }
    override fun setDeviceMuted(muted: Boolean, flags: Int) { currentPlayer.setDeviceMuted(muted, flags) }
    override fun increaseDeviceVolume() = currentPlayer.increaseDeviceVolume()
    override fun decreaseDeviceVolume() = currentPlayer.decreaseDeviceVolume()
    
    // ExoPlayer Specifics
    override fun getAudioFormat(): androidx.media3.common.Format? = currentPlayer.audioFormat
    override fun getVideoFormat(): androidx.media3.common.Format? = currentPlayer.videoFormat
    override fun getAudioDecoderCounters(): androidx.media3.exoplayer.DecoderCounters? = currentPlayer.audioDecoderCounters
    override fun getVideoDecoderCounters(): androidx.media3.exoplayer.DecoderCounters? = currentPlayer.videoDecoderCounters
    override fun setVideoScalingMode(videoScalingMode: Int) { currentPlayer.videoScalingMode = videoScalingMode }
    override fun getVideoScalingMode(): Int = currentPlayer.videoScalingMode
    override fun setVideoChangeFrameRateStrategy(videoChangeFrameRateStrategy: Int) { currentPlayer.videoChangeFrameRateStrategy = videoChangeFrameRateStrategy }
    override fun getVideoChangeFrameRateStrategy(): Int = currentPlayer.videoChangeFrameRateStrategy
    override fun setHandleAudioBecomingNoisy(handleAudioBecomingNoisy: Boolean) { currentPlayer.setHandleAudioBecomingNoisy(handleAudioBecomingNoisy) }
    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) { currentPlayer.skipSilenceEnabled = skipSilenceEnabled }
    override fun getSkipSilenceEnabled(): Boolean = currentPlayer.skipSilenceEnabled
    override fun setAudioSessionId(audioSessionId: Int) { currentPlayer.audioSessionId = audioSessionId }
    override fun getAudioSessionId(): Int = currentPlayer.audioSessionId
    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) { currentPlayer.setAuxEffectInfo(auxEffectInfo) }
    override fun clearAuxEffectInfo() { currentPlayer.clearAuxEffectInfo() }
    override fun setPreferredAudioDevice(audioDeviceInfo: android.media.AudioDeviceInfo?) { currentPlayer.setPreferredAudioDevice(audioDeviceInfo) }
    override fun setCameraMotionListener(listener: CameraMotionListener) { currentPlayer.setCameraMotionListener(listener) }
    override fun clearCameraMotionListener(listener: CameraMotionListener) { currentPlayer.clearCameraMotionListener(listener) }
    override fun setVideoFrameMetadataListener(listener: VideoFrameMetadataListener) { currentPlayer.setVideoFrameMetadataListener(listener) }
    override fun clearVideoFrameMetadataListener(listener: VideoFrameMetadataListener) { currentPlayer.clearVideoFrameMetadataListener(listener) }
    override fun getClock(): Clock = currentPlayer.clock
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) { currentPlayer.setMediaItems(mediaItems, resetPosition) }
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) { currentPlayer.setMediaItems(mediaItems, startIndex, startPositionMs) }
    override fun setMediaItems(mediaItems: MutableList<MediaItem>) { currentPlayer.setMediaItems(mediaItems) }
    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) { currentPlayer.addMediaItems(index, mediaItems); nextPlayer?.addMediaItems(index, mediaItems) }
    override fun addMediaItems(mediaItems: MutableList<MediaItem>) { currentPlayer.addMediaItems(mediaItems); nextPlayer?.addMediaItems(mediaItems) }
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) { currentPlayer.moveMediaItem(currentIndex, newIndex); nextPlayer?.moveMediaItem(currentIndex, newIndex) }
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) { currentPlayer.moveMediaItems(fromIndex, toIndex, newIndex); nextPlayer?.moveMediaItems(fromIndex, toIndex, newIndex) }
    override fun removeMediaItem(index: Int) { currentPlayer.removeMediaItem(index); nextPlayer?.removeMediaItem(index) }
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) { currentPlayer.removeMediaItems(fromIndex, toIndex); nextPlayer?.removeMediaItems(fromIndex, toIndex) }
    override fun clearMediaItems() { currentPlayer.clearMediaItems(); nextPlayer?.clearMediaItems() }
    override fun isCommandAvailable(command: Int): Boolean = currentPlayer.isCommandAvailable(command)
    override fun canAdvertiseSession(): Boolean = currentPlayer.canAdvertiseSession()
    override fun getAvailableCommands(): Player.Commands = currentPlayer.availableCommands
    override fun prepare() = currentPlayer.prepare()
    override fun getPlaybackState(): Int = currentPlayer.playbackState
    override fun getPlaybackSuppressionReason(): Int = currentPlayer.playbackSuppressionReason
    // override fun getPlayerError(): PlaybackException? = currentPlayer.playerError // Already implemented above
    override fun addAnalyticsListener(listener: AnalyticsListener) {
        analyticsListeners.add(listener)
        currentPlayer.addAnalyticsListener(listener)
    }
    override fun removeAnalyticsListener(listener: AnalyticsListener) {
        analyticsListeners.remove(listener)
        currentPlayer.removeAnalyticsListener(listener)
    }
    override fun getAnalyticsCollector(): AnalyticsCollector = currentPlayer.analyticsCollector
    override fun setSeekParameters(seekParameters: SeekParameters?) { currentPlayer.setSeekParameters(seekParameters) }
    override fun getSeekParameters(): SeekParameters = currentPlayer.seekParameters
    override fun setForegroundMode(foregroundMode: Boolean) { currentPlayer.setForegroundMode(foregroundMode) }
    override fun setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean) { currentPlayer.setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems) }
    override fun getPauseAtEndOfMediaItems(): Boolean = currentPlayer.pauseAtEndOfMediaItems
    override fun setShuffleOrder(shuffleOrder: ShuffleOrder) { currentPlayer.setShuffleOrder(shuffleOrder) }
    
    // Missing methods from Player interface
    override fun setMediaSource(mediaSource: MediaSource) { currentPlayer.setMediaSource(mediaSource) }
    override fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long) { currentPlayer.setMediaSource(mediaSource, startPositionMs) }
    override fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean) { currentPlayer.setMediaSource(mediaSource, resetPosition) }
    override fun setMediaSources(mediaSources: MutableList<MediaSource>) { currentPlayer.setMediaSources(mediaSources) }
    override fun setMediaSources(mediaSources: MutableList<MediaSource>, resetPosition: Boolean) { currentPlayer.setMediaSources(mediaSources, resetPosition) }
    override fun setMediaSources(mediaSources: MutableList<MediaSource>, startIndex: Int, startPositionMs: Long) { currentPlayer.setMediaSources(mediaSources, startIndex, startPositionMs) }
    override fun addMediaSource(mediaSource: MediaSource) { currentPlayer.addMediaSource(mediaSource) }
    override fun addMediaSource(index: Int, mediaSource: MediaSource) { currentPlayer.addMediaSource(index, mediaSource) }
    override fun addMediaSources(mediaSources: MutableList<MediaSource>) { currentPlayer.addMediaSources(mediaSources) }
    override fun addMediaSources(index: Int, mediaSources: MutableList<MediaSource>) { currentPlayer.addMediaSources(index, mediaSources) }
    override fun hasNextMediaItem(): Boolean = currentPlayer.hasNextMediaItem()
    override fun hasPreviousMediaItem(): Boolean = currentPlayer.hasPreviousMediaItem()
    override fun seekToNextMediaItem() = currentPlayer.seekToNextMediaItem()
    override fun seekToPreviousMediaItem() = currentPlayer.seekToPreviousMediaItem()
    override fun seekToNext() = currentPlayer.seekToNext()
    override fun seekToPrevious() = currentPlayer.seekToPrevious()
    
    override fun setImageOutput(imageOutput: ImageOutput?) { 
        if (imageOutput != null) currentPlayer.setImageOutput(imageOutput) 
    }

    override fun increaseDeviceVolume(flags: Int) { currentPlayer.increaseDeviceVolume(flags) }
    override fun decreaseDeviceVolume(flags: Int) { currentPlayer.decreaseDeviceVolume(flags) }
    override fun prepare(mediaSource: MediaSource) { currentPlayer.prepare(mediaSource) }
    override fun prepare(mediaSource: MediaSource, resetPosition: Boolean, resetState: Boolean) { currentPlayer.prepare(mediaSource, resetPosition, resetState) }
    override fun addAudioOffloadListener(listener: ExoPlayer.AudioOffloadListener) { currentPlayer.addAudioOffloadListener(listener) }
    override fun removeAudioOffloadListener(listener: ExoPlayer.AudioOffloadListener) { currentPlayer.removeAudioOffloadListener(listener) }
    override fun getRendererCount(): Int = currentPlayer.rendererCount
    override fun getRendererType(index: Int): Int = currentPlayer.getRendererType(index)
    override fun getRenderer(index: Int): Renderer = currentPlayer.getRenderer(index)
    override fun getTrackSelector(): TrackSelector? = currentPlayer.trackSelector
    override fun getCurrentTrackGroups(): TrackGroupArray = currentPlayer.currentTrackGroups
    override fun getCurrentTrackSelections(): TrackSelectionArray = currentPlayer.currentTrackSelections
    override fun getPlaybackLooper(): Looper = currentPlayer.playbackLooper
    override fun setPreloadConfiguration(preloadConfiguration: ExoPlayer.PreloadConfiguration) { currentPlayer.preloadConfiguration = preloadConfiguration }
    override fun getPreloadConfiguration(): ExoPlayer.PreloadConfiguration = currentPlayer.preloadConfiguration
    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) { currentPlayer.replaceMediaItem(index, mediaItem) }
    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>) { currentPlayer.replaceMediaItems(fromIndex, toIndex, mediaItems) }
    override fun setVideoEffects(effects: MutableList<Effect>) { currentPlayer.setVideoEffects(effects) }
    override fun createMessage(target: PlayerMessage.Target): PlayerMessage = currentPlayer.createMessage(target)
    override fun setWakeMode(mode: Int) { currentPlayer.setWakeMode(mode) }
    
    override fun setPriority(priority: Int) { 
        // Try to find a setter, otherwise no-op
    }
    
    // override fun isReleased(): Boolean = false // If strictly required

    // Missing Player/ExoPlayer overrides
    override fun setPriorityTaskManager(priorityTaskManager: PriorityTaskManager?) { currentPlayer.setPriorityTaskManager(priorityTaskManager) }
    override fun isSleepingForOffload(): Boolean = currentPlayer.isSleepingForOffload
    override fun isTunnelingEnabled(): Boolean = currentPlayer.isTunnelingEnabled
    override fun setMediaItem(mediaItem: MediaItem) { currentPlayer.setMediaItem(mediaItem) }
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) { currentPlayer.setMediaItem(mediaItem, startPositionMs) }
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) { currentPlayer.setMediaItem(mediaItem, resetPosition) }
    override fun addMediaItem(mediaItem: MediaItem) { currentPlayer.addMediaItem(mediaItem) }
    override fun addMediaItem(index: Int, mediaItem: MediaItem) { currentPlayer.addMediaItem(index, mediaItem) }
    override fun isPlaying(): Boolean = currentPlayer.isPlaying
    override fun seekBack() { currentPlayer.seekBack() }
    override fun seekForward() { currentPlayer.seekForward() }
    override fun seekToPreviousWindow() { currentPlayer.seekToPreviousWindow() }
    override fun seekToNextWindow() { currentPlayer.seekToNextWindow() }
    override fun hasNext(): Boolean = currentPlayer.hasNext()
    override fun hasNextWindow(): Boolean = currentPlayer.hasNextWindow()
    override fun next() { currentPlayer.next() }
    override fun setPlaybackSpeed(speed: Float) { currentPlayer.setPlaybackSpeed(speed) }
    override fun getCurrentManifest(): Any? = currentPlayer.currentManifest
    override fun getCurrentPeriodIndex(): Int = currentPlayer.currentPeriodIndex
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
    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) { currentPlayer.setAudioAttributes(audioAttributes, handleAudioFocus) }
}
