/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.DisposableEffect
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.MediaItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.metrolist.music.devtools.ui.InfoCard
import com.metrolist.music.devtools.ui.InfoRow
import kotlinx.coroutines.delay

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
            item {
                InfoCard(title = stringResource(R.string.dev_background_processes)) {
                    InfoRow(stringResource(R.string.dev_discord_rpc), (playerConnection.service.discordRpc != null).toString())
                    InfoRow(stringResource(R.string.dev_scrobbling), (playerConnection.service.scrobbleManager != null).toString())
                    InfoRow(stringResource(R.string.dev_crossfade_enabled), playerConnection.service.crossfadeEnabled.toString())
                    InfoRow(stringResource(R.string.dev_loudness_enhancer), (playerConnection.service.loudnessEnhancer != null).toString())
                    InfoRow(stringResource(R.string.dev_sleep_timer_active), playerConnection.service.sleepTimer.isActive.toString())
                }
            }
            item {
                InfoCard(title = stringResource(R.string.dev_listen_together)) {
                    val ltManager = playerConnection.service.listenTogetherManager
                    val roomState by ltManager.roomState.collectAsState()
                    val role by ltManager.role.collectAsState()
                    val connectionState by ltManager.connectionState.collectAsState()
                    
                    val roomCode = roomState?.roomCode ?: stringResource(R.string.dev_none)
                    
                    InfoRow(stringResource(R.string.dev_listen_together_status), connectionState.name)
                    InfoRow(stringResource(R.string.dev_listen_together_room), roomCode)
                    InfoRow(stringResource(R.string.dev_listen_together_role), role.name)
                }
            }
            item {
                InfoCard(title = stringResource(R.string.dev_queue_viewer)) {
                    QueueViewerRow(playerConnection)
                }
            }
        }
    }
}

@Composable
fun QueueViewerRow(playerConnection: com.metrolist.music.playback.PlayerConnection) {
    var upcomingItems by remember { androidx.compose.runtime.mutableStateOf<List<String>>(emptyList()) }
    
    val fallbackUnknown = stringResource(R.string.dev_unknown)
    val updateQueue = {
        val player = playerConnection.player
        val currentIndex = player.currentMediaItemIndex
        val items = mutableListOf<String>()
        for (i in 1..5) {
            val index = currentIndex + i
            if (index < player.mediaItemCount) {
                val item = player.getMediaItemAt(index)
                items.add(item.mediaMetadata.title?.toString() ?: fallbackUnknown)
            }
        }
        upcomingItems = items
    }

    DisposableEffect(playerConnection.player) {
        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                updateQueue()
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateQueue()
            }
        }
        playerConnection.player.addListener(listener)
        updateQueue()
        onDispose { playerConnection.player.removeListener(listener) }
    }
    
    if (upcomingItems.isEmpty()) {
        InfoRow(stringResource(R.string.dev_queue_viewer_subtitle), stringResource(R.string.dev_empty))
    } else {
        InfoRow(stringResource(R.string.dev_queue_viewer_subtitle), "")
        upcomingItems.forEachIndexed { index, title ->
            InfoRow("+${index + 1}", title)
        }
    }
}

@Composable
fun TimelineRow(playerConnection: com.metrolist.music.playback.PlayerConnection) {
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var queueSize by remember { mutableIntStateOf(0) }
    val isPlaying by playerConnection.isPlaying.collectAsState()
    
    DisposableEffect(playerConnection.player) {
        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                duration = playerConnection.player.duration
                queueSize = playerConnection.player.mediaItemCount
            }
            override fun onPlaybackStateChanged(state: Int) {
                duration = playerConnection.player.duration
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                currentPosition = playerConnection.player.currentPosition
            }
        }
        playerConnection.player.addListener(listener)
        currentPosition = playerConnection.player.currentPosition
        duration = playerConnection.player.duration
        queueSize = playerConnection.player.mediaItemCount
        
        onDispose { playerConnection.player.removeListener(listener) }
    }
    
    LaunchedEffect(isPlaying, playerConnection.player) {
        if (isPlaying) {
            while (isPlaying) {
                currentPosition = playerConnection.player.currentPosition
                delay(1000L)
            }
        } else {
            currentPosition = playerConnection.player.currentPosition
        }
    }
    
    val posFormatted = if (currentPosition == C.TIME_UNSET) 0L else currentPosition
    val durFormatted = if (duration == C.TIME_UNSET) 0L else duration

    InfoRow(stringResource(R.string.dev_queue_size), queueSize.toString())
    InfoRow(stringResource(R.string.dev_timeline), stringResource(R.string.dev_timeline_format, posFormatted, durFormatted))
}
