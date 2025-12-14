package com.metrolist.music.wrapped

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.music.ui.theme.BartleFontFamily
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedPager(
    userName: String,
    viewModel: WrappedViewModel = hiltViewModel()
) {
    val wrappedData by viewModel.wrappedData.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 6 })
    val view = LocalView.current

    if (!view.isInEditMode) {
        LaunchedEffect(Unit) {
            val window = (view.context as android.app.Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
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
                viewModel.playSongForPage(page)
            }
    }

    VerticalPager(state = pagerState) { page ->
        val pageOffset = pagerState.currentPageOffsetFraction

        when (page) {
            0 -> IntroSlide(isVisible = pagerState.currentPage == 0 && pageOffset == 0f)
            1 -> MinutesSlide(
                isVisible = pagerState.currentPage == 1 && pageOffset == 0f,
                totalMinutes = wrappedData?.totalMinutes ?: 0
            )
            2 -> PlaceholderSlide(color = Color(0xFF311B92), text = "Genres", pagerState = pagerState, pageOffset = pageOffset)
            3 -> PlaceholderSlide(color = Color(0xFF1A237E), text = "Artists", pagerState = pagerState, pageOffset = pageOffset)
            4 -> PlaceholderSlide(color = Color(0xFF0D47A1), text = "Album", pagerState = pagerState, pageOffset = pageOffset)
            5 -> PlaceholderSlide(color = Color(0xFF01579B), text = "Top Song", pagerState = pagerState, pageOffset = pageOffset)
        }
    }
}

@Composable
fun IntroSlide(isVisible: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        KineticText(
            text = "The stage is set...",
            isVisible = isVisible,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = BartleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White
            )
        )
    }
}

@Composable
fun MinutesSlide(isVisible: Boolean, totalMinutes: Int) {
    var animatedMinutes by remember { mutableStateOf(0) }
    val animatedMinutesState by animateIntAsState(
        targetValue = animatedMinutes,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            animatedMinutes = totalMinutes
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4A148C)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KineticText(
                text = "You've listened to",
                isVisible = isVisible,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = BartleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$animatedMinutesState",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = BartleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            KineticText(
                text = "minutes of music",
                isVisible = isVisible,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = BartleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
            )
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
