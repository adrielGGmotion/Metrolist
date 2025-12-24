package com.metrolist.music.ui.screens.wrapped

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.screens.wrapped.pages.WrappedIntro
import com.metrolist.music.ui.screens.wrapped.pages.WrappedMinutesScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedMinutesTease
import com.metrolist.music.ui.screens.wrapped.pages.WrappedEndScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTop5ArtistsScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTop5SongsScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTopArtistScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTopSongScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(navController: NavController) {
    val messagePairSaver = Saver<MessagePair, List<Any>>(
        save = { listOf(it.range.first, it.range.last, it.tease, it.reveal) },
        restore = {
            MessagePair(
                range = (it[0] as Long)..(it[1] as Long),
                tease = it[2] as String,
                reveal = it[3] as String
            )
        }
    )
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { WrappedManager(context, getDatabaseDao(context), scope) }

    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { insetsController.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val screens = remember {
        listOf(
            "Welcome", "Minutes Tease", "Minutes Reveal", "Top Song", "Top 5 Songs",
            "Top Artist", "Top 5 Artists", "End & Playlist"
        )
    }
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val totalMinutes by manager.totalMinutes.collectAsState(initial = 0L)
    val isLoading by manager.isLoading.collectAsState()
    val accountInfo by manager.accountInfo.collectAsState()
    val topSongs by manager.topSongs.collectAsState()
    val topArtists by manager.topArtists.collectAsState()
    var playlist by remember { mutableStateOf<Map<Int, String?>>(emptyMap()) }

    LaunchedEffect(topSongs, topArtists) {
        if (topSongs.isNotEmpty() && topArtists.isNotEmpty()) {
            playlist = manager.generatePlaylistMap(topSongs, topArtists)
            manager.prepareAudio(playlist)
        }
    }

    DisposableEffect(manager) {
        onDispose {
            manager.releaseAudio()
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            manager.onPageChanged(page)
        }
    }

    val messagePair = rememberSaveable(totalMinutes, saver = messagePairSaver) {
        WrappedRepository.getMessage(totalMinutes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(R.drawable.arrow_back), "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Mute action */ }) {
                        Icon(painterResource(R.drawable.volume_up), "Mute", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(paddingValues)) { page ->
            when (page) {
                0 -> WrappedIntro { scope.launch { pagerState.animateScrollToPage(page = 1) } }
                1 -> WrappedMinutesTease(
                    messagePair = messagePair,
                    onNavigateForward = { scope.launch { pagerState.animateScrollToPage(page = 2) } },
                    manager = manager,
                    isLoading = isLoading
                )
                2 -> WrappedMinutesScreen(
                    messagePair = messagePair, totalMinutes = totalMinutes,
                    isVisible = pagerState.currentPage == 2
                )
                3 -> WrappedTopSongScreen(
                    topSong = topSongs.firstOrNull(),
                    isVisible = pagerState.currentPage == 3
                )
                4 -> WrappedTop5SongsScreen(
                    topSongs = topSongs,
                    isVisible = pagerState.currentPage == 4
                )
                5 -> WrappedTopArtistScreen(
                    topArtist = topArtists.firstOrNull(),
                    isVisible = pagerState.currentPage == 5
                )
                6 -> WrappedTop5ArtistsScreen(
                    topArtists = topArtists,
                    isVisible = pagerState.currentPage == 6
                )
                7 -> WrappedEndScreen()
            }
        }
    }
}
