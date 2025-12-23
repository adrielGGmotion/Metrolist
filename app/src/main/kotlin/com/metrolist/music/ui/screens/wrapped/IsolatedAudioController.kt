package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.PauseListenHistoryKey
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IsolatedAudioController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var player: ExoPlayer? = null
    private var loadJob: Job? = null
    private var wasHistoryPausedInitially: Boolean = false
    private val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)


    init {
        scope.launch {
            // 1. Save the original state
            wasHistoryPausedInitially = context.dataStore.data.first()[PauseListenHistoryKey] ?: false
            // 2. Set the key to true for the duration of Wrapped
            context.dataStore.edit { settings ->
                settings[PauseListenHistoryKey] = true
            }
        }
        initializePlayer()
    }

    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(context)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true // handleAudioFocus
                )
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE // Loop the track
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                seekTo(30000)
                            }
                        }
                    })
                }
        }
    }

    fun play() {
        player?.playWhenReady = true
    }

    fun pause() {
        player?.playWhenReady = false
    }

    fun setMute(isMuted: Boolean) {
        player?.volume = if (isMuted) 0f else 1f
    }

    fun load(songId: String?) {
        loadJob?.cancel() // Cancel any previous loading job
        if (songId == null) {
            player?.stop()
            player?.clearMediaItems()
            return
        }

        loadJob = scope.launch {
            val streamUrl = withContext(Dispatchers.IO) {
                if (connectivityManager == null) {
                    return@withContext null
                }
                YTPlayerUtils.playerResponseForPlayback(
                    videoId = songId,
                    // Using a reasonable default for audio quality in this isolated context
                    audioQuality = AudioQuality.AUTO,
                    connectivityManager = connectivityManager
                ).getOrNull()?.streamUrl
            }

            if (streamUrl != null) {
                val mediaItem = MediaItem.fromUri(streamUrl)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                play()
            } else {
                // Handle case where stream URL could not be fetched
                // Maybe log an error or play a fallback sound
                player?.stop()
            }
        }
    }

    fun release() {
        loadJob?.cancel()
        player?.release()
        player = null
        scope.launch {
            // Restore the original setting.
            context.dataStore.edit { settings ->
                settings[PauseListenHistoryKey] = wasHistoryPausedInitially
            }
        }
    }
}
