package com.metrolist.music.ui.shapes

import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ExpressiveShapes {
    // Standard Geom / Expressive from MaterialShapes
    val Slanted: Shape = MaterialShapes.Slanted.toComposeShape()
    val Arch: Shape = MaterialShapes.Arch.toComposeShape()
    val Fan: Shape = MaterialShapes.Fan.toComposeShape()
    val Arrow: Shape = MaterialShapes.Arrow.toComposeShape()
    val SemiCircle: Shape = MaterialShapes.SemiCircle.toComposeShape()
    val Oval: Shape = MaterialShapes.Oval.toComposeShape()
    val Pill: Shape = MaterialShapes.Pill.toComposeShape()
    
    val Diamond: Shape = MaterialShapes.Diamond.toComposeShape()
    val Clamshell: Shape = MaterialShapes.ClamShell.toComposeShape()
    val Gem: Shape = MaterialShapes.Gem.toComposeShape()
    val Puffy: Shape = MaterialShapes.Puffy.toComposeShape()
    val PuffyDiamond: Shape = MaterialShapes.PuffyDiamond.toComposeShape()
    val Ghostish: Shape = MaterialShapes.Ghostish.toComposeShape()
    val Bun: Shape = MaterialShapes.Bun.toComposeShape()
    
    // Pixel Shapes
    val PixelCircle: Shape = MaterialShapes.PixelCircle.toComposeShape()
    val PixelTriangle: Shape = MaterialShapes.PixelTriangle.toComposeShape()

    // Clovers
    val Clover4Leaf: Shape = MaterialShapes.Clover4Leaf.toComposeShape()
    val Clover8Leaf: Shape = MaterialShapes.Clover8Leaf.toComposeShape()

    // Cookies
    val Cookie4Sided: Shape = MaterialShapes.Cookie4Sided.toComposeShape()
    val Cookie6Sided: Shape = MaterialShapes.Cookie6Sided.toComposeShape()
    val Cookie7Sided: Shape = MaterialShapes.Cookie7Sided.toComposeShape()
    val Cookie9Sided: Shape = MaterialShapes.Cookie9Sided.toComposeShape()
    val Cookie12Sided: Shape = MaterialShapes.Cookie12Sided.toComposeShape()

    // Bursts & Stars
    val Flower: Shape = MaterialShapes.Flower.toComposeShape()
    val Burst: Shape = MaterialShapes.Burst.toComposeShape()
    val SoftBurst: Shape = MaterialShapes.SoftBurst.toComposeShape()
    val Boom: Shape = MaterialShapes.Boom.toComposeShape()
    val SoftBoom: Shape = MaterialShapes.SoftBoom.toComposeShape()
    
    val Sunny: Shape = MaterialShapes.Sunny.toComposeShape()
    val VerySunny: Shape = MaterialShapes.VerySunny.toComposeShape()

    // Custom
    val Heart: Shape = createHeartShape()
}
