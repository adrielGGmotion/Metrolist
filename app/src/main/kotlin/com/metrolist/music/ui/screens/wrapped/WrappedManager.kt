package com.metrolist.music.ui.screens.wrapped

import android.util.Log
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.constants.ArtistSongSortType
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.SongWithStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

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

            // Assign Top Song first, as it's the most important.
            val topSong = topSongs.first()
            playlistMap[WrappedScreenType.TopSong] = topSong.id
            usedSongIds.add(topSong.id)

            // Assign Top Artist's song, ensuring it's not the same as the top overall song.
            topArtists.firstOrNull()?.let { artist ->
                val artistTopSongs = databaseDao.artistSongs(
                    artistId = artist.id, // Verified schema: artist.id is correct
                    sortType = ArtistSongSortType.PLAY_TIME,
                    descending = true
                ).first()

                val topArtistSong = artistTopSongs.firstOrNull { it.id !in usedSongIds }
                    ?: artistTopSongs.firstOrNull() // Fallback to any song if all are used

                topArtistSong?.let {
                    playlistMap[WrappedScreenType.TopArtist] = it.id
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
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            _topSongs.value = databaseDao.mostPlayedSongsStats(fromTimestamp, limit = 50).first()
            _topArtists.value = databaseDao.mostPlayedArtists(fromTimestamp, limit = 5).first()

            val seenSongIds = mutableSetOf<String>()
            var totalSeconds = 0L

            withContext(Dispatchers.IO) {
                val events = databaseDao.events().first()
                events.forEach { event ->
                    totalSeconds += event.event.playTime / 1000
                    seenSongIds.add(event.song.id)
                }
            }

            withContext(Dispatchers.IO) {
                // Continue with existing history fetching logic...
                val historyPage = YouTube.musicHistory().getOrNull()
                historyPage?.sections?.forEach { section ->
                    section.songs.forEach { song ->
                        if (song.id !in seenSongIds) {
                            totalSeconds += song.duration ?: 0
                        }
                    }
                }
            }

            val totalMinutes = totalSeconds / 60
            _totalMinutes.value = totalMinutes
            generatePlaylistMap()
            _isLoading.value = false
        }
    }
}
