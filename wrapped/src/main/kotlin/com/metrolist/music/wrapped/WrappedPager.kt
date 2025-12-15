package com.metrolist.music.wrapped

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import android.content.Intent
import androidx.core.content.FileProvider
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.wrapped.slides.WelcomeSlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private suspend fun ExoPlayer.fadeVolume(from: Float, to: Float, duration: Long) {
    val steps = 10
    val stepDelay = duration / steps
    val stepSize = (to - from) / steps
    var volume = from
    withContext(Dispatchers.Default) {
        for (i in 1..steps) {
            volume += stepSize
            this@fadeVolume.volume = volume
            delay(stepDelay)
        }
    }
}

private fun playSongWithSmartSeek(exoPlayer: ExoPlayer, song: SongItem) {
    val mediaItem = MediaItem.fromUri("https://music.youtube.com/watch?v=${song.id}")
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()

    val startTime = song.duration?.let { it * 0.3 }?.toLong() ?: 0L
    exoPlayer.seekTo(startTime)
    exoPlayer.play()
}

@Composable
fun WrappedPager(
    userName: String,
    viewModel: WrappedViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val slides = listOf<@Composable () -> Unit>(
        { WelcomeSlide { coroutineScope.launch { pagerState.animateScrollToPage(1) } } },
        { MinutesSlide(viewModel.wrappedStats.collectAsState().value) },
        { TopArtistsSlide(viewModel.wrappedStats.collectAsState().value) },
        { TopAlbumsSlide(viewModel.wrappedStats.collectAsState().value) },
        { TopSongsSlide(viewModel.wrappedStats.collectAsState().value) },
        { SummarySlide(viewModel.wrappedStats.collectAsState().value) { viewModel.savePlaylist() } }
    )
    val wrappedStats by viewModel.wrappedStats.collectAsState()
    val context = LocalContext.current
    val exoPlayer1 = remember { ExoPlayer.Builder(context).build() }
    val exoPlayer2 = remember { ExoPlayer.Builder(context).build() }
    var activePlayer by remember { mutableStateOf(exoPlayer1) }

    DisposableEffect(Unit) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer1.pause()
                    exoPlayer2.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    activePlayer.play()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer1.release()
                    exoPlayer2.release()
                }
                else -> {}
            }
        }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    LaunchedEffect(pagerState, wrappedStats) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (wrappedStats != null && wrappedStats!!.topSongs.isNotEmpty()) {
                val song = when (page) {
                    0 -> wrappedStats!!.topSongs.random()
                    1 -> wrappedStats!!.topSongs.first()
                    2 -> wrappedStats!!.topSongs.getOrNull(1) ?: wrappedStats!!.topSongs.first()
                    3 -> wrappedStats!!.topSongs.getOrNull(2) ?: wrappedStats!!.topSongs.first()
                    4 -> wrappedStats!!.topSongs.getOrNull(3) ?: wrappedStats!!.topSongs.first()
                    5 -> wrappedStats!!.topSongs.getOrNull(4) ?: wrappedStats!!.topSongs.first()
                    else -> wrappedStats!!.topSongs.first()
                }
                val nextPlayer = if (activePlayer == exoPlayer1) exoPlayer2 else exoPlayer1
                launch {
                    playSongWithSmartSeek(nextPlayer, song)
                    activePlayer.fadeVolume(1f, 0f, 500)
                    nextPlayer.fadeVolume(0f, 1f, 500)
                    activePlayer.stop()
                    activePlayer = nextPlayer
                }
            }
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        slides[page]()
    }
}

@Composable
fun MinutesSlide(wrappedStats: WrappedStats?) {
    val context = LocalContext.current
    val shareImageGenerator = remember { ShareImageGenerator(context) }
    var targetMinutes by remember { mutableIntStateOf(0) }
    val animatedMinutes by animateIntAsState(
        targetValue = targetMinutes,
        animationSpec = tween(durationMillis = 2000)
    )

    LaunchedEffect(wrappedStats) {
        if (wrappedStats != null) {
            targetMinutes = wrappedStats.totalMinutes.toInt()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = animatedMinutes.toString(), fontSize = 100.sp)
            if (wrappedStats != null) {
                Text(
                    text = when {
                        wrappedStats.totalMinutes < 500 -> "You're not that music fan... Are you?"
                        wrappedStats.totalMinutes > 10000 -> "It's been a rough year, you've been listening for ${wrappedStats.totalMinutes} Minutes, that's a lot!"
                        else -> "It's been a while since you've been using Metrolist..."
                    }
                )
            }
            val coroutineScope = rememberCoroutineScope()
            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    val bitmap = shareImageGenerator.generateStatStory(wrappedStats, Stat.MINUTES)
                    val file = File(context.cacheDir, "wrapped_minutes.png")
                    FileOutputStream(file).use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.wrapped.provider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share your Wrapped"))
                }
            }) {
                Text("Share")
            }
        }
    }
}

@Composable
fun GenresSlide(wrappedStats: WrappedStats?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (wrappedStats != null) {
            Text("Defining your taste is a complex mission... But we tried.")
        }
    }
}

@Composable
fun TopArtistsSlide(wrappedStats: WrappedStats?) {
    if (wrappedStats != null) {
        TopArtistsLayout(wrappedStats.topArtists)
    }
}

@Composable
fun TopAlbumsSlide(wrappedStats: WrappedStats?) {
    if (wrappedStats != null) {
        TopAlbumsLayout(wrappedStats.topSongs.mapNotNull { it.album })
    }
}

@Composable
fun TopSongsSlide(wrappedStats: WrappedStats?) {
    if (wrappedStats != null) {
        TopSongsLayout(wrappedStats.topSongs)
    }
}

@Composable
fun SummarySlide(wrappedStats: WrappedStats?, onSavePlaylist: () -> Unit) {
    val context = LocalContext.current
    val shareImageGenerator = remember { ShareImageGenerator(context) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (wrappedStats != null) {
            Column {
                Text("One more little thing...")
                Text("Top 5 Artists:")
                wrappedStats.topArtists.forEach { Text(it.name) }
                Text("Top 5 Songs:")
                wrappedStats.topSongs.forEach { Text(it.title) }
                Text("Total Minutes: ${wrappedStats.totalMinutes}")
                val coroutineScope = rememberCoroutineScope()
                Button(onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val bitmap = shareImageGenerator.generateReceipt(wrappedStats)
                        val file = File(context.cacheDir, "wrapped_receipt.png")
                        FileOutputStream(file).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.wrapped.provider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share your Wrapped"))
                    }
                }) {
                    Text("Share")
                }
            val coroutineScope = rememberCoroutineScope()
            Button(onClick = onSavePlaylist) {
                    Text("Save Playlist")
                }
            }
        }
    }
}
