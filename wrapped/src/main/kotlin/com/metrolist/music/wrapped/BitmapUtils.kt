package com.metrolist.music.wrapped

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import java.io.File
import java.io.FileOutputStream

fun createBitmapFromComposable(context: Context, composable: @Composable () -> Unit): Bitmap {
    val view = ComposeView(context).apply {
        setContent(composable)
    }
    view.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
    )
    view.layout(0, 0, 1080, 1920)
    val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}
