package com.metrolist.music.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

fun captureComposableAsBitmap(
    context: Context,
    content: @Composable () -> Unit
): Bitmap {
    val composeView = ComposeView(context).apply {
        setContent(content)
    }

    val width = 1080
    val height = 1920

    composeView.layout(0, 0, width, height)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    composeView.draw(canvas)

    return bitmap
}
