package com.metrolist.music.ui.screens.wrapped.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.R
import com.metrolist.music.ui.theme.bbh_bartle
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

@Composable
fun WrappedShareCard(accountInfo: AccountInfo?, totalMinutes: Long) {
    val formattedMinutes = remember(totalMinutes) {
        NumberFormat.getNumberInstance(Locale.GERMAN).format(totalMinutes)
    }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        repeat(5) { RandomBackgroundShape() }
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "App Icon",
            tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(32.dp).size(48.dp)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("I've listened to", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text(
                text = formattedMinutes, color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold,
                fontFamily = bbh_bartle, textAlign = TextAlign.Center, lineHeight = 80.sp,
                style = MaterialTheme.typography.displayLarge.copy(drawStyle = Stroke(with(density) { 2.dp.toPx() }))
            )
            Text(
                text = "MINUTES\nON METROLIST!", color = Color.White, fontSize = 40.sp,
                fontWeight = FontWeight.Bold, fontFamily = bbh_bartle, textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )
            Spacer(Modifier.height(32.dp))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(accountInfo?.thumbnailUrl).build(),
                contentDescription = "User Profile Picture",
                modifier = Modifier.size(120.dp).clip(CircleShape), contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun RandomBackgroundShape() {
    val size = remember { Random.nextInt(100, 300).dp }
    val xOffset = remember { Random.nextInt(-100, 100).dp }
    val yOffset = remember { Random.nextInt(-100, 100).dp }
    val rotation = remember { Random.nextFloat() * 360f }
    val cornerRadius = remember { Random.nextInt(20, 100).dp }
    Canvas(Modifier.fillMaxSize().offset(x = xOffset, y = yOffset).graphicsLayer { rotationZ = rotation }) {
        drawRoundRect(
            color = Color.DarkGray.copy(alpha = 0.2f),
            size = androidx.compose.ui.geometry.Size(size.toPx(), size.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
        )
    }
}
