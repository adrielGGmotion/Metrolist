package com.metrolist.music.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.dao.BlacklistedArtistDao
import com.metrolist.music.db.entities.BlacklistedArtist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlacklistedArtistsViewModel @Inject constructor(
    private val blacklistedArtistDao: BlacklistedArtistDao
) : ViewModel() {
    val blacklistedArtists = blacklistedArtistDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun unblockArtist(artist: BlacklistedArtist) {
        viewModelScope.launch {
            blacklistedArtistDao.delete(artist)
        }
    }
}
