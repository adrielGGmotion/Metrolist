package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.viewmodels.BlacklistSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistSettings(
    navController: NavController,
    viewModel: BlacklistSettingsViewModel = hiltViewModel()
) {
    val blacklistedArtists by viewModel.blacklistedArtists.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.blacklist)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(blacklistedArtists) { artist ->
                PreferenceEntry(
                    title = { Text(artist.name) },
                    icon = {
                        IconButton(onClick = { viewModel.removeArtist(artist) }) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = null
                            )
                        }
                    },
                    onClick = {}
                )
            }
        }
    }
}
