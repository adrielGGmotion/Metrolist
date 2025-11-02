package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.dao.BlacklistedArtistDao
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.BlacklistedArtist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongMenuViewModel @Inject constructor(
    private val blacklistedArtistDao: BlacklistedArtistDao
) : ViewModel() {
    fun blockArtist(artist: ArtistEntity) {
        viewModelScope.launch {
            blacklistedArtistDao.insert(
                BlacklistedArtist(
                    id = artist.id,
                    name = artist.name
                )
            )
        }
    }
}
