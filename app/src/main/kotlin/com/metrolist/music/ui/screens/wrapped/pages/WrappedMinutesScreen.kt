package com.metrolist.music.ui.screens.wrapped.pages

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.BuildConfig
import com.metrolist.music.ui.screens.wrapped.MessagePair
import com.metrolist.music.ui.screens.wrapped.components.WrappedShareCard
import com.metrolist.music.ui.theme.bbh_bartle
import com.metrolist.music.ui.utils.captureComposableAsBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

@Composable
fun WrappedMinutesScreen(
    messagePair: MessagePair?, totalMinutes: Long,
    accountInfo: AccountInfo?, isVisible: Boolean
) {
    var loading by remember { mutableStateOf(false) }
    val animatedMinutes = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun share() {
        scope.launch {
            loading = true
            val bitmap = withContext(Dispatchers.Default) {
                captureComposableAsBitmap(context) {
                    WrappedShareCard(accountInfo, totalMinutes)
                }
            }
            val file = File(context.cacheDir, "wrapped.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share your Wrapped"))
            loading = false
        }
    }

    LaunchedEffect(isVisible, totalMinutes) {
        if (isVisible && totalMinutes > 0) {
            animatedMinutes.animateTo(targetValue = totalMinutes.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            repeat(3) {
                AnimatedDecorativeElement(
                    Modifier.padding(start = (16 + it * 30).dp, top = (16 + it * 40).dp).size((50 + it * 15).dp),
                    isVisible
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            repeat(3) {
                AnimatedDecorativeElement(
                    Modifier.padding(end = (16 + it * 30).dp, bottom = (16 + it * 40).dp).size((60 + it * 10).dp),
                    isVisible
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FormattedText(
                text = messagePair?.tease ?: "", modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, textAlign = TextAlign.Center)
            )
            Spacer(Modifier.height(32.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val density = LocalDensity.current
                val baseStyle = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() })
                )
                val textStyle = remember(totalMinutes, maxWidth) {
                    val finalText = totalMinutes.toString()
                    var style = baseStyle.copy(fontSize = 96.sp)
                    var textWidth = textMeasurer.measure(finalText, style).size.width
                    while (textWidth > constraints.maxWidth) {
                        style = style.copy(fontSize = style.fontSize * 0.95f)
                        textWidth = textMeasurer.measure(finalText, style).size.width
                    }
                    style.copy(lineHeight = style.fontSize * 1.08f)
                }
                Text(animatedMinutes.value.toInt().toString(), style = textStyle, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(16.dp))
            FormattedText(
                text = messagePair?.reveal ?: "", modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { share() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AnimatedDecorativeElement(modifier: Modifier = Modifier, isVisible: Boolean) {
    val rotation = remember { Animatable(0f) }
    val shapeType = remember { Random.nextInt(3) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(Random.nextLong(500))
            rotation.animateTo(targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(Random.nextInt(1000, 3000)), repeatMode = RepeatMode.Restart))
        }
    }
    Canvas(modifier.graphicsLayer { rotationZ = rotation.value }) {
        val strokeWidth = 2.dp.toPx()
        when (shapeType) {
            0 -> drawArc(Color.White.copy(0.2f), 0f, 90f, false, style = Stroke(strokeWidth))
            1 -> drawCircle(Color.White.copy(0.2f), style = Stroke(strokeWidth))
            2 -> drawRect(Color.White.copy(0.2f), style = Stroke(strokeWidth))
        }
    }
}

@Composable
fun FormattedText(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle) {
    val annotatedString = buildAnnotatedString {
        val parts = text.split("(?=\\*\\*)|(?<=\\*\\*)".toRegex())
        var isBold = false
        for (part in parts) {
            if (part == "**") isBold = !isBold
            else withStyle(SpanStyle(fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)) { append(part) }
        }
    }
    Text(annotatedString, modifier, style = style)
}
