package com.metrolist.music.wrapped

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WrappedAudioManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val _volume = MutableStateFlow(0f)
    val volume: StateFlow<Float> = _volume
    private var playJob: Job? = null
    private val logTag = "[WrappedAudio]"

    init {
        exoPlayer.volume = 1f
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Log.d(logTag, "Player State: $stateString")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(logTag, "Player error: ", error)
            }
        })
    }

    fun play(songId: String) {
        playJob?.cancel()
        playJob = coroutineScope.launch(Dispatchers.Main) {
            Log.d(logTag, "Loading track: $songId")
            Log.d(logTag, "Fetching fresh URL...")
            val playerResponse = YouTube.player(songId, client = YouTubeClient.WEB_REMIX).getOrNull()
            val audioFormat = playerResponse?.streamingData?.adaptiveFormats?.firstOrNull { it.isAudio }
            val streamUrl = audioFormat?.url

            if (streamUrl == null) {
                Log.e(logTag, "Failed to get stream URL for $songId")
                return@launch
            }

            val duration = playerResponse.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000) ?: 0
            val previewStartTime = playerResponse.videoDetails?.previewStartTime?.toLongOrNull()
            val startTime = previewStartTime ?: (duration * 0.3).toLong()

            fadeOut()
            delay(500)

            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl), startTime)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            Log.d(logTag, "Volume: ${exoPlayer.volume}")

            fadeIn()
        }
    }

    private fun fadeOut() {
        _volume.value = 0f
    }

    private fun fadeIn() {
        _volume.value = 1f
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    fun release() {
        exoPlayer.release()
    }
}
