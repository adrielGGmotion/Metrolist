package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metrolist.music.lyrics.AppleMusicLyricsLine

@Composable
fun AppleMusicLyricLine(
    line: AppleMusicLyricsLine,
    currentPosition: Long,
    isActive: Boolean,
    style: TextStyle,
    inactiveColor: Color,
    activeColor: Color
) {
    val progress = remember { Animatable(0f) }
    val fullText = remember(line) {
        line.words.joinToString(" ") { it.text }
    }

    LaunchedEffect(isActive, currentPosition) {
        if (isActive) {
            val lineDuration = (line.endTime - line.startTime).toFloat()
            val elapsedTime = (currentPosition - line.startTime).toFloat()
            val currentProgress = (elapsedTime / lineDuration).coerceIn(0f, 1f)
            progress.snapTo(currentProgress)

            val remainingDuration = line.endTime - currentPosition
            if (remainingDuration > 0) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = remainingDuration.toInt(),
                        easing = LinearEasing
                    )
                )
            }
        } else {
            val targetProgress = if (currentPosition > line.endTime) 1f else 0f
            progress.snapTo(targetProgress)
        }
    }

    val arrangement = when (line.speaker) {
        "v2" -> Arrangement.Start
        "v1" -> Arrangement.End
        else -> Arrangement.Center
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement
    ) {
        Text(
            text = fullText,
            style = style.copy(color = inactiveColor),
            modifier = Modifier
                .graphicsLayer(alpha = 0.99f) // Required for blend mode to work
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(activeColor, activeColor),
                            endX = size.width * progress.value
                        ),
                        blendMode = BlendMode.SrcIn
                    )
                }
        )
    }
}
