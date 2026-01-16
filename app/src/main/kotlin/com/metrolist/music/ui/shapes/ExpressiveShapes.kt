package com.metrolist.music.ui.shapes

import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

object ExpressiveShapes {
    // Clovers
    val Clover4Leaf: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 4,
        innerRadius = 0.2f, // Deeper indentation
        rounding = CornerRounding(radius = 0.4f)
    ).toComposeShape()

    val Clover8Leaf: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        innerRadius = 0.6f,
        rounding = CornerRounding(radius = 0.3f)
    ).toComposeShape()

    // Flower
    val Flower: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        innerRadius = 0.7f,
        rounding = CornerRounding(radius = 0.5f, smoothing = 0.5f)
    ).toComposeShape()

    // Bursts
    val Burst: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 12,
        innerRadius = 0.8f
    ).toComposeShape()
    
    val SoftBurst: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 12,
        innerRadius = 0.8f,
        rounding = CornerRounding(0.1f)
    ).toComposeShape()
    
    val Boom: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 15,
        innerRadius = 0.6f
    ).toComposeShape()
    
    val SoftBoom: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 15,
        innerRadius = 0.6f,
        rounding = CornerRounding(0.2f)
    ).toComposeShape()

    // Sunny
    val Sunny: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        innerRadius = 0.8f
    ).toComposeShape()

    // Note: Some complex shapes like Heart, Ghostish, etc. require custom path data or complex polygon construction
    // which are not standard factory methods. For now, we implement the ones easily reproducible with star/polygon parameters.
    // If exact Material 3 implementation is needed, we would need to replicate the control points from the source.
    // For this task, we will stick to the reusable algorithmic shapes.
}
