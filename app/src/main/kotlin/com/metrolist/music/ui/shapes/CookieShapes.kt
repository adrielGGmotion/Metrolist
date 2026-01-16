package com.metrolist.music.ui.shapes

import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

object CookieShapes {
    /**
     * Cookie shape with 4 sides.
     * Approximated using a 4-vertex star with high inner radius and rounding.
     * The naming follows the Material 3 "Cookie" concept.
     */
    val Cookie4Sided: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 4,
        innerRadius = 0.5f, // Tuned for "cookie" look
        rounding = CornerRounding(radius = 0.4f) // Soft rounding
    ).toComposeShape()

    val Cookie6Sided: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 6,
        innerRadius = 0.75f,
        rounding = CornerRounding(radius = 0.5f)
    ).toComposeShape()

    val Cookie7Sided: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 7,
        innerRadius = 0.75f,
        rounding = CornerRounding(radius = 0.5f)
    ).toComposeShape()

    val Cookie9Sided: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 9,
        innerRadius = 0.8f,
        rounding = CornerRounding(radius = 0.5f)
    ).toComposeShape()

    val Cookie12Sided: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 12,
        innerRadius = 0.8f,
        rounding = CornerRounding(radius = 0.5f)
    ).toComposeShape()
}
