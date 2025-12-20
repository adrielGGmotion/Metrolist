package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.component.AlbumListItem
import com.metrolist.music.ui.component.ArtistListItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.viewmodels.RecentsViewModel

@Composable
fun RecentsScreen(
    navController: NavController,
    viewModel: RecentsViewModel = hiltViewModel()
) {
    val recents by viewModel.recents.collectAsState()
    val playerConnection = LocalPlayerConnection.current ?: return

    LazyColumn {
        items(recents) { item ->
            when (item) {
                is Song -> {
                    SongListItem(
                        song = item,
                        onClick = {
                            playerConnection.playQueue(ListQueue(items = listOf(item.toMediaItem())))
                        }
                    )
                }
                is Artist -> {
                    ArtistListItem(
                        artist = item,
                        onClick = {
                            navController.navigate("artist/${item.id}")
                        }
                    )
                }
                is Album -> {
                    AlbumListItem(
                        album = item,
                        onClick = {
                            navController.navigate("album/${item.id}")
                        }
                    )
                }
                is Playlist -> {
                    PlaylistListItem(
                        playlist = item,
                        onClick = {
                            navController.navigate("playlist/${item.id}")
                        }
                    )
                }
            }
        }
    }
}
