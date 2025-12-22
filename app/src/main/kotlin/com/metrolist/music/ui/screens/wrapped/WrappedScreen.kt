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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.random.Random
import com.metrolist.music.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.metrolist.music.ui.theme.bbh_bartle
import com.metrolist.music.ui.screens.wrapped.WrappedRepository
import com.metrolist.music.ui.screens.wrapped.MessagePair
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(navController: NavController) {
    val messagePairSaver = Saver<MessagePair, List<Any>>(
        save = {
            listOf(it.range.first, it.range.last, it.tease, it.reveal)
        },
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
        "Minutes listened",
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

    val totalMinutes by manager.totalMinutes.collectAsState(initial = 0L)
    val messagePair = rememberSaveable(totalMinutes, saver = messagePairSaver) {
        WrappedRepository.getMessage(totalMinutes)
    }

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
                1 -> WrappedMinutesTease(
                    messagePair = messagePair,
                    onNavigateForward = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = 2,
                                animationSpec = tween(durationMillis = 1000)
                            )
                        }
                    },
                    manager = manager
                )
                2 -> WrappedMinutesTotal(
                    messagePair = messagePair,
                    totalMinutes = totalMinutes
                )
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
fun WrappedMinutesTotal(messagePair: MessagePair?, totalMinutes: Long) {
    val animatedMinutes = remember { Animatable(0f) }

    LaunchedEffect(totalMinutes) {
        if (totalMinutes > 0) {
            animatedMinutes.animateTo(
                targetValue = totalMinutes.toFloat(),
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedDecorativeElement(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(50.dp)
        )
        AnimatedDecorativeElement(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(80.dp)
        )
        AnimatedDecorativeElement(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(60.dp)
        )
        AnimatedDecorativeElement(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(70.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = messagePair?.tease ?: "",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            val text = animatedMinutes.value.toInt().toString()
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val baseStyle = MaterialTheme.typography.displayLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = bbh_bartle,
                drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = with(density) { 2.dp.toPx() }
                )
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val textStyle = remember(totalMinutes, maxWidth) {
                    val finalText = totalMinutes.toString()
                    var style = baseStyle.copy(fontSize = 96.sp)
                    var textWidth = textMeasurer.measure(finalText, style).size.width
                    while (textWidth > constraints.maxWidth) {
                        val newFontSize = style.fontSize * 0.95f
                        style = style.copy(fontSize = newFontSize)
                        textWidth = textMeasurer.measure(finalText, style).size.width
                    }
                    style.copy(lineHeight = style.fontSize * 1.08f)
                }

                Text(
                    text = text,
                    style = textStyle,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = messagePair?.reveal ?: "",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { /* TODO: Share action */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Share",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AnimatedDecorativeElement(modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(Random.nextLong(500)) // Random delay for staggered effect
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
    }

    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation.value }) {
        drawArc(
            color = Color.White.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun WrappedMinutesTease(
    messagePair: MessagePair?,
    onNavigateForward: () -> Unit,
    manager: WrappedManager
) {
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
                modifier = Modifier.padding(horizontal = 24.dp),
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
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
