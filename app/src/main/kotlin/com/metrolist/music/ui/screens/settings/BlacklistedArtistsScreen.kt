package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.ListItem
import com.metrolist.music.viewmodels.BlacklistedArtistsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistedArtistsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val viewModel: BlacklistedArtistsViewModel = hiltViewModel()
    val blacklistedArtists by viewModel.blacklistedArtists.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.blacklisted_artists)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        onLongClick = { /*TODO*/ }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn {
                items(blacklistedArtists) { artist ->
                    ListItem(
                        headlineContent = { Text(artist.name) },
                        trailingContent = {
                            Button(onClick = { viewModel.unblockArtist(artist) }) {
                                Text(stringResource(R.string.unblock))
                            }
                        }
                    )
                }
            }
        }
    }
}
