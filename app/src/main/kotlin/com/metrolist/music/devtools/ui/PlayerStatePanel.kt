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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerStatePanel() {
    val playerConnection = LocalPlayerConnection.current

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(
            title = stringResource(R.string.dev_playback),
            subtitle = stringResource(R.string.dev_playback_subtitle)
        )

        if (playerConnection == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                item { 
                    InfoCard(title = stringResource(R.string.dev_player_status)) {
                        InfoRow(stringResource(R.string.dev_player_service), stringResource(R.string.dev_player_not_initialized)) 
                    }
                }
            }
            return
        }

        val playbackState by playerConnection.playbackState.collectAsState()
        val isPlaying by playerConnection.isPlaying.collectAsState()
        val error by playerConnection.error.collectAsState()
        val currentSong by playerConnection.currentSong.collectAsState(initial = null)
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoCard(title = stringResource(R.string.dev_engine_state)) {
                    InfoRow(stringResource(R.string.dev_service_ready), playerConnection.service.isPlayerReady.value.toString())
                    InfoRow(stringResource(R.string.dev_playback_state), playbackState.toString())
                    InfoRow(stringResource(R.string.dev_is_playing), isPlaying.toString())
                    InfoRow(stringResource(R.string.dev_active_error), error?.message ?: stringResource(R.string.dev_none))
                }
            }
            item {
                InfoCard(title = stringResource(R.string.dev_current_media)) {
                    InfoRow(stringResource(R.string.dev_media_id), currentSong?.id ?: stringResource(R.string.dev_none))
                    InfoRow(stringResource(R.string.dev_title), currentSong?.title ?: stringResource(R.string.dev_none))
                    TimelineRow(playerConnection)
                }
            }
        }
    }
}

@Composable
fun TimelineRow(playerConnection: com.metrolist.music.playback.PlayerConnection) {
    var currentPosition by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var duration by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var queueSize by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
            queueSize = playerConnection.player.mediaItemCount
            kotlinx.coroutines.delay(1000L)
        }
    }
    
    InfoRow(stringResource(R.string.dev_queue_size), queueSize.toString())
    InfoRow(stringResource(R.string.dev_timeline), stringResource(R.string.dev_timeline_format, currentPosition, duration))
}
