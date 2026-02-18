package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SpeedDialGridItem(
    item: YTItem,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square aspect ratio
            .clip(RoundedCornerShape(ThumbnailCornerRadius))
    ) {
        // Thumbnail
        ItemThumbnail(
            thumbnailUrl = item.thumbnail,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius),
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Overlay for Text Readability and Icon Contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f), // Top scrim for icon visibility on bright covers
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Title and Chevron
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp) // Reduced padding for tighter layout
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall, // Smaller, punchier font
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Navigation Chevron for browsable items (Album, Playlist, Artist)
            if (item !is SongItem) {
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Pinned Icon
        if (isPinned) {
            Icon(
                painter = painterResource(R.drawable.ic_push_pin),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(16.dp)
            )
        }

        // Play Button Overlay logic (optional, reuse from YouTubeGridItem logic if needed)
        // if (item is SongItem && !isActive) {
        //      OverlayPlayButton(visible = true)
        // }

        // Album Play handling
        if (item is AlbumItem && !isActive) {
             AlbumPlayButton(
                visible = false, // Hidden to match screenshot style
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        var albumWithSongs = database.albumWithSongs(item.id).first()
                        if (albumWithSongs?.songs.isNullOrEmpty()) {
                            YouTube.album(item.id).onSuccess { albumPage ->
                                database.transaction { insert(albumPage) }
                                albumWithSongs = database.albumWithSongs(item.id).first()
                            }.onFailure { reportException(it) }
                        }
                        albumWithSongs?.let {
                            withContext(Dispatchers.Main) {
                                playerConnection.playQueue(LocalAlbumRadio(it))
                            }
                        }
                    }
                }
            )
        }
    }
}
