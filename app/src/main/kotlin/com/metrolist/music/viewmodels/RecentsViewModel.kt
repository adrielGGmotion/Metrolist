package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.entities.InteractionHistory
import com.metrolist.music.db.entities.InteractionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val database: DatabaseDao
) : ViewModel() {
    private val recentInteractions: StateFlow<List<InteractionHistory>> =
        database.getInteractionHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    val recents: StateFlow<List<Any>> =
        recentInteractions.flatMapLatest { interactions ->
            combine(
                database.getSongsByIdsFlow(interactions.filter { it.type == InteractionType.SONG }.map { it.itemId }),
                database.getArtistsByIds(interactions.filter { it.type == InteractionType.ARTIST }.map { it.itemId }),
                database.getAlbumsByIds(interactions.filter { it.type == InteractionType.ALBUM }.map { it.itemId }),
                database.getPlaylistsByIds(interactions.filter { it.type == InteractionType.PLAYLIST }.map { it.itemId })
            ) { songs, artists, albums, playlists ->
                (songs + artists + albums + playlists).sortedByDescending {
                    when (it) {
                        is com.metrolist.music.db.entities.Song -> interactions.find { interaction -> interaction.itemId == it.id }?.timestamp
                        is com.metrolist.music.db.entities.Artist -> interactions.find { interaction -> interaction.itemId == it.id }?.timestamp
                        is com.metrolist.music.db.entities.Album -> interactions.find { interaction -> interaction.itemId == it.id }?.timestamp
                        is com.metrolist.music.db.entities.Playlist -> interactions.find { interaction -> interaction.itemId == it.id }?.timestamp
                        else -> null
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
}
