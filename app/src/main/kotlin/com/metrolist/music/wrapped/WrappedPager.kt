package com.metrolist.music.wrapped

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            0 -> IntroSlide(pagerState = pagerState)
            1 -> MinutesSlide(
                pagerState = pagerState,
                totalMinutes = wrappedData?.totalMinutes ?: 0
            )
            2 -> GenreSlide(
                pagerState = pagerState,
                topGenre = wrappedData?.topGenre ?: "N/A"
            )
            3 -> PlaceholderSlide(color = Color(0xFF1A237E), text = "Artists", pagerState = pagerState, pageOffset = pageOffset)
            4 -> PlaceholderSlide(color = Color(0xFF0D47A1), text = "Album", pagerState = pagerState, pageOffset = pageOffset)
            5 -> PlaceholderSlide(color = Color(0xFF01579B), text = "Top Song", pagerState = pagerState, pageOffset = pageOffset)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroSlide(pagerState: PagerState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = pagerState.currentPage == 0,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        ) {
            Text(text = "The stage is set...", color = Color.White)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinutesSlide(pagerState: PagerState, totalMinutes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4A148C)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedCounter(
                targetValue = totalMinutes,
                isVisible = pagerState.currentPage == 1
            )
            Text(
                text = when {
                    totalMinutes < 1000 -> "Warming up?"
                    totalMinutes > 10000 -> "Music is your oxygen."
                    else -> "Minutes Listened"
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AnimatedCounter(targetValue: Int, isVisible: Boolean) {
    val counter = remember { Animatable(0f) }

    LaunchedEffect(isVisible, targetValue) {
        if (isVisible) {
            counter.animateTo(
                targetValue = targetValue.toFloat(),
                animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing)
            )
        }
    }

    Text(
        text = "%,d".format(counter.value.toInt()),
        color = Color.White,
        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenreSlide(pagerState: PagerState, topGenre: String) {
    val isVisible = pagerState.currentPage == 2
    val scale = remember { Animatable(0f) }
    val line1OffsetX = remember { Animatable(-1000f) }
    val line2OffsetX = remember { Animatable(1000f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            line1OffsetX.animateTo(0f, animationSpec = tween(800))
            line2OffsetX.animateTo(0f, animationSpec = tween(800))
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF311B92)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = line1OffsetX.value.dp)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .offset(x = line2OffsetX.value.dp)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                )
        )
        Text(
            text = topGenre,
            color = Color.White,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.scale(scale.value)
        )
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
