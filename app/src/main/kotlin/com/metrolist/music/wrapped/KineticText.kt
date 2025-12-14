package com.metrolist.music.wrapped

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch

@Composable
fun KineticText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    isVisible: Boolean,
) {
    var scale by remember { mutableStateOf(0.8f) }
    var alpha by remember { mutableStateOf(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            launch {
                animate(initialValue = 0.8f, targetValue = 1f) { value, _ ->
                    scale = value
                }
            }
            launch {
                animate(initialValue = 0f, targetValue = 1f) { value, _ ->
                    alpha = value
                }
            }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .alpha(alpha)
    ) {
        Text(
            text = text,
            style = style,
            textAlign = textAlign
        )
    }
}
