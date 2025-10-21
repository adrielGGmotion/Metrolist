package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.BlacklistedArtist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlacklistSettingsViewModel @Inject constructor(
    private val database: MusicDatabase
) : ViewModel() {
    val blacklistedArtists = database.getBlacklistedArtists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun removeArtist(artist: BlacklistedArtist) {
        viewModelScope.launch {
            database.delete(artist)
        }
    }
}
