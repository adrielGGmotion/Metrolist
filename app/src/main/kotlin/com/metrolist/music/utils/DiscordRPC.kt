package com.metrolist.music.utils

import android.content.Context
import android.util.Log
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {
    suspend fun updateSong(song: Song, currentPlaybackTimeMillis: Long, playbackSpeed: Float = 1.0f, useDetails: Boolean = false) = runCatching {
        Log.d("DiscordRPC", "--- updateSong ENTER ---")
        Log.d("DiscordRPC", "Song data: $song")
        Log.d("DiscordRPC", "Large image URL: ${song.song.thumbnailUrl}")
        Log.d("DiscordRPC", "Small image URL: ${song.artists.firstOrNull()?.thumbnailUrl}")

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

        val largeImage = song.song.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
        val smallImage = song.artists.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
        Log.d("DiscordRPC", "Created RpcImage objects: large=$largeImage, small=$smallImage")

        setActivity(
            name = context.getString(R.string.app_name).removeSuffix(" Debug"),
            details = songTitleWithRate,
            state = song.artists.joinToString { it.name },
            detailsUrl = "https://music.youtube.com/watch?v=${song.song.id}",
            largeImage = largeImage,
            smallImage = smallImage,
            largeText = song.album?.title,
            smallText = song.artists.firstOrNull()?.name,
            buttons = listOf(
                "Listen on YouTube Music" to "https://music.youtube.com/watch?v=${song.song.id}",
                "Visit Metrolist" to "https://github.com/mostafaalagamy/Metrolist"
            ),
            type = Type.LISTENING,
            statusDisplayType = if (useDetails) StatusDisplayType.DETAILS else StatusDisplayType.STATE,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID
        )
    }.onFailure {
        Log.e("DiscordRPC", "Error updating song", it)
    }

    companion object {
        private const val APPLICATION_ID = "1411019391843172514"
    }
}
