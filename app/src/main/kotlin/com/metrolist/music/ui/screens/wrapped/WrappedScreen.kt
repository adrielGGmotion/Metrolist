package com.metrolist.music.ui.screens.wrapped

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(navController: NavController) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val manager = remember {
        WrappedPlaybackManager(playerConnection, scope)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(manager, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> manager.pausePlayback()
                Lifecycle.Event.ON_RESUME -> manager.resumePlayback()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        manager.initialize()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.release()
        }
    }

    val screens = listOf(
        "Welcome screen",
        "Minutes intro screen",
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

    val isManagerReady by manager.isReady.collectAsState()

    LaunchedEffect(pagerState, isManagerReady) {
        if (isManagerReady) {
            snapshotFlow { pagerState.currentPage }.collectLatest { page ->
                manager.playSongForPage(page)
            }
        }
    }

    CompositionLocalProvider(LocalWrappedManager provides manager) {
        var isMuted by remember { mutableStateOf(false) }

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
                        IconButton(onClick = {
                            manager.toggleMute()
                            isMuted = !isMuted
                        }) {
                            Icon(
                                painter = painterResource(id = if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
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
            WrappedPage(
                name = screens[page],
                number = page + 1,
                offset = pagerState.currentPageOffsetFraction
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
