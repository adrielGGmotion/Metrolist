package com.metrolist.music.wrapped

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.music.LocalPlayerConnection
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedPager(
    userName: String,
    viewModel: WrappedViewModel = hiltViewModel()
) {
    val wrappedData by viewModel.wrappedData.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 6 })
    val volume by viewModel.volume.collectAsState()
    val animatedVolume by animateFloatAsState(
        targetValue = volume,
        animationSpec = tween(durationMillis = 500),
        label = "volume"
    )

    LaunchedEffect(animatedVolume) {
        viewModel.setVolume(animatedVolume)
    }

    val view = LocalView.current
    val playerConnection = LocalPlayerConnection.current

    if (!view.isInEditMode) {
        LaunchedEffect(Unit) {
            val window = (view.context as android.app.Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            playerConnection?.player?.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Re-enable decor fitting when leaving the Wrapped screen
            val window = (view.context as android.app.Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    LaunchedEffect(pagerState, wrappedData) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val songId = when (page) {
                    0 -> wrappedData?.topSongs?.randomOrNull()?.id
                    1 -> null // Continue playing current song
                    2 -> wrappedData?.topSongs?.getOrNull(1)?.id
                    3 -> wrappedData?.topArtists?.firstOrNull()?.let {
                        wrappedData?.topSongs?.firstOrNull { song ->
                            song.artists.any { artist -> artist.id == it.id }
                        }?.id
                    }
                    4 -> wrappedData?.topAlbumSongs?.firstOrNull()?.id
                    5 -> wrappedData?.topSongs?.firstOrNull()?.id
                    else -> null
                }
                songId?.let { viewModel.playSong(it) }
            }
    }

    VerticalPager(state = pagerState) { page ->
        val pageOffset = pagerState.currentPageOffsetFraction

        when (page) {
            0 -> PlaceholderSlide(color = Color(0xFF6A1B9A), text = "Intro: $userName", pagerState = pagerState, pageOffset = pageOffset)
            1 -> PlaceholderSlide(color = Color(0xFF4A148C), text = "Minutes", pagerState = pagerState, pageOffset = pageOffset)
            2 -> PlaceholderSlide(color = Color(0xFF311B92), text = "Genres", pagerState = pagerState, pageOffset = pageOffset)
            3 -> PlaceholderSlide(color = Color(0xFF1A237E), text = "Artists", pagerState = pagerState, pageOffset = pageOffset)
            4 -> PlaceholderSlide(color = Color(0xFF0D47A1), text = "Album", pagerState = pagerState, pageOffset = pageOffset)
            5 -> PlaceholderSlide(color = Color(0xFF01579B), text = "Top Song", pagerState = pagerState, pageOffset = pageOffset)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaceholderSlide(
    color: Color,
    text: String,
    pagerState: PagerState,
    pageOffset: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$text\nPage Offset: %.2f".format(pageOffset), color = Color.White)
    }
}
