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
import android.util.Log
import com.metrolist.music.constants.ArtistSongSortType
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

    private val _trackMap = MutableStateFlow<Map<WrappedScreenType, String?>>(emptyMap())
    val trackMap = _trackMap.asStateFlow()

    private fun generateTrackMap() {
        scope.launch {
            val topSongs = _topSongs.value
            val topArtists = _topArtists.value

            if (topSongs.isEmpty()) {
                _trackMap.value = emptyMap()
                return@launch
            }

            val usedTrackIds = mutableSetOf<String>()
            val finalMap = mutableMapOf<WrappedScreenType, String?>()

            // Priority 1: Top Song
            val topSong = topSongs.first()
            usedTrackIds.add(topSong.id)
            finalMap[WrappedScreenType.TopSong] = topSong.id

            // Priority 2: Top Artist's song
            val topArtist = topArtists.firstOrNull()
            if (topArtist != null) {
                val topArtistTracks = databaseDao.artistSongs(
                    artistId = topArtist.artist.id,
                    sortType = ArtistSongSortType.PLAY_TIME,
                    descending = true
                ).first()
                val artistSong = topArtistTracks.firstOrNull { it.id !in usedTrackIds }
                if (artistSong != null) {
                    finalMap[WrappedScreenType.TopArtist] = artistSong.id
                    usedTrackIds.add(artistSong.id)
                } else {
                    // Fallback: if all of the artist's top songs are already used,
                    // just use their top song (which will be a repeat)
                    finalMap[WrappedScreenType.TopArtist] = topArtistTracks.firstOrNull()?.id
                }
            }

            // Priority 3: Top 5 Songs
            val top5Song = topSongs.firstOrNull { it.id !in usedTrackIds }
            if (top5Song != null) {
                finalMap[WrappedScreenType.Top5Songs] = top5Song.id
                usedTrackIds.add(top5Song.id)
            }

            // Priority 4: Top 5 Artists
            for (artist in topArtists.take(5)) {
                val artistTracks = databaseDao.artistSongs(
                    artistId = artist.artist.id,
                    sortType = ArtistSongSortType.PLAY_TIME,
                    descending = true
                ).first()
                val artistSong = artistTracks.firstOrNull { it.id !in usedTrackIds }
                if (artistSong != null) {
                    finalMap.putIfAbsent(WrappedScreenType.Top5Artists, artistSong.id)
                    usedTrackIds.add(artistSong.id)
                    break // Found a song for this screen, move on
                }
            }


            // Priority 5: Linked Screens (Minutes Tease/Reveal)
            var potentialFiller = topSongs.filter { it.id !in usedTrackIds }
            if (potentialFiller.isEmpty()) {
                potentialFiller = topSongs // Fallback: allow repeats if we run out
            }
            val minutesSong = potentialFiller.randomOrNull()
            if (minutesSong != null) {
                finalMap[WrappedScreenType.MinutesTease] = minutesSong.id
                finalMap[WrappedScreenType.MinutesReveal] = minutesSong.id
                usedTrackIds.add(minutesSong.id)
            }

            // Priority 6: Filler Screens (Intro/Outro)
            potentialFiller = topSongs.filter { it.id !in usedTrackIds }
            if (potentialFiller.isEmpty()) {
                potentialFiller = topSongs // Fallback: allow repeats
            }
            val introSong = potentialFiller.randomOrNull()
            if (introSong != null) {
                finalMap[WrappedScreenType.Welcome] = introSong.id
                usedTrackIds.add(introSong.id)
            }
            potentialFiller = topSongs.filter { it.id !in usedTrackIds }
            if (potentialFiller.isEmpty()) {
                potentialFiller = topSongs
            }
            val outroSong = potentialFiller.randomOrNull()
            if (outroSong != null) {
                finalMap[WrappedScreenType.End] = outroSong.id
                usedTrackIds.add(outroSong.id)
            }

            // Log the results for verification
            finalMap.forEach { (screen, songId) ->
                val songName = topSongs.find { it.id == songId }?.title ?: "N/A"
                Log.d("WrappedMap", "Screen: ${screen::class.simpleName}, Song: $songName (ID: $songId)")
            }

            _trackMap.value = finalMap
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
            generateTrackMap()
            _isLoading.value = false
        }
    }
}
