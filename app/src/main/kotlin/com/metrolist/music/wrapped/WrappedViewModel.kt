package com.metrolist.music.wrapped

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WrappedViewModel @Inject constructor(
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _wrappedData = MutableStateFlow<WrappedData?>(null)
    val wrappedData: StateFlow<WrappedData?> = _wrappedData

    private val audioManager = WrappedAudioManager(context, viewModelScope)
    val volume: StateFlow<Float> = audioManager.volume

    init {
        viewModelScope.launch {
            _wrappedData.value = calculateWrappedData(database)
        }
    }

    fun playSong(songId: String) {
        audioManager.play(songId)
    }

    fun setVolume(volume: Float) {
        audioManager.setVolume(volume)
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
