package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IsolatedAudioController(
    private val context: Context,
    private val parentScope: CoroutineScope,
    private val playlist: Map<Int, String?>
) {
    private lateinit var players: List<ExoPlayer>
    private var currentPlayerIndex = 0
    
    private var loadJob: Job? = null
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
    private val TAG = "IsolatedAudioController"

    fun prepare() {
        Log.d(TAG, "Preparing audio controller...")
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
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    fun onPageChanged(page: Int) {
        Log.d(TAG, "Page changed to $page")
        val songId = playlist[page]
        
        if (songId == null) {
            Log.d(TAG, "No song for page $page, fading out.")
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
            try {
                val playerResponse = YouTube.player(songId, null, YouTubeClient.WEB_REMIX).getOrThrow()
                playerResponse.streamingData?.adaptiveFormats
                    ?.filter { it.mimeType?.startsWith("audio") == true }
                    ?.maxByOrNull { it.bitrate ?: 0 }?.url
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get URL for song $songId", e)
                null
            }
        }
    }

    private suspend fun load(player: ExoPlayer, songId: String, onReady: () -> Unit = {}) {
        Log.d(TAG, "Loading song $songId...")
        val url = getSongUrl(songId)
        if (url != null) {
            withContext(Dispatchers.Main) {
                val mediaItem = MediaItem.Builder().setUri(url).setMediaId(songId).build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "Player state changed to $playbackState")
                        if (playbackState == Player.STATE_READY) {
                            val seekPosition = if (player.duration > 30000) 30000L else 0L
                            Log.d(TAG, "Player is ready, seeking to $seekPosition")
                            player.seekTo(seekPosition)
                            player.removeListener(this)
                            onReady()
                        }
                    }
                })
            }
        } else {
            Log.e(TAG, "Failed to get URL for song $songId")
        }
    }

    private fun loadAndPlay(player: ExoPlayer, songId: String) {
        Log.d(TAG, "Loading and playing song $songId")
        scope.launch {
            load(player, songId) {
                Log.d(TAG, "Song $songId is ready, playing now.")
                player.volume = 1f
                player.play()
            }
        }
    }
    
    private fun crossfade(from: ExoPlayer, to: ExoPlayer) {
        Log.d(TAG, "Crossfading players.")
        scope.launch {
            fadeIn(to)
            fadeOut(from)
        }
    }

    private fun fadeIn(player: ExoPlayer) {
        Log.d(TAG, "Fading in player.")
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
        Log.d(TAG, "Fading out player.")
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
        Log.d(TAG, "Releasing audio controller.")
        scope.cancel()
        players.forEach { it.release() }
    }
}
