/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(
    token = token,
    os = "Android",
    browser = "Discord Android",
    device = android.os.Build.DEVICE,
    userAgent = SuperProperties.userAgent,
    superPropertiesBase64 = SuperProperties.superPropertiesBase64
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var restartJob: Job? = null
    private var lastActivityParams: ActivityParams? = null

    private data class ActivityParams(
        val song: Song,
        val currentPlaybackTimeMillis: Long,
        val playbackSpeed: Float,
        val useDetails: Boolean,
        val status: String,
        val button1Text: String,
        val button1Visible: Boolean,
        val button2Text: String,
        val button2Visible: Boolean,
        val activityType: String,
        val activityName: String
    )

    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        playbackSpeed: Float = 1.0f,
        useDetails: Boolean = false,
        status: String = "online",
        button1Text: String = "",
        button1Visible: Boolean = true,
        button2Text: String = "",
        button2Visible: Boolean = true,
        activityType: String = "listening",
        activityName: String = "",
    ) = runCatching {
        lastActivityParams = ActivityParams(
            song, currentPlaybackTimeMillis, playbackSpeed, useDetails, status,
            button1Text, button1Visible, button2Text, button2Visible, activityType, activityName
        )

        if (!isRpcRunning()) {
            Timber.tag(TAG).d("RPC not running, attempting to connect...")
            // The KizzyRPC.setActivity will call connect() if not running
        }

        scheduleJitteredRestart()

        val currentTime = System.currentTimeMillis()

        val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime

        val songTitleWithRate = if (playbackSpeed != 1.0f) {
            "${song.song.title} [${String.format("%.2fx", playbackSpeed)}]"
        } else {
            song.song.title
        }

        val remainingDuration = song.song.duration * 1000L - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

        val buttonsList = mutableListOf<Pair<String, String>>()
        if (button1Visible) {
            val resolvedText = resolveVariables(
                button1Text.ifEmpty { "Listen on YouTube Music" },
                song
            )
            buttonsList.add(resolvedText to "https://music.youtube.com/watch?v=${song.song.id}")
        }
        if (button2Visible) {
            val resolvedText = resolveVariables(
                button2Text.ifEmpty { "Visit Metrolist" },
                song
            )
            buttonsList.add(resolvedText to "https://github.com/MetrolistGroup/Metrolist")
        }

        val type = when (activityType) {
            "playing" -> Type.PLAYING
            "watching" -> Type.WATCHING
            "competing" -> Type.COMPETING
            else -> Type.LISTENING
        }

        val name = activityName.ifEmpty {
            context.getString(R.string.app_name).removeSuffix(" Debug")
        }

        setActivity(
            name = name,
            details = songTitleWithRate,
            state = song.artists.joinToString { it.name },
            detailsUrl = "https://music.youtube.com/watch?v=${song.song.id}",
            largeImage = song.song.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            smallImage = song.artists.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            largeText = song.album?.title,
            smallText = song.artists.firstOrNull()?.name,
            buttons = if (buttonsList.isNotEmpty()) buttonsList else null,
            type = type,
            statusDisplayType = if (useDetails) StatusDisplayType.DETAILS else StatusDisplayType.STATE,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID,
            status = status
        )
    }

    override suspend fun close() {
        restartJob?.cancel()
        super.close()
    }

    fun closeRPCWithJitter() {
        restartJob?.cancel()
        super.closeRPC()
    }

    private fun scheduleJitteredRestart() {
        if (restartJob?.isActive == true) return

        restartJob = scope.launch {
            // Random delay between 2 to 4 hours to avoid detection
            val minMillis = 2 * 60 * 60 * 1000L
            val maxMillis = 4 * 60 * 60 * 1000L
            val delayMillis = Random.nextLong(minMillis, maxMillis)

            Timber.tag(TAG).d("Scheduling jittered RPC restart in ${delayMillis / 1000 / 60} minutes")
            delay(delayMillis)

            Timber.tag(TAG).d("Executing jittered RPC restart...")
            closeRPCWithJitter()
            delay(5000) // Give it some time to settle

            lastActivityParams?.let { params ->
                updateSong(
                    params.song,
                    params.currentPlaybackTimeMillis, // Note: this might be slightly outdated but will be corrected on next player event
                    params.playbackSpeed,
                    params.useDetails,
                    params.status,
                    params.button1Text,
                    params.button1Visible,
                    params.button2Text,
                    params.button2Visible,
                    params.activityType,
                    params.activityName
                )
            }
        }
    }

    companion object {
        private const val TAG = "DiscordRPC"
        private const val APPLICATION_ID = "1411019391843172514"

        /**
         * Resolves template variables in text.
         * Supported: {song_name}, {artist_name}, {album_name}
         */
        fun resolveVariables(text: String, song: Song): String {
            return text
                .replace("{song_name}", song.song.title)
                .replace("{artist_name}", song.artists.joinToString { it.name })
                .replace("{album_name}", song.album?.title ?: "")
        }
    }
}
