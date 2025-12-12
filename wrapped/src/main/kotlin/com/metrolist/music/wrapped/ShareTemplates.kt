package com.metrolist.music.wrapped

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.metrolist.music.R

val bbhBartle = FontFamily(Font(R.font.bbh_bartle))
val jost = FontFamily(Font(R.font.jost_400_book))

@Composable
fun StatStoryTemplate(stats: WrappedStats, stat: Stat) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (stat) {
                    Stat.MINUTES -> stats.totalMinutes.toString()
                    Stat.TOP_ARTIST -> stats.topArtists.first().name
                    Stat.TOP_ALBUM -> stats.topAlbum?.name ?: "N/A"
                },
                color = Color.White,
                fontSize = 120.sp,
                fontFamily = bbhBartle
            )
            Text(
                text = "2025",
                color = Color.White,
                fontSize = 80.sp,
                fontFamily = jost
            )
        }
    }
}

@Composable
fun ListStoryTemplate(stats: WrappedStats, listType: ListType) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val list = when (listType) {
                ListType.SONGS -> stats.topSongs.map { it.title }
                ListType.ARTISTS -> stats.topArtists.map { it.name }
            }
            list.forEach {
                Text(
                    text = it,
                    color = Color.White,
                    fontSize = 50.sp,
                    fontFamily = jost
                )
            }
        }
    }
}

@Composable
fun ReceiptTemplate(stats: WrappedStats) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your 2025 Wrapped",
                color = Color.White,
                fontSize = 80.sp,
                fontFamily = bbhBartle
            )
            Text(
                text = "Total Minutes: ${stats.totalMinutes}",
                color = Color.White,
                fontSize = 50.sp,
                fontFamily = jost
            )
            Text(
                text = "Top Songs:",
                color = Color.White,
                fontSize = 50.sp,
                fontFamily = jost
            )
            stats.topSongs.forEach {
                Text(
                    text = it.title,
                    color = Color.White,
                    fontSize = 40.sp,
                    fontFamily = jost
                )
            }
            Text(
                text = "Top Artists:",
                color = Color.White,
                fontSize = 50.sp,
                fontFamily = jost
            )
            stats.topArtists.forEach {
                Text(
                    text = it.name,
                    color = Color.White,
                    fontSize = 40.sp,
                    fontFamily = jost
                )
            }
            Text(
                text = "Top Album: ${stats.topAlbum?.name ?: "N/A"}",
                color = Color.White,
                fontSize = 50.sp,
                fontFamily = jost
            )
        }
    }
}
