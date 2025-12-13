package com.metrolist.music.wrapped

import android.content.Context
import androidx.media3.common.MediaItem
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

    init {
        exoPlayer.volume = 0f
    }

    fun play(songId: String) {
        playJob?.cancel()
        playJob = coroutineScope.launch(Dispatchers.Main) {
            val playerResponse = YouTube.player(songId, client = YouTubeClient.WEB_REMIX).getOrNull()
            val audioFormat = playerResponse?.streamingData?.adaptiveFormats?.firstOrNull { it.isAudio }
            val streamUrl = audioFormat?.url ?: return@launch

            val duration = playerResponse.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000) ?: 0
            val previewStartTime = playerResponse.videoDetails?.previewStartTime?.toLongOrNull()
            val startTime = previewStartTime ?: (duration * 0.3).toLong()

            fadeOut()
            delay(500)

            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl), startTime)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

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
