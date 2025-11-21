package com.metrolist.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.menu.LyricsMenu
import kotlinx.coroutines.delay

@Composable
fun LyricsScreen(
    onClose: () -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    var position by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            position = playerConnection.player.currentPosition
            delay(500)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Lyrics(
            sliderPositionProvider = { position },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = { onClose() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Close"
            )
        }
        IconButton(
            onClick = {
                menuState.show {
                    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
                    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
                    LyricsMenu(
                        lyricsProvider = { currentLyrics },
                        songProvider = { currentSong as? com.metrolist.music.db.entities.SongEntity },
                        mediaMetadataProvider = { mediaMetadata },
                        onDismiss = menuState::dismiss
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription = "More"
            )
        }
    }
}
