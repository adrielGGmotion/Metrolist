package com.metrolist.music.wrapped

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
    private val playerConnectionManager: PlayerConnectionManager
) : ViewModel() {
    private val _wrappedData = MutableStateFlow<WrappedData?>(null)
    val wrappedData: StateFlow<WrappedData?> = _wrappedData

    private lateinit var audioManager: WrappedAudioManager

    init {
        viewModelScope.launch {
            val playerConnection = playerConnectionManager.playerConnection.first { it != null }
            audioManager = WrappedAudioManager(playerConnection, viewModelScope)
            _wrappedData.value = calculateWrappedData(database)
        }
    }

    fun playSong(songId: String) {
        if (::audioManager.isInitialized) {
            audioManager.play(songId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::audioManager.isInitialized) {
            audioManager.release()
        }
    }
}
