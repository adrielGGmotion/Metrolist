package com.metrolist.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.SongItem

@Composable
fun ArtistSongCard(
    song: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        YouTubeListItem(
            item = song,
            isActive = isActive,
            isPlaying = isPlaying,
            trailingContent = trailingContent
        )
    }
}
