package com.metrolist.music.utils

import android.content.Context
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {

    private var lastSong: Song? = null
    private var lastCurrentPlaybackTimeMillis: Long = 0
    private var lastPlaybackSpeed: Float = 1.0f
    private var lastUseDetails: Boolean = false
    suspend fun updateSong(song: Song, currentPlaybackTimeMillis: Long, playbackSpeed: Float = 1.0f, useDetails: Boolean = false) {
        lastSong = song
        lastCurrentPlaybackTimeMillis = currentPlaybackTimeMillis
        lastPlaybackSpeed = playbackSpeed
        lastUseDetails = useDetails
        updateSongInternal()
    }

    suspend fun refresh(currentPlaybackTimeMillis: Long) {
        lastCurrentPlaybackTimeMillis = currentPlaybackTimeMillis
        updateSongInternal()
    }

    private suspend fun updateSongInternal() = runCatching {
        if (lastSong == null) return@runCatching
        val currentTime = System.currentTimeMillis()
        
        val adjustedPlaybackTime = (lastCurrentPlaybackTimeMillis / lastPlaybackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime
        
        val songTitleWithRate = if (lastPlaybackSpeed != 1.0f) {
            "${lastSong!!.song.title} [${String.format("%.2fx", lastPlaybackSpeed)}]"
        } else {
            lastSong!!.song.title
        }
        
        val remainingDuration = lastSong!!.song.duration * 1000L - lastCurrentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / lastPlaybackSpeed).toLong()
        
        setActivity(
            name = context.getString(R.string.app_name).removeSuffix(" Debug"),
            details = songTitleWithRate,
            state = lastSong!!.artists.joinToString { it.name },
            detailsUrl = "https://music.youtube.com/watch?v=${lastSong!!.song.id}",
            largeImage = lastSong!!.song.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            smallImage = lastSong!!.artists.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            largeText = lastSong!!.album?.title,
            smallText = lastSong!!.artists.firstOrNull()?.name,
            buttons = listOf(
                "Listen on YouTube Music" to "https://music.youtube.com/watch?v=${lastSong!!.song.id}",
                "Visit Metrolist" to "https://github.com/mostafaalagamy/Metrolist"
            ),
            type = Type.LISTENING,
            statusDisplayType = if (lastUseDetails) StatusDisplayType.DETAILS else StatusDisplayType.STATE,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID
        )
    }

    companion object {
        private const val APPLICATION_ID = "1411019391843172514"
    }
}
