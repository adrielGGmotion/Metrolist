package com.metrolist.music.ui.screens.wrapped.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun WrappedTopArtistTease(
    onNavigateForward: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000) // 2-second delay before auto-navigating
        onNavigateForward()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "And your top artist is...",
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
