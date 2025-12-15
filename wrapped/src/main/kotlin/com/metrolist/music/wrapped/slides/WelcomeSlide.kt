package com.metrolist.music.wrapped.slides

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import kotlinx.coroutines.delay

val BartleFontFamily = FontFamily(
    Font(R.font.bbh_bartle, FontWeight.Normal)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal)
)

@Composable
fun WelcomeSlide(onNext: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val animations = List(5) { i ->
        animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 500, delayMillis = i * 100)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "2025",
            fontFamily = BartleFontFamily,
            fontSize = 200.sp,
            color = Color.White.copy(alpha = 0.1f),
            style = TextStyle.Default.copy(
                drawStyle = Stroke(width = 2f)
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(animations[0].value)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
                .alpha(animations[1].value)
                .drawBehind {
                    drawPath(
                        path = CookieShape().createOutline(
                            size,
                            layoutDirection,
                            this
                        ).path,
                        color = Color.DarkGray
                    )
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
                .alpha(animations[2].value)
                .graphicsLayer { rotationZ = rotation }
                .drawBehind {
                    drawPath(
                        path = CookieShape().createOutline(
                            size,
                            layoutDirection,
                            this
                        ).path,
                        color = Color.Red,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_metrolist_logo),
                contentDescription = "Metrolist Logo",
                modifier = Modifier.alpha(animations[3].value)
            )
            Box {
                Text(
                    text = "METROLIST",
                    fontFamily = BartleFontFamily,
                    fontSize = 50.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    style = TextStyle.Default.copy(
                        drawStyle = Stroke(width = 2f)
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
                Text(
                    text = "METROLIST",
                    fontFamily = BartleFontFamily,
                    fontSize = 50.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    style = TextStyle.Default.copy(
                        drawStyle = Stroke(width = 2f)
                    ),
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                )
                Text(
                    text = "METROLIST",
                    fontFamily = BartleFontFamily,
                    fontSize = 50.sp,
                    color = Color.White,
                )
            }
            Text(
                text = "it's time to see what you've been listening to",
                fontFamily = InterFontFamily,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.alpha(animations[4].value)
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .alpha(animations[4].value),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text(
                text = "let's go!",
                fontFamily = InterFontFamily,
                color = Color.Black
            )
        }
    }
}

@Preview
@Composable
fun WelcomeSlidePreview() {
    WelcomeSlide {}
}
