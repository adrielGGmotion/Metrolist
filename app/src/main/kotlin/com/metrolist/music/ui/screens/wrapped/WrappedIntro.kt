package com.metrolist.music.ui.screens.wrapped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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

private val titleTextStyle = TextStyle(
    fontFamily = bbhBartleFamily,
    fontSize = 48.sp,
    drawStyle = Stroke(width = 3f)
)

private val yearTextStyle = TextStyle(
    fontFamily = bbhBartleFamily,
    fontSize = 250.sp,
    fontWeight = FontWeight.Bold,
    drawStyle = Stroke(width = 4f),
    color = Color.White
)

@Composable
fun WrappedIntro(onNext: () -> Unit) {
    val yearAlpha = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoOffsetY = remember { Animatable(-50f) }
    val title1Alpha = remember { Animatable(0f) }
    val title2Alpha = remember { Animatable(0f) }
    val title3Alpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(50f) }
    val buttonAlpha = remember { Animatable(0f) }
    val buttonScale = remember { Animatable(0.5f) }


    LaunchedEffect(Unit) {
        launch {
            delay(200)
            yearAlpha.animateTo(0.1f, animationSpec = tween(800))
        }
        launch {
            delay(400)
            logoAlpha.animateTo(1f, animationSpec = tween(600))
            logoOffsetY.animateTo(0f, animationSpec = tween(600))
        }
        launch {
            delay(600)
            title3Alpha.animateTo(1f, animationSpec = tween(500))
        }
        launch {
            delay(700)
            title2Alpha.animateTo(1f, animationSpec = tween(500))
        }
        launch {
            delay(800)
            title1Alpha.animateTo(1f, animationSpec = tween(500))
        }
        launch {
            delay(1000)
            subtitleAlpha.animateTo(1f, animationSpec = tween(600))
            subtitleOffsetY.animateTo(0f, animationSpec = tween(600))
        }
        launch {
            delay(1200)
            buttonAlpha.animateTo(1f, animationSpec = tween(500))
            buttonScale.animateTo(1f, animationSpec = tween(500))
        }
    }


    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "2025",
            style = yearTextStyle,
            modifier = Modifier
                .alpha(yearAlpha.value)
                .graphicsLayer {
                    rotationZ = -90f
                }
                .offset(x = -maxWidth / 3, y = 0.dp)
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
                    .offset(y = logoOffsetY.value.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box {
                Text(
                    text = "METROLIST",
                    color = shadowColor1,
                    style = titleTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 8.dp, x = 4.dp)
                        .alpha(title1Alpha.value),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "METROLIST",
                    color = shadowColor2,
                    style = titleTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 4.dp, x = 2.dp)
                        .alpha(title2Alpha.value),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "METROLIST",
                    color = Color.White,
                    style = titleTextStyle.copy(drawStyle = Stroke(width = 0f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(title3Alpha.value),
                    textAlign = TextAlign.Center
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "it's time to see what you've been listening to",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .offset(y = subtitleOffsetY.value.dp)
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
