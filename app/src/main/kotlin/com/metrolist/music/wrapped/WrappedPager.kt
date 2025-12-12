package com.metrolist.music.wrapped

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedPager(
    userName: String,
    viewModel: WrappedViewModel = hiltViewModel()
) {
    val wrappedData by viewModel.wrappedData.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 6 })

    HorizontalPager(state = pagerState) { page ->
        when (page) {
            0 -> IntroSlide(userName = userName)
            1 -> wrappedData?.let { MinutesSlide(totalMinutes = it.totalMinutes) }
            2 -> wrappedData?.let { TopArtistsSlide(artists = it.topArtists) }
            3 -> wrappedData?.let { TopAlbumSlide(album = it.topAlbum) }
            4 -> wrappedData?.let { TopSongsSlide(songs = it.topSongs) }
            5 -> wrappedData?.let { SummarySlide(wrappedData = it) }
            else -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Slide ${page + 1}")
            }
        }
    }
}

@Composable
fun IntroSlide(userName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "The stage is set, $userName.")
    }
}
