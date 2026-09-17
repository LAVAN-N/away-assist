package com.awayassist.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

enum class ThemeTransitionType {
    EMIT_LIGHT,     // Expanding photon wavefront from bulb across entire screen
    ABSORB_LIGHT    // Collapsing light disk sucked from entire screen into bulb
}

/**
 * Full-screen optical theme transition effect:
 * - Switching to Light Mode: An expansive, luminous photon wave emits from the bulb center,
 *   sweeping across the entire screen and illuminating cards, backgrounds, and headers with warm daylight.
 * - Switching to Dark Mode: The reverse optical phenomenon—light across the whole screen is drawn inward,
 *   absorbed and swallowed back into the bulb until the filament extinguishes into twilight.
 */
@Composable
fun FullScreenThemeWaveOverlay(
    isDark: Boolean,
    bulbScreenPosition: Offset?,
    modifier: Modifier = Modifier
) {
    var previousDarkState by remember { mutableStateOf<Boolean?>(null) }
    val animProgress = remember { Animatable(0f) }
    var activeTransition by remember { mutableStateOf<ThemeTransitionType?>(null) }

    LaunchedEffect(isDark) {
        if (previousDarkState != null && previousDarkState != isDark) {
            activeTransition = if (!isDark) ThemeTransitionType.EMIT_LIGHT else ThemeTransitionType.ABSORB_LIGHT
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 850,
                    easing = FastOutSlowInEasing
                )
            )
            activeTransition = null
        }
        previousDarkState = isDark
    }

    val currentTransition = activeTransition ?: return

    Canvas(modifier = modifier.fillMaxSize()) {
        val origin = bulbScreenPosition ?: Offset(size.width * 0.44f, size.height * 0.48f)
        val progress = animProgress.value
        val maxRadius = hypot(size.width, size.height) * 1.35f

        when (currentTransition) {
            ThemeTransitionType.EMIT_LIGHT -> {
                // Expanding photon wavefront radiating from bulb across whole screen
                val currentRadius = maxRadius * progress
                val fade = (1f - progress * 0.85f).coerceIn(0f, 1f)

                // 1. Full Screen Soft Sunlight Wash
                drawRect(
                    color = Color(0xFFFFFAEB).copy(alpha = 0.18f * (1f - progress * progress))
                )

                // 2. Expanding Golden Dawn Aura Pool
                if (currentRadius > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF9C4).copy(alpha = 0.65f * fade),
                                Color(0xFFFFE082).copy(alpha = 0.50f * fade),
                                Color(0x80FFB300).copy(alpha = 0.30f * fade),
                                Color(0x20FF8F00).copy(alpha = 0.12f * fade),
                                Color.Transparent
                            ),
                            center = origin,
                            radius = currentRadius.coerceAtLeast(10f)
                        ),
                        center = origin,
                        radius = currentRadius
                    )
                }

                // 3. Incandescent Leading Wavefront Shockwave Rim
                drawCircle(
                    color = Color(0xFFFFF59D).copy(alpha = 0.85f * (1f - progress)),
                    center = origin,
                    radius = currentRadius,
                    style = Stroke(
                        width = (4.dp.toPx() * (1f - progress * 0.5f)).coerceAtLeast(1f)
                    )
                )

                // 4. Secondary Trailing Harmonic Wave
                val trailRadius = (currentRadius * 0.78f).coerceAtLeast(0f)
                if (trailRadius > 0f) {
                    drawCircle(
                        color = Color(0x90FFE082).copy(alpha = 0.55f * (1f - progress)),
                        center = origin,
                        radius = trailRadius,
                        style = Stroke(
                            width = (2.2.dp.toPx() * (1f - progress)).coerceAtLeast(0.5f)
                        )
                    )
                }
            }

            ThemeTransitionType.ABSORB_LIGHT -> {
                // Reverse phenomenon: Light across entire screen is sucked/absorbed back into the bulb
                val currentRadius = maxRadius * (1f - progress)
                val concentration = (1f + progress * 1.2f).coerceIn(1f, 2.2f)

                // 1. Contracting Light Pool being drawn into the bulb
                if (currentRadius > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF9C4).copy(alpha = (0.75f * concentration).coerceAtMost(0.95f)),
                                Color(0xFFFFD54F).copy(alpha = 0.60f),
                                Color(0x90FF8F00).copy(alpha = 0.38f),
                                Color(0x506366F1).copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = origin,
                            radius = currentRadius.coerceAtLeast(15f)
                        ),
                        center = origin,
                        radius = currentRadius
                    )
                }

                // 2. Contracting Event Horizon Perimeter Ring (Luminous Inrush Ring)
                val ringColor = if (progress < 0.7f) Color(0xFFFFF59D) else Color(0xFF818CF8)
                drawCircle(
                    color = ringColor.copy(alpha = (0.85f * (1f - progress * 0.25f)).coerceIn(0f, 1f)),
                    center = origin,
                    radius = currentRadius.coerceAtLeast(2f),
                    style = Stroke(
                        width = (3.5.dp.toPx() * (1f + progress * 0.6f)).coerceAtLeast(1.2f)
                    )
                )

                // 3. Inward Vacuum Streamer Chime Rings
                val innerVortexRadius = (currentRadius * 0.52f).coerceAtLeast(0f)
                if (innerVortexRadius > 0f) {
                    drawCircle(
                        color = Color(0x80818CF8).copy(alpha = 0.50f * (1f - progress * 0.5f)),
                        center = origin,
                        radius = innerVortexRadius,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 4. Final Pinpoint Implosion Spark at Bulb Filament (progress > 0.80f)
                if (progress > 0.80f) {
                    val filamentProgress = ((progress - 0.80f) / 0.20f).coerceIn(0f, 1f)
                    val sparkRadius = (20.dp.toPx() * (1f - filamentProgress)).coerceAtLeast(0f)
                    if (sparkRadius > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFDE7).copy(alpha = 1f - filamentProgress),
                                    Color(0xFFFFB300).copy(alpha = 0.8f * (1f - filamentProgress)),
                                    Color.Transparent
                                ),
                                center = origin,
                                radius = sparkRadius
                            ),
                            center = origin,
                            radius = sparkRadius
                        )
                    }
                }
            }
        }
    }
}
