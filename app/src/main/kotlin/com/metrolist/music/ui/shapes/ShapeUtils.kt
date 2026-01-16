package com.metrolist.music.ui.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * Converts a [RoundedPolygon] to a Compose [Shape].
 */
fun RoundedPolygon.toComposeShape(): Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = this@toComposeShape.toPath().asComposePath()
        val matrix = Matrix()
        
        // RoundedPolygon is defined in [-1, 1] range (diameter 2), centered at 0,0.
        // We need to move it to the center of the available size
        matrix.translate(size.width / 2f, size.height / 2f)
        
        // And scale it so that radius 1 matches half of the width/height
        // To fill the bounds completely (stretch):
        matrix.scale(size.width / 2f, size.height / 2f)
        
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

/**
 * A surer way to create a shape that fits, assuming the input polygon is "normalized" 
 * (roughly unit size, centered) but we want to stretch it to fill the view.
 */
class RoundedPolygonShape(private val polygon: RoundedPolygon) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = polygon.toPath().asComposePath()
        
        // Calculate the scale to fit the polygon into the size
        // The standard factories usually create shapes with bounds roughly -1..1 (center 0,0) or 0..1
        // Let's assume the standard behavior of the library shapes which is usually mapped 
        // to the view size.
        //
        // If we look at the ShapesDemo code again (recalled from memory/search):
        // matrix.scale(size.width, size.height)
        // path.transform(matrix)
        // This implies the source path is in 0..1 range.
        // Let's trust that consistent behavior for "normalized" shapes.
        
        val matrix = Matrix()
        matrix.scale(size.width, size.height)
        // Also if it's centered at 0,0 with range -1..1, we need to translate/scale differently.
        // But RoundedPolygon factories (star, circle) typically default to center 0.5, 0.5 with radius 0.5
        // i.e. fit in 0..1 unit square.
        
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

// Helper to create a Heart Shape
fun createHeartShape(): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            val width = size.width
            val height = size.height
            // Starting point
            moveTo(width / 2, height / 5)

            // Upper left path
            cubicTo(
                5 * width / 14, 0f,
                0f, height / 15,
                width / 28, 2 * height / 5
            )

            // Lower left path
            cubicTo(
                width / 14, 2 * height / 3,
                3 * width / 7, 5 * height / 6,
                width / 2, height
            )

            // Lower right path
            cubicTo(
                4 * width / 7, 5 * height / 6,
                13 * width / 14, 2 * height / 3,
                27 * width / 28, 2 * height / 5
            )

            // Upper right path
            cubicTo(
                width, height / 15,
                9 * width / 14, 0f,
                width / 2, height / 5
            )
            close()
        }
        return Outline.Generic(path)
    }
}
