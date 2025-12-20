package com.metrolist.music.ui.screens.wrapped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BBHBBartle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

private val DarkGray = Color(0xFF1A1A1A)
private val Gray = Color(0xFF595959)
private val White = Color.White

private val MetrolistTextStyle = TextStyle(
    fontFamily = BBHBBartle,
    fontWeight = FontWeight.W900,
    lineHeight = 1.sp,
    textAlign = TextAlign.Center
)

private const val METROLIST_TEXT = "METROLIST"
private const val YEAR_TEXT = "2025"

@Composable
fun WrappedIntro(onStart: () -> Unit) {
    val animatables = List(5) { remember { Animatable(0f) } }
    var animationsFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        val animations = animatables.mapIndexed { index, animatable ->
            async {
                delay(index * 150L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.5f,
                        stiffness = 100f
                    )
                )
            }
        }
        animations.awaitAll()
        animationsFinished = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        YearText(
            alpha = animatables[0].value,
            scale = 1f + (1 - animatables[0].value) * 0.2f
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Logo(
                alpha = animatables[1].value,
                scale = 1f + (1 - animatables[1].value) * 0.2f
            )
            Spacer(modifier = Modifier.height(16.dp))
            MetrolistTitle(
                alpha = animatables[2].value,
                scale = 1f + (1 - animatables[2].value) * 0.2f
            )
            Spacer(modifier = Modifier.height(16.dp))
            Subtitle(
                alpha = animatables[3].value,
                scale = 1f + (1 - animatables[2].value) * 0.2f
            )
        }
        StartButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .alpha(if (animationsFinished) 1f else 0f),
            onClick = onStart
        )
    }
}

@Composable
private fun Logo(alpha: Float, scale: Float) {
    Image(
        painter = painterResource(id = R.drawable.metrolist_logo),
        contentDescription = "Metrolist Logo",
        modifier = Modifier
            .alpha(alpha)
            .offset(y = (24 * (1 - scale)).dp)
    )
}

@Composable
private fun YearText(alpha: Float, scale: Float) {
    ProvideTextStyle(
        value = TextStyle(
            fontFamily = BBHBBartle,
            fontWeight = FontWeight.W900,
            fontSize = 300.sp,
            color = DarkGray.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha * 0.3f)
        ) {
            Text(
                text = YEAR_TEXT,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .drawWithContent {
                        rotate(-90f) {
                            this@drawWithContent.drawContent()
                        }
                    }
            )
        }
    }
}

@Composable
private fun MetrolistTitle(alpha: Float, scale: Float) {
    Box(
        modifier = Modifier.fillMaxWidth(0.8f),
        contentAlignment = Alignment.Center
    ) {
        AutoResizeText(
            text = METROLIST_TEXT,
            style = MetrolistTextStyle,
            modifier = Modifier
                .alpha(alpha)
                .offset(y = (24 * (1 - scale)).dp)
        )
    }
}


@Composable
fun AutoResizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var aStyle by remember { mutableStateOf(style) }
        var ready by remember { mutableStateOf(false) }

        val density = LocalDensity.current
        val fs = with(density) {
            aStyle.fontSize.toPx()
        }

        val animatable = remember { Animatable(0.8f) }
        LaunchedEffect(ready) {
            if (ready) {
                animatable.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 100f))
            }
        }
        val scale = animatable.value

        EchoEffectText(
            text = text,
            style = aStyle,
            scale = scale,
            onTextLayout = { result ->
                if (!ready) {
                    val newFs = fs / result.size.width * constraints.maxWidth
                    if (newFs < fs) {
                        aStyle = aStyle.copy(fontSize = (newFs * 0.95).sp)
                    } else {
                        ready = true
                    }
                }
            }
        )
    }
}

@Composable
private fun EchoEffectText(
    text: String,
    style: TextStyle,
    scale: Float,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = style.copy(color = DarkGray),
            modifier = Modifier.offset(x = (4 * scale).dp, y = (4 * scale).dp),
            onTextLayout = onTextLayout,
        )
        Text(
            text = text,
            style = style.copy(color = Gray),
            modifier = Modifier.offset(x = (2 * scale).dp, y = (2 * scale).dp),
        )
        Text(
            text = text,
            style = style.copy(color = White),
        )
    }
}


@Composable
private fun Subtitle(alpha: Float, scale: Float) {
    Text(
        text = "it's time to see what you've been listening to",
        color = Color.White.copy(alpha = 0.8f),
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .alpha(alpha)
            .offset(y = (16 * (1 - scale)).dp)
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    )
}

@Composable
private fun StartButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Text(text = "let's go!")
    }
}
