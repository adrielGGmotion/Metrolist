package com.metrolist.music.ui.screens.wrapped.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.screens.wrapped.WrappedManager
@Composable
fun WrappedTopSongTease(
    onNavigateForward: () -> Unit,
    manager: WrappedManager,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        com.metrolist.music.ui.screens.wrapped.components.PrimaryButton(
            onClick = onNavigateForward,
            isLoading = isLoading,
            text = "Reveal your top song"
        )
    }
}
