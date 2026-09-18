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
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

enum class ThemeTransitionType {
    EMIT_LIGHT,     // Synchronous radial illumination revealing light mode outward from bulb
    ABSORB_LIGHT    // Synchronous radial contraction absorbing daylight back into bulb
}

// Silky, slow, cinematic easing curves for realistic fluid optical physics
private val SoftEmitEasing = CubicBezierEasing(0.20f, 0.90f, 0.30f, 1.0f)
private val SoftAbsorbEasing = CubicBezierEasing(0.38f, 0.05f, 0.22f, 1.0f)

/**
 * Full-screen synchronous optical theme transition overlay:
 *
 * - Dark -> Light Mode:
 *   Prevents the screen from immediately snapping to white. Instead, the dark atmosphere
 *   is held across the screen and smoothly, continuously pulled back in a radial aperture
 *   expanding outward from the bulb, seamlessly revealing the light theme in sync with
 *   the warm golden dawn emission wave.
 *
 * - Light -> Dark Mode:
 *   Maintains a daylight atmospheric veil that smoothly contracts inward from the screen
 *   edges and corners toward the bulb, revealing the dark theme in its wake and absorbing
 *   the light gracefully into the bulb filament.
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
            try {
                animProgress.snapTo(0f)
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1600,
                        easing = if (isEnteringLight) SoftEmitEasing else SoftAbsorbEasing
                    )
                )
            } finally {
                activeTransition = null
            }
        }
        previousDarkState = isDark
    }

    val currentTransition = activeTransition ?: return

    Canvas(modifier = modifier.fillMaxSize()) {
        val origin = bulbScreenPosition ?: Offset(size.width * 0.44f, size.height * 0.48f)
        val progress = animProgress.value
        val maxRadius = hypot(size.width, size.height) * 1.35f
        val feather = (200.dp.toPx()).coerceAtLeast(100f)

        when (currentTransition) {
            ThemeTransitionType.EMIT_LIGHT -> {
                // Expanding photon wave revealing light theme outward from bulb
                val revealRadius = maxRadius * progress
                val gradientRadius = (revealRadius + feather).coerceAtLeast(10f)

                val stopTransparent = (revealRadius / gradientRadius).coerceIn(0f, 0.95f)
                val stopMid = ((revealRadius + feather * 0.45f) / gradientRadius).coerceIn(stopTransparent, 0.98f)

                // 1. Dark Atmospheric Veil: Masks light theme and expands radially from bulb
                if (progress < 0.999f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                stopTransparent to Color.Transparent,
                                stopMid to Color(0xD8090A10),
                                1.0f to Color(0xFF090A10)
                            ),
                            center = origin,
                            radius = gradientRadius
                        )
                    )
                }

                // 2. Warm Photonic Sunrise Aura at the opening aperture
                val auraRadius = (revealRadius + feather * 0.5f).coerceAtLeast(10f)
                val fade = (1f - progress * 0.70f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x95FFF9C4).copy(alpha = 0.60f * fade),
                            Color(0x65FFE082).copy(alpha = 0.42f * fade),
                            Color(0x35FFB300).copy(alpha = 0.24f * fade),
                            Color(0x12FF8F00).copy(alpha = 0.09f * fade),
                            Color.Transparent
                        ),
                        center = origin,
                        radius = auraRadius
                    ),
                    center = origin,
                    radius = auraRadius
                )

                // 3. Diffuse Ambient Wavefront leading edge
                if (revealRadius > 0f) {
                    val waveInnerStop = (revealRadius * 0.85f / gradientRadius).coerceIn(0f, 0.9f)
                    val wavePeakStop = (revealRadius / gradientRadius).coerceIn(waveInnerStop, 0.95f)
                    val waveOuterStop = ((revealRadius + feather * 0.35f) / gradientRadius).coerceIn(wavePeakStop, 1.0f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                waveInnerStop to Color.Transparent,
                                wavePeakStop to Color(0x65FFF59D).copy(alpha = 0.50f * (1f - progress)),
                                waveOuterStop to Color.Transparent,
                                1.0f to Color.Transparent
                            ),
                            center = origin,
                            radius = gradientRadius
                        ),
                        center = origin,
                        radius = gradientRadius
                    )
                }
            }

            ThemeTransitionType.ABSORB_LIGHT -> {
                // Contracting daylight pool absorbed smoothly back into bulb
                val activeRadius = maxRadius * (1f - progress)
                val gradientRadius = (activeRadius + feather).coerceAtLeast(10f)

                val stopInner = ((activeRadius * 0.65f) / gradientRadius).coerceIn(0f, 0.9f)
                val stopEdge = (activeRadius / gradientRadius).coerceIn(stopInner, 0.95f)
                val globalFade = (1f - progress * 0.40f).coerceIn(0f, 1f)

                // 1. Daylight Atmosphere Veil contracting smoothly inward toward the bulb
                if (progress > 0.001f && progress < 0.999f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFFF6EADB).copy(alpha = 0.94f * (1f - progress * 0.25f)),
                                stopInner to Color(0xFFF6EADB).copy(alpha = 0.85f * (1f - progress * 0.35f)),
                                stopEdge to Color(0x80F6EADB).copy(alpha = 0.50f * (1f - progress * 0.6f)),
                                1.0f to Color.Transparent
                            ),
                            center = origin,
                            radius = gradientRadius
                        )
                    )
                }

                // 2. Contracting Warm Daylight Pool (Multi-stop gaussian-smooth radial blend)
                if (activeRadius > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF9C4).copy(alpha = 0.70f * globalFade),
                                Color(0xFFFFD54F).copy(alpha = 0.52f * globalFade),
                                Color(0x90FF8F00).copy(alpha = 0.32f * globalFade),
                                Color(0x356366F1).copy(alpha = 0.15f * (1f - progress)),
                                Color.Transparent
                            ),
                            center = origin,
                            radius = activeRadius.coerceAtLeast(15f)
                        ),
                        center = origin,
                        radius = activeRadius
                    )
                }

                // 3. Concentrated Inner Core Warmth (pulling inward toward bulb center)
                val innerCoreRadius = (activeRadius * 0.45f).coerceAtLeast(0f)
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

                // 4. Final Delicate Filament Dissipation at Bulb (progress > 0.70f)
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
