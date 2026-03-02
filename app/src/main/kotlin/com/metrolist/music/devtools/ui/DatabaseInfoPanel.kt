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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.LocalDatabase
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DatabaseInfoPanel() {
    val database = LocalDatabase.current
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    
    val songCount by database.devCountSongs().collectAsState(initial = 0)
    val albumCount by database.devCountAlbums().collectAsState(initial = 0)
    val artistCount by database.devCountArtists().collectAsState(initial = 0)
    val playlistCount by database.devCountPlaylists().collectAsState(initial = 0)

    val dbName = remember { database.openHelper.databaseName ?: "Unknown" }
    val isDbOpen = remember { database.isOpen }

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
                    InfoRow(stringResource(R.string.database_name), dbName)
                    InfoRow(stringResource(R.string.status), if (isDbOpen) stringResource(R.string.dev_db_connected) else stringResource(R.string.dev_db_disconnected))
                }
            }
            item {
                InfoCard(title = stringResource(R.string.library_stats)) {
                    InfoRow(stringResource(R.string.songs), numberFormat.format(songCount))
                    InfoRow(stringResource(R.string.albums), numberFormat.format(albumCount))
                    InfoRow(stringResource(R.string.artists), numberFormat.format(artistCount))
                    InfoRow(stringResource(R.string.playlists), numberFormat.format(playlistCount))
                }
            }
        }
    }
}
