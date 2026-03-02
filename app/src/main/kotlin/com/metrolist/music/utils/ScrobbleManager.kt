/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.lastfm.LastFM
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.min

class ScrobbleManager(
    private val scope: CoroutineScope,
    var minSongDuration: Int = 30,
    var scrobbleDelayPercent: Float = 0.5f,
    var scrobbleDelaySeconds: Int = 50
) {
    private companion object {
        const val TAG = "LastFM"
    }
    private var scrobbleJob: Job? = null
    private var scrobbleRemainingMillis: Long = 0L
    private var scrobbleTimerStartedAt: Long = 0L
    private var songStartedAt: Long = 0L
    private var songStarted = false
    var useNowPlaying = true

    fun destroy() {
        scrobbleJob?.cancel()
        scrobbleRemainingMillis = 0L
        scrobbleTimerStartedAt = 0L
        songStartedAt = 0L
        songStarted = false
        Timber.tag(TAG).d("ScrobbleManager destroyed")
    }

    fun onSongStart(metadata: MediaMetadata?, duration: Long? = null) {
        if (metadata == null) return
        songStartedAt = System.currentTimeMillis() / 1000
        songStarted = true
        Timber.tag(TAG).d("onSongStart: ${metadata.title}")
        startScrobbleTimer(metadata, duration)
        if (useNowPlaying) {
            updateNowPlaying(metadata)
        }
    }

    fun onSongResume(metadata: MediaMetadata) {
        Timber.tag(TAG).d("onSongResume: ${metadata.title}")
        resumeScrobbleTimer(metadata)
    }

    fun onSongPause() {
        Timber.tag(TAG).d("onSongPause")
        pauseScrobbleTimer()
    }

    fun onSongStop() {
        Timber.tag(TAG).d("onSongStop")
        stopScrobbleTimer()
        songStarted = false
    }

    private fun startScrobbleTimer(metadata: MediaMetadata, duration: Long? = null) {
        scrobbleJob?.cancel()
        val duration = duration?.toInt()?.div(1000) ?: metadata.duration

        if (duration <= minSongDuration) {
            Timber.tag(TAG).d("Song too short to scrobble (${duration}s)")
            return
        }

        val threshold = duration * 1000L * scrobbleDelayPercent
        scrobbleRemainingMillis = min(threshold.toLong(), scrobbleDelaySeconds * 1000L)

        if (scrobbleRemainingMillis <= 0) {
            scrobbleSong(metadata)
            return
        }
        scrobbleTimerStartedAt = System.currentTimeMillis()
        Timber.tag(TAG).d("Starting scrobble timer for ${scrobbleRemainingMillis}ms")
        scrobbleJob = scope.launch {
            delay(scrobbleRemainingMillis)
            scrobbleSong(metadata)
            scrobbleJob = null
        }
    }

    private fun pauseScrobbleTimer() {
        scrobbleJob?.cancel()
        if (scrobbleTimerStartedAt != 0L) {
            val elapsed = System.currentTimeMillis() - scrobbleTimerStartedAt
            scrobbleRemainingMillis -= elapsed
            if (scrobbleRemainingMillis < 0) scrobbleRemainingMillis = 0
            scrobbleTimerStartedAt = 0L
            Timber.tag(TAG).d("Scrobble timer paused. ${scrobbleRemainingMillis}ms remaining")
        }
    }

    private fun resumeScrobbleTimer(metadata: MediaMetadata) {
        if (scrobbleRemainingMillis <= 0) return
        scrobbleJob?.cancel()
        scrobbleTimerStartedAt = System.currentTimeMillis()
        Timber.tag(TAG).d("Resuming scrobble timer for ${scrobbleRemainingMillis}ms")
        scrobbleJob = scope.launch {
            delay(scrobbleRemainingMillis)
            scrobbleSong(metadata)
            scrobbleJob = null
        }
    }

    private fun stopScrobbleTimer() {
        scrobbleJob?.cancel()
        scrobbleJob = null
        scrobbleRemainingMillis = 0
        Timber.tag(TAG).d("Scrobble timer stopped")
    }

    private fun scrobbleSong(metadata: MediaMetadata) {
        Timber.tag(TAG).d("Scrobbling: ${metadata.title}")
        scope.launch {
            LastFM.scrobble(
                artist = metadata.artists.joinToString { it.name },
                track = metadata.title,
                duration = metadata.duration,
                timestamp = songStartedAt,
                album = metadata.album?.title,
            )
        }
    }

    private fun updateNowPlaying(metadata: MediaMetadata) {
        Timber.tag(TAG).d("Updating Now Playing: ${metadata.title}")
        scope.launch {
            LastFM.updateNowPlaying(
                artist = metadata.artists.joinToString { it.name },
                track = metadata.title,
                album = metadata.album?.title,
                duration = metadata.duration
            )
        }
    }

    fun onPlayerStateChanged(isPlaying: Boolean, metadata: MediaMetadata?, duration: Long? = null) {
        if (metadata == null) return
        if (isPlaying) {
            if (!songStarted) {
                onSongStart(metadata, duration)
            } else {
                onSongResume(metadata)
            }
        } else {
            onSongPause()
        }
    }
}
