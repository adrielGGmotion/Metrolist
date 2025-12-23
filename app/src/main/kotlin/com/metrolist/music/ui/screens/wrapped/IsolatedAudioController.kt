package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.music.constants.PauseListenHistoryKey
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class IsolatedAudioController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var player: ExoPlayer? = null
    private var wasHistoryPausedInitially: Boolean = false

    init {
        scope.launch {
            wasHistoryPausedInitially = context.dataStore.data.first()[PauseListenHistoryKey] ?: false
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
                    true
                )
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ALL // Loop the entire playlist
                }
        }
    }

    fun preparePlaylist(mediaItems: List<MediaItem>) {
        player?.setMediaItems(mediaItems)
        player?.prepare()
    }

    fun playTrackAtIndex(index: Int) {
        if (index < 0 || player?.mediaItemCount == 0 || index >= (player?.mediaItemCount ?: 0)) {
            player?.playWhenReady = false
            return
        }
        player?.seekTo(index, 30000)
        player?.playWhenReady = true
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

    fun release() {
        player?.release()
        player = null
        scope.launch {
            context.dataStore.edit { settings ->
                settings[PauseListenHistoryKey] = wasHistoryPausedInitially
            }
        }
    }
}
