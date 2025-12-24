package com.metrolist.music.playback

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.ShuffleOrder
import com.metrolist.music.extensions.setOffloadEnabled

class CrossfadeManager(private val exoPlayer: ExoPlayer) : Player by exoPlayer {

    // Custom methods for CrossfadeManager that are ExoPlayer specific
    fun addAnalyticsListener(listener: AnalyticsListener) = exoPlayer.addAnalyticsListener(listener)

    var skipSilenceEnabled: Boolean
        get() = exoPlayer.skipSilenceEnabled
        set(value) {
            exoPlayer.skipSilenceEnabled = value
        }

    fun setShuffleOrder(shuffleOrder: ShuffleOrder) = exoPlayer.setShuffleOrder(shuffleOrder)

    // Not actually implementing crossfade yet, just providing the method stub
    fun startCrossfade(durationSeconds: Int) {
        // Implement crossfade logic here
    }

    fun setOffloadEnabled(enabled: Boolean) {
        exoPlayer.setOffloadEnabled(enabled)
    }

    val audioSessionId: Int
        get() = exoPlayer.audioSessionId
}
