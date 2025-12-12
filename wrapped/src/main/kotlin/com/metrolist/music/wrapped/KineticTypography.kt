package com.metrolist.music.wrapped

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset

@Composable
fun KineticTypography(
    text: String,
    animationStyle: AnimationStyle
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = when (animationStyle) {
            AnimationStyle.SLIDE_IN_FROM_LEFT -> slideIn(tween(500)) { IntOffset(-it.width, 0) } + fadeIn(tween(500))
            AnimationStyle.SLIDE_IN_FROM_RIGHT -> slideIn(tween(500)) { IntOffset(it.width, 0) } + fadeIn(tween(500))
            AnimationStyle.SCALE_IN -> scaleIn(tween(500)) + fadeIn(tween(500))
        }
    ) {
        Text(text)
    }
}

enum class AnimationStyle {
    SLIDE_IN_FROM_LEFT,
    SLIDE_IN_FROM_RIGHT,
    SCALE_IN
}
