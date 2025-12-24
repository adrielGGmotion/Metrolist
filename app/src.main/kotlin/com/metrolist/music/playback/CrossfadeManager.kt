package com.metrolist.music.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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

    private val internalListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player === currentPlayer) {
                listeners.forEach {
                    // Forward events, but report this manager as the player.
                    it.onEvents(this@CrossfadeManager, events)
                }
            }
        }
    }

    private val player1: ExoPlayer
    private val player2: ExoPlayer
    private var currentPlayer: ExoPlayer

    init {
        val builder = ExoPlayer.Builder(context)
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
        player1 = builder.build()
        player2 = builder.build()
        currentPlayer = player1

        player1.addListener(internalListener)
        player2.addListener(internalListener)
    }

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
        currentPlayer.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            play()
        } else {
            pause()
        }
        currentPlayer.playWhenReady = playWhenReady
    }

    override fun stop() {
        crossfadeJob?.cancel()
        player1.stop()
        player2.stop()
    }

    override fun release() {
        crossfadeJob?.cancel()
        player1.release()
        player2.release()
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        player1.setMediaItems(mediaItems)
        player2.setMediaItems(mediaItems)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        player1.setMediaItems(mediaItems, resetPosition)
        player2.setMediaItems(mediaItems, resetPosition)
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        player1.setMediaItems(mediaItems, startIndex, startPositionMs)
        player2.setMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        player1.addMediaItem(mediaItem)
        player2.addMediaItem(mediaItem)
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        player1.addMediaItems(mediaItems)
        player2.addMediaItems(mediaItems)
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        player1.addMediaItem(index, mediaItem)
        player2.addMediaItem(index, mediaItem)
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        player1.addMediaItems(index, mediaItems)
        player2.addMediaItems(index, mediaItems)
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

    private suspend fun startCrossfade() {
        while (scope.isActive) {
            val duration = currentPlayer.duration
            val position = currentPlayer.currentPosition
            if (duration > 0 && duration - position < crossfadeDuration * 1000) {
                if (!isCrossfading) {
                    val nextPlayer = if (currentPlayer === player1) player2 else player1
                    if (currentPlayer.nextMediaItemIndex != C.INDEX_UNSET) {
                        isCrossfading = true
                        // Move to next track
                        nextPlayer.seekTo(currentPlayer.nextMediaItemIndex, 0)
                        nextPlayer.prepare()
                        nextPlayer.play()

                        // Volume ramp
                        val steps = 20
                        val stepDuration = (crossfadeDuration * 1000) / steps
                        for (i in 1..steps) {
                            val volume = 1f - (i.toFloat() / steps)
                            currentPlayer.volume = max(0f, volume)
                            nextPlayer.volume = max(0f, 1f - volume)
                            delay(stepDuration.toLong())
                        }

                        currentPlayer.stop()
                        currentPlayer.volume = 1f
                        currentPlayer = nextPlayer
                        isCrossfading = false
                    }
                }
            }
            delay(500)
        }
    }

    // Custom methods for CrossfadeManager
    fun addAnalyticsListener(listener: AnalyticsListener) {
        player1.addAnalyticsListener(listener)
        player2.addAnalyticsListener(listener)
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
}
