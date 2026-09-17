package com.awayassist.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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

// Silky, slow, cinematic easing curves for realistic fluid optical physics
private val SoftEmitEasing = CubicBezierEasing(0.18f, 0.88f, 0.28f, 1.0f)
private val SoftAbsorbEasing = CubicBezierEasing(0.38f, 0.05f, 0.22f, 1.0f)

/**
 * Full-screen optical theme transition effect with slow, graceful easing:
 * - Switching to Light Mode: A slow, warm, expansive photon wave emits smoothly from the bulb center,
 *   gliding across the entire screen and illuminating cards, backgrounds, and headers with warm daylight.
 * - Switching to Dark Mode: The reverse optical phenomenon—light across the whole screen is drawn inward slowly,
 *   absorbed and swallowed gracefully back into the bulb until the filament extinguishes into deep twilight.
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
            val isEnteringLight = !isDark
            activeTransition = if (isEnteringLight) ThemeTransitionType.EMIT_LIGHT else ThemeTransitionType.ABSORB_LIGHT
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1600,
                    easing = if (isEnteringLight) SoftEmitEasing else SoftAbsorbEasing
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
                // Expanding photon wavefront radiating slowly and smoothly from bulb
                val currentRadius = maxRadius * progress
                val fade = (1f - progress * 0.80f).coerceIn(0f, 1f)

                // 1. Soft Ambient Daylight Wash over Full Viewport
                drawRect(
                    color = Color(0xFFFFFAEB).copy(alpha = 0.16f * (1f - progress * 0.9f))
                )

                // 2. Expanding Golden Dawn Aura Pool (Deep Radial Layering)
                if (currentRadius > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF9C4).copy(alpha = 0.65f * fade),
                                Color(0xFFFFE082).copy(alpha = 0.48f * fade),
                                Color(0x80FFB300).copy(alpha = 0.32f * fade),
                                Color(0x25FF8F00).copy(alpha = 0.14f * fade),
                                Color.Transparent
                            ),
                            center = origin,
                            radius = currentRadius.coerceAtLeast(10f)
                        ),
                        center = origin,
                        radius = currentRadius
                    )
                }

                // 3. Incandescent Leading Wavefront Shockwave Rim (Primary Edge)
                drawCircle(
                    color = Color(0xFFFFF59D).copy(alpha = 0.80f * (1f - progress)),
                    center = origin,
                    radius = currentRadius,
                    style = Stroke(
                        width = (4.5.dp.toPx() * (1f - progress * 0.5f)).coerceAtLeast(1f)
                    )
                )

                // 4. Secondary Trailing Harmonic Ripple
                val trailRadius1 = (currentRadius * 0.82f).coerceAtLeast(0f)
                if (trailRadius1 > 0f) {
                    drawCircle(
                        color = Color(0x90FFE082).copy(alpha = 0.50f * (1f - progress)),
                        center = origin,
                        radius = trailRadius1,
                        style = Stroke(
                            width = (2.5.dp.toPx() * (1f - progress)).coerceAtLeast(0.5f)
                        )
                    )
                }

                // 5. Tertiary Ambient Ripple
                val trailRadius2 = (currentRadius * 0.62f).coerceAtLeast(0f)
                if (trailRadius2 > 0f) {
                    drawCircle(
                        color = Color(0x60FFD54F).copy(alpha = 0.35f * (1f - progress)),
                        center = origin,
                        radius = trailRadius2,
                        style = Stroke(
                            width = (1.8.dp.toPx() * (1f - progress)).coerceAtLeast(0.5f)
                        )
                    )
                }
            }

            ThemeTransitionType.ABSORB_LIGHT -> {
                // Reverse phenomenon: Light across entire screen is gently sucked/absorbed back into bulb
                val currentRadius = maxRadius * (1f - progress)
                val concentration = (1f + progress * 1.4f).coerceIn(1f, 2.4f)
                val fadeOut = (1f - progress * 0.35f).coerceIn(0f, 1f)

                // 1. Contracting Light Pool being drawn gracefully into the bulb
                if (currentRadius > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF9C4).copy(alpha = (0.75f * concentration).coerceAtMost(0.96f)),
                                Color(0xFFFFD54F).copy(alpha = 0.62f * fadeOut),
                                Color(0x95FF8F00).copy(alpha = 0.40f * fadeOut),
                                Color(0x556366F1).copy(alpha = 0.28f * fadeOut),
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
                val ringColor = if (progress < 0.65f) Color(0xFFFFF59D) else Color(0xFF818CF8)
                drawCircle(
                    color = ringColor.copy(alpha = (0.85f * (1f - progress * 0.20f)).coerceIn(0f, 1f)),
                    center = origin,
                    radius = currentRadius.coerceAtLeast(2f),
                    style = Stroke(
                        width = (3.8.dp.toPx() * (1f + progress * 0.7f)).coerceAtLeast(1.2f)
                    )
                )

                // 3. Inward Vacuum Streamer Chime Rings
                val innerVortexRadius = (currentRadius * 0.50f).coerceAtLeast(0f)
                if (innerVortexRadius > 0f) {
                    drawCircle(
                        color = Color(0x80818CF8).copy(alpha = 0.50f * (1f - progress * 0.4f)),
                        center = origin,
                        radius = innerVortexRadius,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 4. Final Pinpoint Implosion Spark at Bulb Filament (progress > 0.75f)
                if (progress > 0.75f) {
                    val filamentProgress = ((progress - 0.75f) / 0.25f).coerceIn(0f, 1f)
                    val sparkRadius = (22.dp.toPx() * (1f - filamentProgress)).coerceAtLeast(0f)
                    if (sparkRadius > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFDE7).copy(alpha = 1f - filamentProgress),
                                    Color(0xFFFFB300).copy(alpha = 0.85f * (1f - filamentProgress)),
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
