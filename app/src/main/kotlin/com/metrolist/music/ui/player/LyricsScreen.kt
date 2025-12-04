package com.metrolist.music.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.Lyrics

@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    navController: NavController,
    backgroundAlpha: Float
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    Box(modifier = Modifier.fillMaxSize()) {
        Lyrics(
            sliderPositionProvider = { playerConnection.player.currentPosition },
            modifier = Modifier.padding(horizontal = 24.dp),
            showLyrics = true
        )
    }
}
