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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.metrolist.music.ui.screens.wrapped.pages.WrappedIntro
import com.metrolist.music.ui.screens.wrapped.pages.WrappedMinutesScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedMinutesTease
import com.metrolist.music.ui.screens.wrapped.pages.WrappedEndScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTop5ArtistsScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTop5SongsScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTopArtistScreen
import com.metrolist.music.ui.screens.wrapped.pages.WrappedTopSongScreen
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val isMuted by audioService.isMuted.collectAsState()
    val messagePair = rememberSaveable(totalMinutes, saver = messagePairSaver) {
        WrappedRepository.getMessage(totalMinutes)
    }
    var lastPlayedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState, topSongs) {
        if (topSongs.isEmpty()) return@LaunchedEffect

        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
            val topSongId = topSongs.first().id
            val songToPlay = when (page) {
                3 -> topSongId // Top Song Screen
                4 -> topSongs.getOrNull(1)?.id ?: topSongId // Top 5 Songs screen (e.g., show #2)
                5 -> topArtists.firstOrNull()?.let { topSongs.find { song -> song.id == it.id }?.id } ?: topSongId // Top Artist Screen
                6 -> topArtists.getOrNull(1)?.let { topSongs.find { song -> song.id == it.id }?.id } ?: topSongId // Top 5 Artists
                0, 1, 2, 7 -> { // Intro, Tease, Reveal, End screens
                    val eligibleSongs = topSongs.take(50).filter { it.id != lastPlayedId }
                    if (eligibleSongs.isNotEmpty()) {
                        eligibleSongs.random().id
                    } else {
                        topSongs.take(50).randomOrNull()?.id // Fallback if all top songs were last played
                    }
                }
                else -> null // No music for other screens
            }
            lastPlayedId = songToPlay
            audioService.onPageChanged(songToPlay)
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
