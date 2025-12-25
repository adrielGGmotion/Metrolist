package com.metrolist.music.ui.screens.wrapped.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.db.entities.SongWithStats
import com.metrolist.music.ui.screens.wrapped.components.AnimatedDecorativeElement
import com.metrolist.music.ui.theme.bbh_bartle
import kotlin.random.Random

@Composable
fun WrappedTotalSongsScreen(
    topSongs: List<SongWithStats>,
    isVisible: Boolean
) {
    val animatedSongs = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val totalSongs = topSongs.size

    LaunchedEffect(isVisible, totalSongs) {
        if (isVisible && totalSongs > 0) {
            animatedSongs.animateTo(targetValue = totalSongs.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            repeat(5) {
                AnimatedDecorativeElement(
                    Modifier.padding(start = (Random.nextInt(0, 150)).dp, top = (Random.nextInt(0, 150)).dp).size((Random.nextInt(20, 100)).dp),
                    isVisible
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            repeat(5) {
                AnimatedDecorativeElement(
                    Modifier.padding(end = (Random.nextInt(0, 150)).dp, bottom = (Random.nextInt(0, 150)).dp).size((Random.nextInt(20, 100)).dp),
                    isVisible
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "You listened to",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, textAlign = TextAlign.Center)
            )
            Spacer(Modifier.height(32.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val density = LocalDensity.current
                val baseStyle = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() })
                )
                val textStyle = remember(totalSongs, maxWidth) {
                    val finalText = totalSongs.toString()
                    var style = baseStyle.copy(fontSize = 96.sp)
                    var textWidth = textMeasurer.measure(finalText, style).size.width
                    while (textWidth > constraints.maxWidth) {
                        style = style.copy(fontSize = style.fontSize * 0.95f)
                        textWidth = textMeasurer.measure(finalText, style).size.width
                    }
                    style.copy(lineHeight = style.fontSize * 1.08f)
                }
                Text(animatedSongs.value.toInt().toString(), style = textStyle, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "unique songs",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            )
        }
    }
}
