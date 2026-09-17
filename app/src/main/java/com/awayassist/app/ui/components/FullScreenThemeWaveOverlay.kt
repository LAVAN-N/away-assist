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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

enum class ThemeTransitionType {
    EMIT_LIGHT,     // Expanding photon wavefront from bulb across entire screen
    ABSORB_LIGHT    // Seamless atmospheric contraction of light pulled back into bulb
}

// Silky, slow, cinematic easing curves for realistic fluid optical physics
private val SoftEmitEasing = CubicBezierEasing(0.18f, 0.88f, 0.28f, 1.0f)
private val SoftAbsorbEasing = CubicBezierEasing(0.38f, 0.05f, 0.22f, 1.0f)

/**
 * Full-screen optical theme transition effect with slow, graceful easing and pure gradient blending:
 * - Switching to Light Mode: A warm, expansive daylight illumination emits smoothly from the bulb center,
 *   gliding across the entire screen and illuminating cards, backgrounds, and headers with warm daylight.
 * - Switching to Dark Mode: Pure atmospheric blend—the light across the screen is drawn inward and absorbed
 *   seamlessly into the bulb without hard geometric circles, melting gracefully into the dark atmosphere.
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

                // 3. Soft Diffuse Leading Wavefront
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x60FFF59D).copy(alpha = 0.50f * (1f - progress)),
                            Color(0x25FFE082).copy(alpha = 0.25f * (1f - progress)),
                            Color.Transparent
                        ),
                        center = origin,
                        radius = currentRadius.coerceAtLeast(10f)
                    ),
                    center = origin,
                    radius = currentRadius
                )
            }

            ThemeTransitionType.ABSORB_LIGHT -> {
                // Pure atmospheric blend: Light across entire screen is gently sucked/absorbed back into bulb
                // Zero literal circle outlines/strokes — completely organic soft gradient falloff
                val currentRadius = maxRadius * (1f - progress)
                val concentration = (1f + progress * 1.3f).coerceIn(1f, 2.3f)
                val globalFade = (1f - progress * 0.5f).coerceIn(0f, 1f)

                // 1. Soft full-screen atmospheric warmth wash that fades as light is drawn into bulb
                drawRect(
                    color = Color(0x28FFE082).copy(alpha = 0.18f * (1f - progress))
                )

                // 2. Outer Atmospheric Twilight Blend Gradient (seamless diffuse falloff)
                val outerRadius = (maxRadius * (1f - progress * 0.70f)).coerceAtLeast(20f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x356366F1).copy(alpha = 0.22f * (1f - progress)),
                            Color(0x184F46E5).copy(alpha = 0.12f * (1f - progress)),
                            Color.Transparent
                        ),
                        center = origin,
                        radius = outerRadius
                    ),
                    center = origin,
                    radius = outerRadius
                )

                // 3. Contracting Warm Daylight Pool (Multi-stop gaussian-smooth radial blend)
                if (currentRadius > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF9C4).copy(alpha = (0.75f * concentration).coerceAtMost(0.95f) * globalFade),
                                Color(0xFFFFD54F).copy(alpha = 0.55f * globalFade),
                                Color(0x90FF8F00).copy(alpha = 0.35f * globalFade),
                                Color(0x406366F1).copy(alpha = 0.18f * (1f - progress)),
                                Color.Transparent
                            ),
                            center = origin,
                            radius = currentRadius.coerceAtLeast(15f)
                        ),
                        center = origin,
                        radius = currentRadius
                    )
                }

                // 4. Concentrated Inner Core Warmth (pulling inward toward bulb center)
                val innerCoreRadius = (currentRadius * 0.45f).coerceAtLeast(0f)
                if (innerCoreRadius > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFDE7).copy(alpha = 0.65f * (1f - progress * 0.25f)),
                                Color(0x85FFE082).copy(alpha = 0.40f * (1f - progress * 0.4f)),
                                Color(0x20FFB300).copy(alpha = 0.15f * (1f - progress)),
                                Color.Transparent
                            ),
                            center = origin,
                            radius = innerCoreRadius.coerceAtLeast(8f)
                        ),
                        center = origin,
                        radius = innerCoreRadius
                    )
                }

                // 5. Final Delicate Filament Dissipation at Bulb (progress > 0.70f)
                if (progress > 0.70f) {
                    val filamentProgress = ((progress - 0.70f) / 0.30f).coerceIn(0f, 1f)
                    val sparkRadius = (28.dp.toPx() * (1f - filamentProgress)).coerceAtLeast(0f)
                    if (sparkRadius > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFDE7).copy(alpha = 0.90f * (1f - filamentProgress)),
                                    Color(0x70FFB300).copy(alpha = 0.55f * (1f - filamentProgress)),
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
