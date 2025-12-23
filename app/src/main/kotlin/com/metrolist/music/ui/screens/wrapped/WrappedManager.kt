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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random

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

            _topSongs.value = databaseDao.mostPlayedSongsStats(fromTimestamp, limit = 5).first()
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

    fun playTrackForPage(page: Int) {
        if (isLoading.value) return

        val songIdToPlay = when (page) {
            WrappedConstants.PAGE_TOP_SONG, WrappedConstants.PAGE_TOP_ARTIST -> topSongs.value.firstOrNull()?.id
            WrappedConstants.PAGE_TOP_5_ARTISTS -> {
                if (topArtists.value.size > 1) {
                    // We can't get the artistId from SongWithStats, so this logic is flawed.
                    // Instead, we'll just play a random song from the top 5, excluding the first.
                    topSongs.value.getOrNull(Random.nextInt(1, topSongs.value.size))?.id
                } else {
                    topSongs.value.firstOrNull()?.id // Fallback if only one top artist
                }
            }
            else -> topSongs.value.firstOrNull()?.id
        }
        audioController.load(songIdToPlay)
    }

    fun release() {
        audioController.release()
    }
}
