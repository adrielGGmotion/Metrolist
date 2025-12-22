package com.metrolist.music.ui.screens.wrapped.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun WrappedPage(name: String, number: Int, offset: Float) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Screen $number", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text(name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text("Offset: $offset", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
