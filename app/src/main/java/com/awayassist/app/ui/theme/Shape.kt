package com.awayassist.app.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Continuous curvature (Squircle) shape approximating Apple's superellipse curve.
 * This avoids abrupt curvature changes seen in standard circular fillets.
 */
class SquircleShape(val cornerRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val maxRadius = min(size.width, size.height) / 2f
        val r = min(radiusPx, maxRadius)

        if (r <= 0f) {
            val path = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            }
            return Outline.Generic(path)
        }

        val path = Path().apply {
            val w = size.width
            val h = size.height

            // Smooth cubic bezier continuous curvature factor
            // For iOS squircles, the transition starts further out along each edge
            val p = r * 0.55228475f

            moveTo(r, 0f)
            lineTo(w - r, 0f)
            cubicTo(
                w - r + p, 0f,
                w, r - p,
                w, r
            )
            lineTo(w, h - r)
            cubicTo(
                w, h - r + p,
                w - r + p, h,
                w - r, h
            )
            lineTo(r, h)
            cubicTo(
                r - p, h,
                0f, h - r + p,
                0f, h - r
            )
            lineTo(0f, r)
            cubicTo(
                0f, r - p,
                r - p, 0f,
                r, 0f
            )
            close()
        }

        return Outline.Generic(path)
    }
}

val SquircleSmall = SquircleShape(10.dp)
val SquircleMedium = SquircleShape(16.dp)
val SquircleLarge = SquircleShape(24.dp)
val SquircleXLarge = SquircleShape(32.dp)
val SquirclePill = SquircleShape(1000.dp)
