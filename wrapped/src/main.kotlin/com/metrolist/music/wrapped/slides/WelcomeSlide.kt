package com.metrolist.music.wrapped.slides

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R

val BartleFontFamily = FontFamily(
    Font(R.font.bbh_bartle, FontWeight.Normal)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal)
)

@Composable
fun WelcomeSlide(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "2025",
            fontFamily = BartleFontFamily,
            fontSize = 200.sp,
            color = Color.White.copy(alpha = 0.1f),
            style = TextStyle.Default.copy(
                drawStyle = Stroke(width = 2f)
            ),
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
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
                contentDescription = "Metrolist Logo"
            )
            Text(
                text = "METROLIST",
                fontFamily = BartleFontFamily,
                fontSize = 50.sp,
                color = Color.White,
                style = TextStyle.Default.copy(
                    drawStyle = Stroke(width = 1f)
                )
            )
            Text(
                text = "it's time to see what you've been listening to",
                fontFamily = InterFontFamily,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
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
