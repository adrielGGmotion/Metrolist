package com.metrolist.music.ui.screens.wrapped

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.db.DatabaseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongWithStats
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
}
