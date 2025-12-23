package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.db.DatabaseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.SongWithStats
import com.metrolist.music.ui.screens.wrapped.WrappedConstants.PAGE_TOP_5_ARTISTS
import com.metrolist.music.ui.screens.wrapped.WrappedConstants.PAGE_TOP_5_SONGS
import com.metrolist.music.ui.screens.wrapped.WrappedConstants.PAGE_TOP_ARTIST
import com.metrolist.music.ui.screens.wrapped.WrappedConstants.PAGE_TOP_SONG
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class WrappedManager(
    context: Context,
    private val databaseDao: DatabaseDao,
    private val scope: CoroutineScope
) {
    private val audioController = IsolatedAudioController(context, scope)

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

    private val pageToSongMap = mutableMapOf<Int, String?>()

    init {
        loadData()
    }

    private fun loadData() {
        scope.launch {
            _isLoading.value = true
            _accountInfo.value = YouTube.accountInfo().getOrNull()

            val fromTimestamp = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val topSongsResult = databaseDao.mostPlayedSongsStats(fromTimestamp, limit = 10).first()
            _topSongs.value = topSongsResult
            _topArtists.value = databaseDao.mostPlayedArtists(fromTimestamp, limit = 5).first()

            assignSongsToPages(topSongsResult)

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
            _isLoading.value = false
        }
    }

    private suspend fun assignSongsToPages(songs: List<SongWithStats>) {
        if (songs.isEmpty()) return

        val availableSongs = songs.toMutableList()
        val assignedIds = mutableSetOf<String>()

        // Rule 1: Top Song screen gets the #1 song
        val topSong = availableSongs.removeFirstOrNull()
        pageToSongMap[PAGE_TOP_SONG] = topSong?.id
        topSong?.id?.let { assignedIds.add(it) }

        // Rule 2: Top Artist screen
        val topArtist = topArtists.value.firstOrNull()
        if (topArtist != null) {
            // Find the top artist's most listened to song THAT ISN'T ALREADY ASSIGNED
            val topArtistSong = databaseDao.getTopSongForArtist(topArtist.id, assignedIds).firstOrNull()
            if (topArtistSong != null) {
                pageToSongMap[PAGE_TOP_ARTIST] = topArtistSong.song.id
                topArtistSong.song.id.let { songId ->
                    assignedIds.add(songId)
                    availableSongs.removeAll { it.id == songId }
                }
            } else {
                // Fallback: If no other song, use the next available unassigned song
                val fallbackSong = availableSongs.removeFirstOrNull()
                pageToSongMap[PAGE_TOP_ARTIST] = fallbackSong?.id
                fallbackSong?.id?.let { assignedIds.add(it) }
            }
        }


        // Assign remaining songs to other screens
        pageToSongMap[PAGE_TOP_5_SONGS] = availableSongs.removeFirstOrNull()?.id?.also { assignedIds.add(it) }
        pageToSongMap[PAGE_TOP_5_ARTISTS] = availableSongs.removeFirstOrNull()?.id?.also { assignedIds.add(it) }

        // Default for any other screen (e.g., intro, minutes)
        val defaultSong = topSong?.id
        val allPages = (0..7)
        allPages.forEach { page ->
            if (!pageToSongMap.containsKey(page)) {
                pageToSongMap[page] = defaultSong
            }
        }
    }


    fun playTrackForPage(page: Int) {
        if (isLoading.value) return
        val songId = pageToSongMap[page]
        audioController.load(songId)
    }

    fun release() {
        audioController.release()
    }
}
