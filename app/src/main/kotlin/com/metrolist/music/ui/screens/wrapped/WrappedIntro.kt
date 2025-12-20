package com.metrolist.music.ui.screens.wrapped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.bbhBartleFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val shadowColor1 = Color.DarkGray
private val shadowColor2 = Color.Gray

private val baseTitleTextStyle = TextStyle(
    fontFamily = bbhBartleFamily,
    fontSize = 60.sp, // Start with a larger font size
)

@Composable
fun WrappedIntro(onNext: () -> Unit) {
    val yearAlpha = remember { Animatable(0f) }
    val yearRotation = remember { Animatable(-120f) }

    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.5f) }

    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(50f) }

    val subtitleAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }
    val buttonScale = remember { Animatable(0.5f) }

    LaunchedEffect(Unit) {
        launch {
            delay(200)
            yearAlpha.animateTo(0.1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f))
            yearRotation.animateTo(-90f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f))
        }
        launch {
            delay(400)
            logoAlpha.animateTo(1f, animationSpec = spring(stiffness = 150f))
            logoScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f))
        }
        launch {
            delay(600)
            titleAlpha.animateTo(1f, animationSpec = spring(stiffness = 150f))
            titleOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f))
        }
        launch {
            delay(900)
            subtitleAlpha.animateTo(1f, animationSpec = spring(stiffness = 100f))
        }
        launch {
            delay(1100)
            buttonAlpha.animateTo(1f, animationSpec = spring(stiffness = 100f))
            buttonScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "2025",
            style = TextStyle(
                fontFamily = bbhBartleFamily,
                fontSize = 250.sp,
                fontWeight = FontWeight.Bold,
                drawStyle = Stroke(width = 4f),
                color = Color.White
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .alpha(yearAlpha.value)
                .graphicsLayer {
                    rotationZ = yearRotation.value
                }
                .offset(x = (150).dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.logo_wrapped),
                contentDescription = "Metrolist Logo",
                modifier = Modifier
                    .height(64.dp)
                    .alpha(logoAlpha.value)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    },
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .offset(y = titleOffsetY.value.dp)
            ) {
                AutoResizeText(
                    text = "METROLIST",
                    baseStyle = baseTitleTextStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "it's time to see what you've been listening to",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(bottom = 64.dp)
                    .alpha(buttonAlpha.value)
                    .graphicsLayer {
                        scaleX = buttonScale.value
                        scaleY = buttonScale.value
                    }
            ) {
                Text(
                    text = "let's go!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AutoResizeText(
    text: String,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    var scaledTextStyle by remember { mutableStateOf(baseStyle) }
    var readyToDraw by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }

        Text(
            text = text,
            style = scaledTextStyle,
            color = Color.White.copy(alpha = 0f), // Render invisibly to measure
            maxLines = 1,
            onTextLayout = { result ->
                if (result.didOverflowWidth) {
                    scaledTextStyle = scaledTextStyle.copy(
                        fontSize = scaledTextStyle.fontSize * 0.95f
                    )
                } else {
                    readyToDraw = true
                }
            }
        )

        if (readyToDraw) {
            Box {
                Text(
                    text = text,
                    color = shadowColor1,
                    style = scaledTextStyle.copy(drawStyle = Stroke(width = 3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 8.dp, x = 4.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = text,
                    color = shadowColor2,
                    style = scaledTextStyle.copy(drawStyle = Stroke(width = 3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 4.dp, x = 2.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = text,
                    color = Color.White,
                    style = scaledTextStyle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
