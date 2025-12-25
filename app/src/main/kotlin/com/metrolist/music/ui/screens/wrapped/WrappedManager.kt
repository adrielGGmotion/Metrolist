package com.metrolist.music.ui.screens.wrapped

import android.util.Log
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.constants.ArtistSongSortType
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.SongWithStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Calendar
import java.util.UUID

sealed class PlaylistCreationState {
    object Idle : PlaylistCreationState()
    object Creating : PlaylistCreationState()
    object Success : PlaylistCreationState()
}

class WrappedManager(
    private val databaseDao: DatabaseDao,
    private val scope: CoroutineScope
) {
    private val _messagePair = MutableStateFlow<MessagePair?>(null)
    val messagePair = _messagePair.asStateFlow()

    private val _totalMinutes = MutableStateFlow<Long?>(null)
    val totalMinutes = _totalMinutes.asStateFlow().filterNotNull()

    private val _accountInfo = MutableStateFlow<AccountInfo?>(null)
    val accountInfo = _accountInfo.asStateFlow()

    private val _topSongs = MutableStateFlow<List<SongWithStats>>(emptyList())
    val topSongs = _topSongs.asStateFlow()

    private val _topArtists = MutableStateFlow<List<Artist>>(emptyList())
    val topArtists = _topArtists.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _trackMap = MutableStateFlow<Map<WrappedScreenType, String?>>(emptyMap())
    val trackMap = _trackMap.asStateFlow()

    private val _playlistCreationState = MutableStateFlow<PlaylistCreationState>(PlaylistCreationState.Idle)
    val playlistCreationState = _playlistCreationState.asStateFlow()

    fun saveWrappedPlaylist() {
        if (_playlistCreationState.value != PlaylistCreationState.Idle) return

        scope.launch {
            _playlistCreationState.value = PlaylistCreationState.Creating
            try {
                withContext(Dispatchers.IO) {
                    val playlistId = UUID.randomUUID().toString()
                    val newPlaylist = PlaylistEntity(
                        id = playlistId,
                        name = WrappedConstants.PLAYLIST_NAME,
                        thumbnailUrl = "android.resource://com.metrolist.music/drawable/ic_launcher_static_foreground",
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true
                    )
                    databaseDao.insert(newPlaylist)

                    val createdPlaylist = databaseDao.playlist(playlistId).first()
                    if (createdPlaylist != null) {
                        val songIds = _topSongs.value.map { it.id }
                        databaseDao.addSongToPlaylist(createdPlaylist, songIds)
                    } else {
                        Log.e("WrappedManager", "Failed to retrieve created playlist with id: $playlistId")
                    }
                }
                _playlistCreationState.value = PlaylistCreationState.Success
            } catch (e: Exception) {
                Log.e("WrappedManager", "Error saving wrapped playlist", e)
                _playlistCreationState.value = PlaylistCreationState.Idle
            }
        }
    }

    /**
     * Generates a map of screen types to song IDs for the Wrapped experience.
     * This function ensures that songs are unique across screens where possible
     * and that certain screens (like Tease/Reveal) share the same song.
     */
    private fun generatePlaylistMap() {
        scope.launch {
            val topSongs = _topSongs.value
            val topArtists = _topArtists.value
            if (topSongs.isEmpty()) {
                Log.w("WrappedManager", "Cannot generate playlist map, top songs list is empty.")
                _trackMap.value = emptyMap()
                return@launch
            }

            val usedSongIds = mutableSetOf<String>()
            val playlistMap = mutableMapOf<WrappedScreenType, String>()

            // Chapter 1: Top Song
            val topSong = topSongs.first()
            playlistMap[WrappedScreenType.TotalSongs] = topSong.id
            playlistMap[WrappedScreenType.TopSongTease] = topSong.id
            playlistMap[WrappedScreenType.TopSongReveal] = topSong.id
            usedSongIds.add(topSong.id)

            // Chapter 2: Top Artist
            topArtists.firstOrNull()?.let { artist ->
                val artistTopSongs = databaseDao.artistSongs(
                    artistId = artist.id,
                    sortType = ArtistSongSortType.PLAY_TIME,
                    descending = true
                ).first()

                val topArtistSong = artistTopSongs.firstOrNull { it.id !in usedSongIds }
                    ?: artistTopSongs.firstOrNull() // Fallback to any song if all are used

                topArtistSong?.let {
                    playlistMap[WrappedScreenType.TotalArtists] = it.id
                    playlistMap[WrappedScreenType.TopArtistTease] = it.id
                    playlistMap[WrappedScreenType.TopArtistReveal] = it.id
                    usedSongIds.add(it.id)
                }
            }

            // Function to find a unique song from the top tracks.
            fun findUniqueSong(): SongWithStats? {
                return topSongs.firstOrNull { it.id !in usedSongIds }
            }

            // Assign songs to the remaining screens.
            val remainingScreens = listOf(
                WrappedScreenType.Welcome,
                WrappedScreenType.MinutesTease,
                WrappedScreenType.MinutesReveal,
                WrappedScreenType.Top5Songs,
                WrappedScreenType.Top5Artists,
                WrappedScreenType.End
            ).filter { it !in playlistMap.keys }

            for (screen in remainingScreens) {
                // Link Tease and Reveal screens to the same song.
                if (screen == WrappedScreenType.MinutesTease || screen == WrappedScreenType.MinutesReveal) {
                    if (!playlistMap.containsKey(WrappedScreenType.MinutesTease)) {
                        val sharedSong = findUniqueSong() ?: topSong // Fallback to top song
                        playlistMap[WrappedScreenType.MinutesTease] = sharedSong.id
                        playlistMap[WrappedScreenType.MinutesReveal] = sharedSong.id
                        usedSongIds.add(sharedSong.id)
                    }
                } else {
                    val song = findUniqueSong() ?: topSong // Fallback to top song
                    playlistMap[screen] = song.id
                    usedSongIds.add(song.id)
                }
            }
            Log.d("WrappedManager", "Generated Playlist Map: $playlistMap")
            _trackMap.value = playlistMap
        }
    }

    fun loadData() {
        scope.launch {
            _isLoading.value = true
            _accountInfo.value = YouTube.accountInfo().getOrNull()

            val fromTimestamp = Calendar.getInstance().apply {
                set(WrappedConstants.YEAR, Calendar.JANUARY, 1, 0, 0, 0)
            }.timeInMillis

            val toTimestamp = Calendar.getInstance().apply {
                set(WrappedConstants.YEAR, Calendar.DECEMBER, 31, 23, 59, 59)
            }.timeInMillis

            _topSongs.value = databaseDao.mostPlayedSongsStats(fromTimestamp, limit = 50, toTimeStamp = toTimestamp).first()
            _topArtists.value = databaseDao.mostPlayedArtists(fromTimestamp, limit = 5, toTimeStamp = toTimestamp).first()

            val totalPlayTimeMs = databaseDao.getTotalPlayTimeInRange(fromTimestamp, toTimestamp).first() ?: 0L
            val totalMinutes = totalPlayTimeMs / 1000 / 60
            _totalMinutes.value = totalMinutes

            generatePlaylistMap()
            _isLoading.value = false
        }
    }
}
