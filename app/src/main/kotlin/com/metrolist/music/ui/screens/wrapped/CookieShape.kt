package com.metrolist.music.ui.screens.wrapped

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

private val Cookie9Sided = RoundedPolygon(
    numVertices = 9,
    rounding = CornerRounding(radius = 0.5f, smoothing = 1.0f)
)

class CookieShape : Shape {
    private val matrix = Matrix()
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        // The polygon is normalized to a [-1, 1] space.
        // We need to scale it to the component's size and translate it to the center.
        matrix.reset()
        matrix.setScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)

        Cookie9Sided.toPath(path.asAndroidPath())
        path.asAndroidPath().transform(matrix)

        return Outline.Generic(path)
    }
}
