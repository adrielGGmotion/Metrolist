/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.LocalDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import timber.log.Timber

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DatabaseInfoPanel() {
    val database = LocalDatabase.current
    
    var songCount by remember { mutableIntStateOf(0) }
    var artistCount by remember { mutableIntStateOf(0) }
    var albumCount by remember { mutableIntStateOf(0) }
    var playlistCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        try {
            coroutineScope {
                val songs = async { database.devCountSongs().first() }
                val albums = async { database.devCountAlbums().first() }
                val artists = async { database.devCountArtists().first() }
                val playlists = async { database.devCountPlaylists().first() }
                
                songCount = songs.await()
                albumCount = albums.await()
                artistCount = artists.await()
                playlistCount = playlists.await()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load Database stats for DevTools")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(
            title = stringResource(R.string.database),
            subtitle = stringResource(R.string.database_subtitle)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoCard(title = stringResource(R.string.connection)) {
                    InfoRow(stringResource(R.string.database_name), stringResource(R.string.dev_db_name))
                    InfoRow(stringResource(R.string.status), stringResource(R.string.dev_db_connected))
                }
            }
            item {
                InfoCard(title = stringResource(R.string.library_stats)) {
                    InfoRow(stringResource(R.string.songs), songCount.toString())
                    InfoRow(stringResource(R.string.albums), albumCount.toString())
                    InfoRow(stringResource(R.string.artists), artistCount.toString())
                    InfoRow(stringResource(R.string.playlists), playlistCount.toString())
                }
            }
        }
    }
}
