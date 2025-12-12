package com.metrolist.music.wrapped

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem

@Composable
fun TopSongsLayout(songs: List<SongItem>) {
    var showList by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (showList) 0.5f else 1f,
        animationSpec = tween(500)
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showList = true
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (songs.isNotEmpty()) {
            AsyncImage(
                model = songs.first().thumbnail,
                contentDescription = null,
                modifier = Modifier.size(300.dp).scale(scale)
            )
        }
        if (showList) {
            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                songs.forEach {
                    Text(it.title)
                }
            }
        }
    }
}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopAlbumsLayout(albums: List<AlbumItem>) {
    FlowRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        albums.forEachIndexed { index, album ->
            AsyncImage(
                model = album.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(if (index == 0) 0.6f else 0.4f)
            )
        }
    }
}

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

@Composable
fun TopArtistsLayout(artists: List<ArtistItem>) {
    var showList by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (showList) 0.5f else 1f,
        animationSpec = tween(500)
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showList = true
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (artists.isNotEmpty()) {
            AsyncImage(
                model = artists.first().thumbnail,
                contentDescription = null,
                modifier = Modifier.size(300.dp).scale(scale)
            )
        }
        if (showList) {
            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                artists.forEach {
                    Row {
                        AsyncImage(
                            model = it.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(it.name)
                    }
                }
            }
        }
    }
}
