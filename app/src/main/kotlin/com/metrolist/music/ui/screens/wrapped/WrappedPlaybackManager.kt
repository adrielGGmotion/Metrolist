package com.metrolist.music.ui.screens.wrapped

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.common.collect.ImmutableList
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.utils.completed
import com.metrolist.music.player.PlayerConnection
import androidx.datastore.preferences.core.edit
import com.metrolist.music.player.PlayerExt.mediaItems
import com.metrolist.music.player.YTPlayerUtils
import com.metrolist.music.preferences.PauseListenHistoryKey
import com.metrolist.music.ui.screens.wrapped.stats.WrappedStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WrappedPlaybackManager(
    private val playerConnection: PlayerConnection,
    private val scope: CoroutineScope,
) {
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    fun initialize() {
        scope.launch {
            playerConnection.dataStore.edit { it[PauseListenHistoryKey] = true }
        }
        saveCurrentPlayerState()
        playerConnection.player?.pause()
        scope.launch {
            prepareSongs()
            _isReady.value = true
        }
    }

    fun release() {
        scope.launch {
            playerConnection.dataStore.edit { it[PauseListenHistoryKey] = false }
        }
        playerConnection.player?.stop()
        restorePlayerState()
    }

    private val streamUrls = mutableMapOf<String, String?>()
    private var topSongId: String? = null
    private var secondTopSongId: String? = null
    private var randomTopSongId: String? = null
    private var topAlbumSongId: String? = null
    private var topArtistSongId: String? = null

    private var originalMediaItems: ImmutableList<MediaItem> = ImmutableList.of()
    private var originalMediaItemIndex: Int = 0
    private var originalPosition: Long = 0
    private var originalRepeatMode: Int = Player.REPEAT_MODE_OFF

    suspend fun prepareSongs() {
        val songIds = mutableListOf<String>()

        val topSongs = withContext(Dispatchers.IO) {
            playerConnection.database.mostPlayedSongs(20, 0, WrappedStats.year)
        }
        if (topSongs.isNotEmpty()) {
            topSongId = topSongs.first().id
            songIds.add(topSongId!!)
            if (topSongs.size > 1) {
                secondTopSongId = topSongs[1].id
                songIds.add(secondTopSongId!!)
                val randomPool = topSongs.subList(1, topSongs.size)
                if (randomPool.isNotEmpty()) {
                    randomTopSongId = randomPool.random().id
                    songIds.add(randomTopSongId!!)
                }
            }
        }

        val mostListenedAlbum = withContext(Dispatchers.IO) {
            playerConnection.database.mostPlayedAlbums(1, 0, WrappedStats.year).firstOrNull()
        }
        if (mostListenedAlbum != null) {
            val albumSongs = withContext(Dispatchers.IO) {
                playerConnection.database.mostPlayedSongsInAlbum(
                    albumId = mostListenedAlbum.id,
                    year = WrappedStats.year,
                    limit = 2
                )
            }
            if (albumSongs.isNotEmpty()) {
                topAlbumSongId = if (albumSongs.first().id != topSongId) {
                    albumSongs.first().id
                } else if (albumSongs.size > 1) {
                    albumSongs[1].id
                } else {
                    secondTopSongId // Fallback
                }
                topAlbumSongId?.let { songIds.add(it) }
            }
        }

        val mostListenedArtist = withContext(Dispatchers.IO) {
            playerConnection.database.mostPlayedArtists(1, 0, WrappedStats.year).firstOrNull()
        }
        if (mostListenedArtist != null) {
            val artistSongs = withContext(Dispatchers.IO) {
                playerConnection.database.mostPlayedSongsByArtist(
                    artistId = mostListenedArtist.id,
                    year = WrappedStats.year,
                    limit = 1
                )
            }
            if (artistSongs.isNotEmpty()) {
                topArtistSongId = artistSongs.first().id
                topArtistSongId?.let { songIds.add(it) }
            }
        }

        songIds.add("B5u_DV1_NM0")

        preloadSongStreamUrls(songIds.distinct())
    }

    private suspend fun preloadSongStreamUrls(songIds: List<String>) = withContext(Dispatchers.IO) {
        val jobs = songIds.map { songId ->
            async {
                try {
                    val playerResponse = YTPlayerUtils.playerResponseForPlayback(
                        videoId = songId,
                        client = YouTubeClient.WEB_REMIX
                    ).completed()
                    val url = playerResponse?.streamingData?.adaptiveFormats
                        ?.filter { it.mimeType?.startsWith("audio") == true }
                        ?.maxByOrNull { it.bitrate ?: 0 }
                        ?.url
                    songId to url
                } catch (e: Exception) {
                    Log.e("WrappedPlaybackManager", "Failed to fetch stream URL for $songId", e)
                    songId to null
                }
            }
        }
        streamUrls.putAll(jobs.awaitAll().toMap())
    }

    fun playSongForPage(page: Int) {
        val songId = when (page) {
            in 0..2 -> randomTopSongId
            in 3..5 -> topSongId
            in 6..7 -> randomTopSongId // Re-using random song as a sensible default
            in 8..10 -> topAlbumSongId
            in 11..13 -> topArtistSongId
            in 14..15 -> "B5u_DV1_NM0"
            else -> null
        }

        scope.launch {
            val url = streamUrls[songId]
            if (songId != null && url != null) {
                withContext(Dispatchers.Main) {
                    val mediaItem = MediaItem.fromUri(url)
                    playerConnection.player?.let { player ->
                        player.setMediaItem(mediaItem)
                        player.repeatMode = Player.REPEAT_MODE_ONE
                        player.prepare()
                        player.seekTo(30000L)
                        player.play()
                    }
                }
            } else {
                Log.w("WrappedPlaybackManager", "No song found for page $page (songId: $songId)")
            }
        }
    }

    fun toggleMute() {
        playerConnection.player?.let { player ->
            player.volume = if (player.volume > 0f) 0f else 1f
        }
    }

    fun pausePlayback() {
        playerConnection.player?.pause()
    }

    fun resumePlayback() {
        playerConnection.player?.play()
    }

    private fun saveCurrentPlayerState() {
        playerConnection.player?.let { player ->
            originalMediaItems = player.mediaItems
            originalMediaItemIndex = player.currentMediaItemIndex
            originalPosition = player.currentPosition
            originalRepeatMode = player.repeatMode
        }
    }

    private fun restorePlayerState() {
        playerConnection.player?.let { player ->
            if (originalMediaItems.isNotEmpty()) {
                player.setMediaItems(originalMediaItems.toList(), originalMediaItemIndex, originalPosition)
                player.repeatMode = originalRepeatMode
                player.prepare()
                player.pause() // Don't auto-play when restoring
            }
        }
    }
}
