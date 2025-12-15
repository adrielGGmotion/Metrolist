package com.metrolist.music.wrapped.slides

import android.graphics.PointF
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private fun Float.toRadians() = this * PI.toFloat() / 180f
private val PointZero = PointF(0f, 0f)

private fun radialToCartesian(
    radius: Float,
    angleRadians: Float,
    center: PointF = PointZero
) = PointF(cos(angleRadians), sin(angleRadians)) * radius + center

private operator fun PointF.times(fl: Float): PointF {
    return PointF(x * fl, y * fl)
}

class CookieShape(
    private val matrix: Matrix = Matrix()
) : Shape {
    private var path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val numVertices = 9
        val innerRadiusRatio = 0.8f
        val radius = 1f
        val innerRadius = radius * innerRadiusRatio

        val vertices = FloatArray(numVertices * 2)
        for (i in 0 until numVertices) {
            val currentRadius = if (i % 2 == 0) radius else innerRadius
            val angle = (i * 360f / numVertices).toRadians()
            val point = radialToCartesian(currentRadius, angle)
            vertices[i * 2] = point.x
            vertices[i * 2 + 1] = point.y
        }

        val polygon = RoundedPolygon(vertices = vertices)
        path.rewind()
        path = polygon.toPath().asComposePath()

        val bounds = polygon.calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }
        val maxDimension = max(bounds.width, bounds.height)
        matrix.reset()
        matrix.scale(size.width / maxDimension, size.height / maxDimension)
        matrix.translate(-bounds.left, -bounds.top)
        path.transform(matrix)

        return Outline.Generic(path)
    }
}
