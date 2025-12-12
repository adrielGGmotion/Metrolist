package com.metrolist.music.wrapped

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.HistoryPage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.EventWithSong
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class WrappedViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    private val _youtubeHistory = MutableStateFlow<HistoryPage?>(null)
    val youtubeHistory = _youtubeHistory.asStateFlow()

    private val _wrappedStats = MutableStateFlow<WrappedStats?>(null)
    val wrappedStats = _wrappedStats.asStateFlow()

    val localHistory: StateFlow<List<EventWithSong>> = flow {
        val startOfYear = LocalDateTime.of(2025, 1, 1, 0, 0)
        val endOfYear = LocalDateTime.of(2025, 12, 31, 23, 59)
        database.events().collect { events ->
            emit(events.filter { it.event.timestamp in startOfYear..endOfYear })
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        fetchYouTubeHistory()
        viewModelScope.launch {
            localHistory.collect {
                calculateStats()
            }
        }
        viewModelScope.launch {
            youtubeHistory.collect {
                calculateStats()
            }
        }
    }

    fun savePlaylist() {
        viewModelScope.launch(Dispatchers.IO) {
            val playlistId = YouTube.createPlaylist("Your Top Songs 2025")
            if (playlistId != null) {
                wrappedStats.value?.topSongs?.forEach {
                    YouTube.addToPlaylist(playlistId, it.id)
                }
            }
        }
    }

    private fun calculateStats() {
        val localPlays = localHistory.value.map {
            Triple(it.song, it.event.playTime, 1)
        }

        val remotePlays = youtubeHistory.value?.sections?.flatMap { section ->
            section.songs.map { song ->
                Triple(song, song.duration ?: 0L, 1)
            }
        } ?: emptyList()

        val allPlays = (localPlays.map { (song, playTime, _) ->
            val songItem = com.metrolist.innertube.models.SongItem(
                id = song.id,
                title = song.song.title,
                artists = song.artists.map { com.metrolist.innertube.models.ArtistItem(name = it.name, id = it.id) },
                album = song.album?.let { com.metrolist.innertube.models.AlbumItem(name = it.title, id = it.id, browseId = it.id, playlistId = "") },
                duration = song.song.duration,
                thumbnail = song.song.thumbnailUrl,
                explicit = song.song.explicit
            )
            songItem to playTime
        } + remotePlays.map { (song, playTime, _) ->
            song to playTime
        }).groupBy { it.first.id }
            .map { (_, pairs) ->
                val song = pairs.first().first
                val totalPlayTime = pairs.sumOf { it.second }
                song to totalPlayTime
            }

        val totalMinutes = allPlays.sumOf { it.second } / 60000

        val topSongs = allPlays
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }

        val topArtists = allPlays
            .flatMap { (song, playTime) -> song.artists.map { it to playTime } }
            .groupBy { it.first.id }
            .map { (artistId, pairs) ->
                val totalPlayTime = pairs.sumOf { it.second }
                val artist = pairs.first().first
                artist to totalPlayTime
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }

        val topAlbum = allPlays
            .filter { it.first.album != null }
            .groupBy { it.first.album!!.id }
            .map { (albumId, pairs) ->
                val totalPlayTime = pairs.sumOf { it.second }
                val album = pairs.first().first.album
                album to totalPlayTime
            }
            .maxByOrNull { it.second }
            ?.first

        _wrappedStats.value = WrappedStats(
            totalMinutes = totalMinutes,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbum = topAlbum,
        )
    }

    private fun fetchYouTubeHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.musicHistory().onSuccess {
                _youtubeHistory.value = it
            }.onFailure {
                reportException(it)
            }
        }
    }
}
