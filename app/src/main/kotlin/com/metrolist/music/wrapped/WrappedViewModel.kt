package com.metrolist.music.wrapped

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.playback.PlayerConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WrappedViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val playerConnectionManager: PlayerConnectionManager,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    private val _wrappedData = MutableStateFlow<WrappedData?>(null)
    val wrappedData: StateFlow<WrappedData?> = _wrappedData

    private lateinit var audioManager: WrappedAudioManager
    private val playedVideoIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            val playerConnection = playerConnectionManager.playerConnection.first { it != null }
            audioManager = WrappedAudioManager(playerConnection, viewModelScope, dataStore)
            _wrappedData.value = calculateWrappedData(database)
        }
    }

    fun playSongForPage(page: Int) {
        if (!::audioManager.isInitialized) return

        val songId = when (page) {
            0 -> _wrappedData.value?.topSongs?.randomOrNull()?.id
            1 -> null // Continue playing current song
            2 -> {
                val topSong = _wrappedData.value?.topSongs?.getOrNull(1)
                if (playedVideoIds.contains(topSong?.id)) {
                    _wrappedData.value?.topSongs?.getOrNull(2)?.id
                } else {
                    topSong?.id
                }
            }
            3 -> {
                val topArtist = _wrappedData.value?.topArtists?.firstOrNull()
                val topArtistSong = _wrappedData.value?.topSongs?.firstOrNull { song ->
                    song.artists.any { artist -> artist.id == topArtist?.id }
                }
                if (playedVideoIds.contains(topArtistSong?.id)) {
                    _wrappedData.value?.topSongs?.firstOrNull { song ->
                        song.artists.any { artist -> artist.id == topArtist?.id } && !playedVideoIds.contains(song.id)
                    }?.id
                } else {
                    topArtistSong?.id
                }
            }
            4 -> {
                val topAlbumSong = _wrappedData.value?.topAlbumSongs?.firstOrNull()
                if (playedVideoIds.contains(topAlbumSong?.id)) {
                    _wrappedData.value?.topAlbumSongs?.getOrNull(1)?.id
                } else {
                    topAlbumSong?.id
                }
            }
            5 -> _wrappedData.value?.topSongs?.firstOrNull()?.id
            else -> null
        }

        songId?.let {
            audioManager.play(it)
            playedVideoIds.add(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::audioManager.isInitialized) {
            audioManager.release()
        }
    }
}
