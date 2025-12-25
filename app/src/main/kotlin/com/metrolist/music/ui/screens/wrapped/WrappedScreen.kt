package com.metrolist.music.ui.screens.wrapped

import android.net.ConnectivityManager
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.screens.wrapped.pages.WrappedEndScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedIntro
import com.metrolist.music.ui.screens.wrapped.pages.WrappedMinutesScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedMinutesTease
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTop5ArtistsScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTop5SongsScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTopArtistScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTopSongScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch


sealed class WrappedScreenType {
    object Welcome : WrappedScreenType()
    object MinutesTease : WrappedScreenType()
    object MinutesReveal : WrappedScreenType()
    object TopSong : WrappedScreenType()
    object Top5Songs : WrappedScreenType()
    object TopArtist : WrappedScreenType()
    object Top5Artists : WrappedScreenType()
    object End : WrappedScreenType()
}

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
    val manager = remember { WrappedManager(getDatabaseDao(context), scope) }
    val connectivityManager = getSystemService(context, ConnectivityManager::class.java)
    val audioService = remember { WrappedAudioService(context, scope, connectivityManager!!) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        manager.loadData()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> audioService.pause()
                Lifecycle.Event.ON_RESUME -> audioService.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioService.release()
        }
    }

    val screens = remember {
        listOf(
            WrappedScreenType.Welcome,
            WrappedScreenType.MinutesTease,
            WrappedScreenType.MinutesReveal,
            WrappedScreenType.TopSong,
            WrappedScreenType.Top5Songs,
            WrappedScreenType.TopArtist,
            WrappedScreenType.Top5Artists,
            WrappedScreenType.End
        )
    }
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val totalMinutes by manager.totalMinutes.collectAsState(initial = 0L)
    val isLoading by manager.isLoading.collectAsState()
    val topSongs by manager.topSongs.collectAsState()
    val topArtists by manager.topArtists.collectAsState()
    val isMuted by audioService.isMuted.collectAsState()
    val trackMap by manager.trackMap.collectAsState()
    val messagePair = rememberSaveable(totalMinutes, saver = messagePairSaver) {
        WrappedRepository.getMessage(totalMinutes)
    }

    LaunchedEffect(pagerState, trackMap) {
        if (trackMap.isEmpty()) return@LaunchedEffect

        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
            val screen = screens.getOrNull(page)
            audioService.onPageChanged(trackMap[screen])
        }
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
                    IconButton(onClick = { audioService.toggleMute() }) {
                        val icon = if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        Icon(painterResource(icon), "Mute", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        VerticalPager(state = pagerState, modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) { page ->
            when (screens[page]) {
                is WrappedScreenType.Welcome -> WrappedIntro { scope.launch { pagerState.animateScrollToPage(page = 1) } }
                is WrappedScreenType.MinutesTease -> WrappedMinutesTease(
                    messagePair = messagePair,
                    onNavigateForward = { scope.launch { pagerState.animateScrollToPage(page = 2) } },
                    manager = manager,
                    isLoading = isLoading
                )
                is WrappedScreenType.MinutesReveal -> WrappedMinutesScreen(
                    messagePair = messagePair, totalMinutes = totalMinutes,
                    isVisible = pagerState.currentPage == 2
                )
                is WrappedScreenType.TopSong -> WrappedTopSongScreen(
                    topSong = topSongs.firstOrNull(),
                    isVisible = pagerState.currentPage == 3
                )
                is WrappedScreenType.Top5Songs -> WrappedTop5SongsScreen(
                    topSongs = topSongs.take(5),
                    isVisible = pagerState.currentPage == 4
                )
                is WrappedScreenType.TopArtist -> WrappedTopArtistScreen(
                    topArtist = topArtists.firstOrNull(),
                    isVisible = pagerState.currentPage == 5
                )
                is WrappedScreenType.Top5Artists -> WrappedTop5ArtistsScreen(
                    topArtists = topArtists,
                    isVisible = pagerState.currentPage == 6
                )
                is WrappedScreenType.End -> WrappedEndScreen(manager = manager)
            }
        }
    }
}
