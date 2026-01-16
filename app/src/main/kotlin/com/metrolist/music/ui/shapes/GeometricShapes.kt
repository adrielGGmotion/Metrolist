package com.metrolist.music.ui.shapes

import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star

object GeometricShapes {
    val Circle: Shape = RoundedPolygon.circle().toComposeShape()
    
    val Square: Shape = RoundedPolygon.rectangle(width = 1f, height = 1f).toComposeShape()
    
    val Pill: Shape = RoundedPolygon.pill().toComposeShape()
    
    val PillStar: Shape = RoundedPolygon.pillStar().toComposeShape()
    
    val Rectangle: Shape = RoundedPolygon.rectangle().toComposeShape() // Standard rectangle
    
    val Triangle: Shape = RoundedPolygon(3).toComposeShape()
    
    val Pentagon: Shape = RoundedPolygon(5).toComposeShape()
    
    val Hexagon: Shape = RoundedPolygon(6).toComposeShape()
    
    val Octagon: Shape = RoundedPolygon(8).toComposeShape()
    
    // Configurable star example
    fun star(
        numVertices: Int = 5,
        innerRadius: Float = 0.5f,
        rounding: Float = 0f
    ): Shape {
        return RoundedPolygon.star(
            numVerticesPerRadius = numVertices,
            innerRadius = innerRadius,
            rounding = CornerRounding(rounding)
        ).toComposeShape()
    }
}
