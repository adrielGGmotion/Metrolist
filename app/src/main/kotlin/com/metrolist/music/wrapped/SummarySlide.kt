package com.metrolist.music.wrapped

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SummarySlide(wrappedData: WrappedData) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Your 2025 Wrapped")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Total Minutes: ${wrappedData.totalMinutes}")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Top Artists:")
            wrappedData.topArtists.forEach { artist ->
                Text(text = artist.name)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Top Songs:")
            wrappedData.topSongs.forEach { song ->
                Text(text = song.title)
            }
        }
    }
}
