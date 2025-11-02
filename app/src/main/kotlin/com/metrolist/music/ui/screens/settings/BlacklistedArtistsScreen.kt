package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.db.entities.BlacklistedArtist
import com.metrolist.music.viewmodels.BlacklistedArtistsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistedArtistsScreen(
    navController: NavController,
    viewModel: BlacklistedArtistsViewModel = hiltViewModel()
) {
    val blacklistedArtists by viewModel.blacklistedArtists.collectAsState()

    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.blacklisted_artists)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            }
        )
        LazyColumn {
            items(blacklistedArtists) { artist ->
                BlacklistedArtistItem(
                    artist = artist,
                    onUnblock = { viewModel.unblockArtist(artist) }
                )
            }
        }
    }
}

@Composable
fun BlacklistedArtistItem(
    artist: BlacklistedArtist,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = artist.name, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = onUnblock) {
            Text(text = stringResource(R.string.unblock))
        }
    }
}
