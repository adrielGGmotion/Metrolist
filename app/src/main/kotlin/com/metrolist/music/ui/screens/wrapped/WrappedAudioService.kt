package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.music.R
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WrappedAudioService(
    private val context: Context,
    private val scope: CoroutineScope,
    private val connectivityManager: ConnectivityManager
) {
    private var player: SafePlayerWrapper? = null
    private var transitionJob: Job? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e("WrappedAudioService", "Player error, switching to fallback audio.", error)
            onPageChanged(null) // Pass null to trigger fallback
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        val targetVolume = if (_isMuted.value) 0f else 1f
        player?.setVolume(targetVolume)
    }

    fun onPageChanged(songId: String?) {
        transitionJob?.cancel()
        transitionJob = scope.launch {
            val currentPlayer = player
            player = null // Prevent further interaction with the old player

            if (currentPlayer != null) {
                fadeOutAndRelease(currentPlayer)
            }

            val newSongUri = getSafeUri(songId)

            withContext(Dispatchers.Main) {
                val newPlayer = SafePlayerWrapper(context, playerListener)
                player = newPlayer
                newPlayer.prepare(MediaItem.fromUri(newSongUri))
                if (songId != null) {
                    newPlayer.seekTo(30_000)
                }
                newPlayer.play()
                fadeIn(newPlayer)
            }
        }
    }

    private suspend fun getSafeUri(songId: String?): Uri {
        val fallbackUri = Uri.parse("android.resource://${context.packageName}/${R.raw.wrapped_theme}")
        if (songId == null) {
            Log.i("WrappedAudio", "No song ID provided, using fallback audio.")
            return fallbackUri
        }

        return try {
            val audioQuality = context.dataStore.get(com.metrolist.music.constants.AudioQualityKey).let {
                AudioQuality.valueOf(it ?: AudioQuality.AUTO.name)
            }

            val playbackData = withContext(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    videoId = songId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager
                ).getOrNull()
            }

            val streamUrl = playbackData?.streamUrl
            if (streamUrl.isNullOrBlank()) {
                Log.w("WrappedAudio", "Resolved URL for $songId is null or blank. Using fallback.")
                fallbackUri
            } else {
                Log.d("WrappedAudio", "Resolved URL: $streamUrl")
                Uri.parse(streamUrl)
            }
        } catch (e: Exception) {
            Log.e("WrappedAudio", "Failed to resolve URL for $songId. Using fallback.", e)
            fallbackUri
        }
    }

    private fun fadeIn(player: SafePlayerWrapper) {
        scope.launch(Dispatchers.Main) {
            val targetVolume = if (_isMuted.value) 0f else 1f
            player.setVolume(0f)
            for (i in 1..20) {
                delay(25)
                player.setVolume(targetVolume * (i / 20f))
            }
            player.setVolume(targetVolume)
        }
    }

    private suspend fun fadeOutAndRelease(player: SafePlayerWrapper) {
        try {
            for (i in 10 downTo 0) {
                delay(25)
                player.setVolume(i / 10f)
            }
        } catch (e: Exception) {
            Log.e("WrappedAudioService", "Error during fade out, releasing immediately.", e)
        } finally {
            withContext(Dispatchers.Main) {
                player.release()
            }
        }
    }

    fun release() {
        transitionJob?.cancel()
        scope.launch {
            player?.let {
                fadeOutAndRelease(it)
            }
            player = null
        }
    }

    private class SafePlayerWrapper(
        context: Context,
        listener: Player.Listener
    ) {
        private var player: ExoPlayer? = ExoPlayer.Builder(context).build()
        @Volatile
        private var isReleased = false

        init {
            player?.addListener(listener)
        }

        fun setVolume(volume: Float) {
            if (isReleased) return
            try {
                player?.volume = volume
            } catch (e: IllegalStateException) {
                Log.w("SafePlayerWrapper", "setVolume failed: ${e.message}")
            }
        }

        fun play() {
            if (isReleased) return
            try {
                player?.play()
            } catch (e: IllegalStateException) {
                Log.w("SafePlayerWrapper", "play failed: ${e.message}")
            }
        }

        fun seekTo(position: Long) {
            if (isReleased) return
            try {
                player?.seekTo(position)
            } catch (e: IllegalStateException) {
                Log.w("SafePlayerWrapper", "seekTo failed: ${e.message}")
            }
        }

        fun prepare(mediaItem: MediaItem) {
            if (isReleased) return
            try {
                player?.setMediaItem(mediaItem)
                player?.prepare()
            } catch (e: IllegalStateException) {
                Log.w("SafePlayerWrapper", "prepare failed: ${e.message}")
            }
        }

        fun release() {
            if (isReleased) return
            isReleased = true
            try {
                player?.release()
                player = null
            } catch (e: Exception) {
                Log.e("SafePlayerWrapper", "Exception during release.", e)
            }
        }
    }
}
