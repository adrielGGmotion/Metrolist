package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IsolatedAudioController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val playlist: Map<Int, String?>
) {
    private lateinit var players: List<ExoPlayer>
    private var currentPlayerIndex = 0

    private var loadJob: Job? = null

    fun prepare() {
        players = listOf(buildPlayer(), buildPlayer())
        // Preload the first song
        onPageChanged(0)
    }

    private fun buildPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .build()
    }

    fun onPageChanged(page: Int) {
        val songId = playlist[page]

        if (songId == null) {
            // No song for this page, fade out current player
            val currentPlayer = players[currentPlayerIndex]
            fadeOut(currentPlayer)
            return
        }

        val currentPlayer = players[currentPlayerIndex]
        val nextPlayer = players[(currentPlayerIndex + 1) % 2]

        if (nextPlayer.currentMediaItem?.mediaId == songId) {
            // The song was preloaded correctly. Crossfade.
            crossfade(from = currentPlayer, to = nextPlayer)
            currentPlayerIndex = (currentPlayerIndex + 1) % 2
        } else {
            // Song was not preloaded, or we are jumping pages.
            // Stop current player, load and play on next player.
            fadeOut(currentPlayer)
            loadAndPlay(nextPlayer, songId)
            currentPlayerIndex = (currentPlayerIndex + 1) % 2
        }

        // Preload the song for the next page
        preload(page + 1)
    }

    private fun preload(page: Int) {
        loadJob?.cancel()
        loadJob = scope.launch {
            val songId = playlist[page]
            if (songId != null) {
                val playerToLoad = players[(currentPlayerIndex + 1) % 2]
                load(playerToLoad, songId)
            }
        }
    }

    private suspend fun getSongUrl(songId: String): String? {
        return withContext(Dispatchers.IO) {
            val playerResponse = YouTube.player(songId, client = YouTubeClient.WEB_REMIX).getOrNull()
            playerResponse?.streamingData?.adaptiveFormats
                ?.filter { it.mimeType?.startsWith("audio") == true }
                ?.maxByOrNull { it.bitrate ?: 0 }?.url
        }
    }

    private suspend fun load(player: ExoPlayer, songId: String, onReady: () -> Unit = {}) {
        val url = getSongUrl(songId)
        if (url != null) {
            withContext(Dispatchers.Main) {
                val mediaItem = MediaItem.Builder().setUri(url).setMediaId(songId).build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            val seekPosition = if (player.duration > 30000) 30000L else 0L
                            player.seekTo(seekPosition)
                            player.removeListener(this)
                            onReady()
                        }
                    }
                })
            }
        }
    }

    private fun loadAndPlay(player: ExoPlayer, songId: String) {
        scope.launch {
            load(player, songId) {
                player.volume = 1f
                player.play()
            }
        }
    }

    private fun crossfade(from: ExoPlayer, to: ExoPlayer) {
        scope.launch {
            fadeIn(to)
            fadeOut(from)
        }
    }

    private fun fadeIn(player: ExoPlayer) {
        scope.launch {
            player.volume = 0f
            player.play()
            for (i in 0..10) {
                player.volume = i / 10f
                delay(50)
            }
            player.volume = 1f
        }
    }

    private fun fadeOut(player: ExoPlayer) {
        scope.launch {
            for (i in 10 downTo 0) {
                player.volume = i / 10f
                delay(50)
            }
            player.stop()
            player.clearMediaItems()
        }
    }

    fun release() {
        players.forEach { it.release() }
    }
}
