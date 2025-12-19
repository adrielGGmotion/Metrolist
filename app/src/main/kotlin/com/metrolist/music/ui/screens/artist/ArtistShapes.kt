package com.metrolist.music.ui.screens.artist

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

fun getArtistShapes(): List<Shape> {
    return listOf(
        FourSidedCookieShape(),
        GhostishShape(),
        BunShape(),
        DropletShape(),
        SpikeShape(),
        LopsidedStarShape(),
        BlobShape(),
        DiamondShape(),
        KiteShape(),
        PillShape(),
        HeartShape(),
        StarTwinkleShape(),
        ExplosionShape(),
        TriangleShape(),
        HeptagonShape()
    )
}

class FourSidedCookieShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            cubicTo(size.width * 0.8f, 0f, size.width, size.height * 0.2f, size.width, size.height * 0.5f)
            cubicTo(size.width, size.height * 0.8f, size.width * 0.8f, size.height, size.width * 0.5f, size.height)
            cubicTo(size.width * 0.2f, size.height, 0f, size.height * 0.8f, 0f, size.height * 0.5f)
            cubicTo(0f, size.height * 0.2f, size.width * 0.2f, 0f, size.width * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

class GhostishShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            cubicTo(size.width * 0.9f, 0f, size.width, size.height * 0.4f, size.width, size.height * 0.8f)
            quadraticTo(size.width, size.height, size.width * 0.5f, size.height)
            quadraticTo(0f, size.height, 0f, size.height * 0.8f)
            cubicTo(0f, size.height * 0.4f, size.width * 0.1f, 0f, size.width * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

class BunShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            cubicTo(size.width * 0.8f, 0f, size.width, size.height * 0.3f, size.width, size.height * 0.6f)
            quadraticTo(size.width, size.height, size.width * 0.5f, size.height)
            quadraticTo(0f, size.height, 0f, size.height * 0.6f)
            cubicTo(0f, size.height * 0.3f, size.width * 0.2f, 0f, size.width * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

class DropletShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            cubicTo(size.width, 0f, size.width, size.height, size.width * 0.5f, size.height)
            cubicTo(0f, size.height, 0f, 0f, size.width * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

class SpikeShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

class LopsidedStarShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width * 0.6f, size.height * 0.4f)
            lineTo(size.width, size.height * 0.5f)
            lineTo(size.width * 0.7f, size.height * 0.7f)
            lineTo(size.width * 0.8f, size.height)
            lineTo(size.width * 0.5f, size.height * 0.8f)
            lineTo(size.width * 0.2f, size.height)
            lineTo(size.width * 0.3f, size.height * 0.7f)
            lineTo(0f, size.height * 0.5f)
            lineTo(size.width * 0.4f, size.height * 0.4f)
            close()
        }
        return Outline.Generic(path)
    }
}

class BlobShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            cubicTo(size.width * 0.8f, 0f, size.width, size.height * 0.2f, size.width, size.height * 0.5f)
            cubicTo(size.width, size.height * 0.8f, size.width * 0.8f, size.height, size.width * 0.5f, size.height)
            cubicTo(size.width * 0.2f, size.height, 0f, size.height * 0.8f, 0f, size.height * 0.5f)
            cubicTo(0f, size.height * 0.2f, size.width * 0.2f, 0f, size.width * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

class DiamondShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width, size.height * 0.5f)
            lineTo(size.width * 0.5f, size.height)
            lineTo(0f, size.height * 0.5f)
            close()
        }
        return Outline.Generic(path)
    }
}

class KiteShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width, size.height * 0.7f)
            lineTo(size.width * 0.5f, size.height)
            lineTo(0f, size.height * 0.7f)
            close()
        }
        return Outline.Generic(path)
    }
}

class PillShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val cornerRadius = size.height / 2
            addOval(
                androidx.compose.ui.geometry.Rect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height
                )
            )
        }
        return Outline.Generic(path)
    }
}

class HeartShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.2f)
            cubicTo(size.width * 0.2f, 0f, 0f, size.height * 0.3f, 0f, size.height * 0.5f)
            cubicTo(0f, size.height * 0.8f, size.width * 0.5f, size.height, size.width * 0.5f, size.height)
            cubicTo(size.width * 0.5f, size.height, size.width, size.height * 0.8f, size.width, size.height * 0.5f)
            cubicTo(size.width, size.height * 0.3f, size.width * 0.8f, 0f, size.width * 0.5f, size.height * 0.2f)
            close()
        }
        return Outline.Generic(path)
    }
}

class StarTwinkleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width * 0.6f, size.height * 0.4f)
            lineTo(size.width, size.height * 0.5f)
            lineTo(size.width * 0.6f, size.height * 0.6f)
            lineTo(size.width * 0.5f, size.height)
            lineTo(size.width * 0.4f, size.height * 0.6f)
            lineTo(0f, size.height * 0.5f)
            lineTo(size.width * 0.4f, size.height * 0.4f)
            close()
        }
        return Outline.Generic(path)
    }
}

class ExplosionShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width * 0.7f, size.height * 0.3f)
            lineTo(size.width, size.height * 0.3f)
            lineTo(size.width * 0.8f, size.height * 0.5f)
            lineTo(size.width, size.height * 0.7f)
            lineTo(size.width * 0.7f, size.height * 0.7f)
            lineTo(size.width * 0.5f, size.height)
            lineTo(size.width * 0.3f, size.height * 0.7f)
            lineTo(0f, size.height * 0.7f)
            lineTo(size.width * 0.2f, size.height * 0.5f)
            lineTo(0f, size.height * 0.3f)
            lineTo(size.width * 0.3f, size.height * 0.3f)
            close()
        }
        return Outline.Generic(path)
    }
}

class TriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

class HeptagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val radius = size.width / 2
            val angle = 2 * Math.PI / 7
            moveTo(
                (radius * kotlin.math.cos(0f) + radius).toFloat(),
                (radius * kotlin.math.sin(0f) + radius).toFloat()
            )
            for (i in 1 until 7) {
                lineTo(
                    (radius * kotlin.math.cos(angle * i) + radius).toFloat(),
                    (radius * kotlin.math.sin(angle * i) + radius).toFloat()
                )
            }
            close()
        }
        return Outline.Generic(path)
    }
}
