package com.metrolist.music.ui.screens.wrapped

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.music.R
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.material3.MaterialTheme
import com.metrolist.music.ui.theme.bbh_bartle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(navController: NavController) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { WrappedManager(getDatabaseDao(context), scope) }

    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val screens = listOf(
        "Welcome screen",
        "Minutes intro screen",
        "Minutes listened (Reveal)",
        "Minutes listened (Total)",
        "Amount of songs listened (all of them)",
        "Most listened song",
        "Most listened song listened amount",
        "List with top 5 most listened songs",
        "Playlist called \"Metrolist Wrapped\"",
        "Total amount of albums listened this year",
        "Most listened album",
        "Top 5 most listened albums",
        "Total listened artists amount",
        "Top 5 artists",
        "Most listened artist",
        "\"Thank you for using metrolist\" screen",
        "Goodbye screen"
    )
    val pagerState = rememberPagerState(pageCount = { screens.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Mute action */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.volume_up),
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> WrappedIntro {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = 1,
                            animationSpec = tween(durationMillis = 1000)
                        )
                    }
                }
                1 -> WrappedMinutesTease(manager = manager) {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = 2,
                            animationSpec = tween(durationMillis = 1000)
                        )
                    }
                }
                2 -> WrappedMinutesReveal(manager = manager)
                else -> WrappedPage(
                    name = screens[page],
                    number = page + 1,
                    offset = pagerState.currentPageOffsetFraction
                )
            }
        }
    }
}

@Composable
fun WrappedMinutesTease(
    manager: WrappedManager,
    onNavigateForward: () -> Unit
) {
    val messagePair by manager.messagePair.collectAsState()

    LaunchedEffect(Unit) {
        manager.loadData()
        delay(3500)
        onNavigateForward()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = messagePair != null,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(1000)
            )
        ) {
            Text(
                text = messagePair?.tease ?: "",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = try {
                    bbh_bartle
                } catch (e: Exception) {
                    FontFamily.Default
                }
            )
        }
    }
}

@Composable
fun WrappedMinutesReveal(manager: WrappedManager) {
    val messagePair by manager.messagePair.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = messagePair != null,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(1000)
            )
        ) {
            Text(
                text = messagePair?.reveal ?: "",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = try {
                    bbh_bartle
                } catch (e: Exception) {
                    FontFamily.Default
                }
            )
        }
    }
}

@Composable
fun WrappedPage(name: String, number: Int, offset: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Screen $number", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text(text = name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Offset: $offset", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
