package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.SongWithStats
import com.metrolist.music.ui.screens.wrapped.WrappedConstants.PAGE_TOP_5_ARTISTS
import com.metrolist.music.ui.screens.wrapped.WrappedConstants.PAGE_TOP_5_SONGS
import com.metrolist.music.ui.screens.wrapped.WrappedConstants.PAGE_TOP_ARTIST
import com.metrolist.music.ui.screens.wrapped.WrappedConstants.PAGE_TOP_SONG
import com.metrolist.music.utils.YTPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class WrappedManager(
    private val context: Context,
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

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val pageToSongMap = mutableMapOf<Int, String?>()
    private val songUrlCache = mutableMapOf<String, String?>()

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
            _topSongs.value = topSongsResult.take(5)
            _topArtists.value = databaseDao.mostPlayedArtists(fromTimestamp, limit = 5).first()

            assignSongsToPages(topSongsResult)
            preloadSongUrls()


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
        val screensWithMusic = setOf(
            PAGE_TOP_SONG,
            PAGE_TOP_ARTIST,
            PAGE_TOP_5_SONGS,
            PAGE_TOP_5_ARTISTS
        )

        // Clear any previous assignments
        pageToSongMap.clear()

        // Rule 1: Top Song screen gets the #1 song
        val topSong = availableSongs.removeFirstOrNull()
        if (topSong != null) {
            pageToSongMap[PAGE_TOP_SONG] = topSong.id
            assignedIds.add(topSong.id)
        }


        // Rule 2: Top Artist screen
        val topArtist = topArtists.value.firstOrNull()
        if (topArtist != null) {
            val topArtistSong = databaseDao.getTopSongForArtist(topArtist.id, assignedIds).firstOrNull()
            if (topArtistSong != null) {
                pageToSongMap[PAGE_TOP_ARTIST] = topArtistSong.song.id
                assignedIds.add(topArtistSong.song.id)
                availableSongs.removeAll { it.id == topArtistSong.song.id }
            } else {
                val fallbackSong = availableSongs.removeFirstOrNull()
                if (fallbackSong != null) {
                    pageToSongMap[PAGE_TOP_ARTIST] = fallbackSong.id
                    assignedIds.add(fallbackSong.id)
                }
            }
        }

        // Assign remaining songs to other screens
        val songForTop5Songs = availableSongs.removeFirstOrNull()
        if (songForTop5Songs != null) {
            pageToSongMap[PAGE_TOP_5_SONGS] = songForTop5Songs.id
            assignedIds.add(songForTop5Songs.id)
        }

        val songForTop5Artists = availableSongs.removeFirstOrNull()
        if (songForTop5Artists != null) {
            pageToSongMap[PAGE_TOP_5_ARTISTS] = songForTop5Artists.id
            assignedIds.add(songForTop5Artists.id)
        }

        // Ensure other screens have no music
        (0..7).forEach { page ->
            if (page !in screensWithMusic) {
                pageToSongMap[page] = null
            }
        }
    }

    private suspend fun preloadSongUrls() {
        val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        if (connectivityManager == null) return

        val songIdsToPreload = pageToSongMap.values.filterNotNull().distinct()
        withContext(Dispatchers.IO) {
            val jobs = songIdsToPreload.map { songId ->
                async {
                    val url = YTPlayerUtils.playerResponseForPlayback(
                        videoId = songId,
                        audioQuality = AudioQuality.AUTO,
                        connectivityManager = connectivityManager
                    ).getOrNull()?.streamUrl
                    songId to url
                }
            }
            jobs.awaitAll().forEach { (songId, url) ->
                songUrlCache[songId] = url
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        audioController.setMute(_isMuted.value)
    }

    fun play() {
        audioController.play()
    }

    fun pause() {
        audioController.pause()
    }

    fun playTrackForPage(page: Int) {
        if (isLoading.value) return
        val songId = pageToSongMap[page]
        val streamUrl = songUrlCache[songId]
        audioController.load(streamUrl)
    }

    fun release() {
        audioController.release()
    }
}
