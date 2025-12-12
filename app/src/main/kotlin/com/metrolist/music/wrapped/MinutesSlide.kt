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
import androidx.compose.ui.unit.sp

@Composable
fun MinutesSlide(totalMinutes: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = totalMinutes.toString(), fontSize = 72.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    totalMinutes < 500 -> "You're not that music fan... Are you?"
                    totalMinutes > 10000 -> "It's been a rough year, you've been listening for 10,000 Minutes, that's a lot!"
                    else -> "It's been a while since you've been using Metrolist..."
                }
            )
        }
    }
}
