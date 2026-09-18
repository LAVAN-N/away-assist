package com.awayassist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.data.ThemeMode
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleLarge
import com.awayassist.app.ui.theme.SquircleMedium
import com.awayassist.app.ui.theme.SquirclePill

/**
 * Renders a dynamic, animated liquid mesh background with floating fluid Notification Bell silhouettes
 * and radiating acoustic soundwaves reflecting the active ringer state.
 */
@Composable
fun LiquidMeshBackground(
    modifier: Modifier = Modifier,
    activeColor: Color = AwayAssistTheme.colors.accentSilent,
    isDark: Boolean = AwayAssistTheme.colors.isDark,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquidMeshAnimation")

    val bell1OffsetX by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell1X"
    )

    val bell1OffsetY by infiniteTransition.animateFloat(
        initialValue = -0.15f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell1Y"
    )

    val bell1Sway by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell1Sway"
    )

    val bell2OffsetX by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell2X"
    )

    val bell2OffsetY by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell2Y"
    )

    val bell2Sway by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell2Sway"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val soundwavePulse by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "soundwavePulse"
    )

    val baseBg = AwayAssistTheme.colors.background
    val cyan = if (isDark) AwayAssistTheme.colors.cyanGlow else Color(0xFFFFB300) // Warm amber gold in light mode
    val azure = if (isDark) AwayAssistTheme.colors.azureGlow else Color(0xFFFFA000) // Warm honey amber in light mode

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                val w = size.width
                val h = size.height

                // 0. Warm Incandescent Lamp-Lit Room Atmosphere for Light Mode
                if (!isDark) {
                    // Primary Lamp Light Dome (Overhead warm tungsten 2700K pool)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x50FFE082),
                                Color(0x28FFB300),
                                Color(0x10FF8F00),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.45f, h * 0.15f),
                            radius = w * 1.05f
                        )
                    )
                    // Secondary Ambient Room Bounce (Lower ambient warmth)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x35FFD54F),
                                Color(0x15FFA000),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.55f, h * 0.85f),
                            radius = w * 0.90f
                        )
                    )
                }

                // 1. Primary Active Notification Bell (Upper Ambient Region)
                val primaryCenter = Offset(
                    w * (0.32f + bell1OffsetX * 0.4f),
                    h * (0.22f + bell1OffsetY * 0.3f)
                )
                val primarySize = Size(
                    w * 0.75f * pulseScale,
                    w * 0.85f * pulseScale
                )

                // Ambient Radial Glow under Primary Bell
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = if (isDark) 0.32f else 0.22f),
                            activeColor.copy(alpha = if (isDark) 0.10f else 0.09f),
                            Color.Transparent
                        ),
                        center = primaryCenter,
                        radius = (w * 0.65f) * pulseScale
                    )
                )

                // Primary Fluid Notification Bell Silhouette
                drawFluidNotificationBell(
                    center = primaryCenter,
                    size = primarySize,
                    rotationDegrees = bell1Sway,
                    fillBrush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = if (isDark) 0.30f else 0.20f),
                            activeColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                            Color.Transparent
                        ),
                        center = primaryCenter,
                        radius = primarySize.width * 0.6f
                    ),
                    strokeBrush = Brush.linearGradient(
                        colors = listOf(
                            activeColor.copy(alpha = if (isDark) 0.35f else 0.28f),
                            Color.Transparent,
                            activeColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                        ),
                        start = Offset(primaryCenter.x - primarySize.width * 0.5f, primaryCenter.y - primarySize.height * 0.5f),
                        end = Offset(primaryCenter.x + primarySize.width * 0.5f, primaryCenter.y + primarySize.height * 0.5f)
                    ),
                    strokeWidth = 1.8.dp.toPx()
                )

                // Radiating Acoustic Chime Soundwave Arcs from Primary Bell
                drawAcousticSoundwaves(
                    center = primaryCenter,
                    bellWidth = primarySize.width,
                    bellHeight = primarySize.height,
                    scale = soundwavePulse,
                    color = activeColor.copy(alpha = if (isDark) 0.20f else 0.16f),
                    rotationDegrees = bell1Sway
                )

                // 2. Secondary Floating Notification Bell (Mid-Right Region)
                val secondaryCenter = Offset(
                    w * (0.68f + bell2OffsetX * 0.3f),
                    h * (0.48f + bell2OffsetY * 0.25f)
                )
                val secondarySize = Size(
                    w * 0.60f * pulseScale,
                    w * 0.68f * pulseScale
                )

                drawFluidNotificationBell(
                    center = secondaryCenter,
                    size = secondarySize,
                    rotationDegrees = bell2Sway,
                    fillBrush = Brush.radialGradient(
                        colors = listOf(
                            cyan.copy(alpha = if (isDark) 0.22f else 0.18f),
                            azure.copy(alpha = if (isDark) 0.07f else 0.08f),
                            Color.Transparent
                        ),
                        center = secondaryCenter,
                        radius = secondarySize.width * 0.55f
                    ),
                    strokeBrush = Brush.linearGradient(
                        colors = listOf(
                            cyan.copy(alpha = if (isDark) 0.28f else 0.22f),
                            Color.Transparent,
                            azure.copy(alpha = if (isDark) 0.12f else 0.10f)
                        )
                    ),
                    strokeWidth = 1.4.dp.toPx()
                )

                // 3. Lower Ambient Floating Soundwave Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            azure.copy(alpha = if (isDark) 0.16f else 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.22f, h * 0.82f),
                        radius = w * 0.65f
                    )
                )
            }
    ) {
        content()
    }
}

/**
 * Draws a fluid, organic Notification Bell silhouette with dome, flaring body, and clapper.
 */
private fun DrawScope.drawFluidNotificationBell(
    center: Offset,
    size: Size,
    rotationDegrees: Float,
    fillBrush: Brush,
    strokeBrush: Brush? = null,
    strokeWidth: Float = 0f
) {
    rotate(degrees = rotationDegrees, pivot = center) {
        val cx = center.x
        val cy = center.y
        val w = size.width
        val h = size.height

        val bellPath = Path().apply {
            val topLoopRadius = w * 0.075f
            val topY = cy - h * 0.44f
            val domeTopY = cy - h * 0.36f
            val waistY = cy + h * 0.05f
            val flareY = cy + h * 0.30f
            val lipBottomY = cy + h * 0.34f

            // Top hanger loop
            moveTo(cx, topY)
            cubicTo(
                cx + topLoopRadius * 1.3f, topY,
                cx + topLoopRadius * 1.3f, domeTopY,
                cx, domeTopY
            )
            cubicTo(
                cx - topLoopRadius * 1.3f, domeTopY,
                cx - topLoopRadius * 1.3f, topY,
                cx, topY
            )

            // Dome top and right shoulder
            moveTo(cx, domeTopY)
            cubicTo(
                cx + w * 0.26f, domeTopY + h * 0.03f,
                cx + w * 0.22f, waistY,
                cx + w * 0.42f, flareY
            )
            // Right flare lip
            cubicTo(
                cx + w * 0.44f, lipBottomY,
                cx + w * 0.34f, lipBottomY,
                cx + w * 0.18f, lipBottomY
            )
            // Bottom clapper dome
            val clapperRadius = w * 0.09f
            cubicTo(
                cx + clapperRadius, lipBottomY,
                cx + clapperRadius, lipBottomY + h * 0.09f,
                cx, lipBottomY + h * 0.09f
            )
            cubicTo(
                cx - clapperRadius, lipBottomY + h * 0.09f,
                cx - clapperRadius, lipBottomY,
                cx - w * 0.18f, lipBottomY
            )
            // Left flare lip
            cubicTo(
                cx - w * 0.34f, lipBottomY,
                cx - w * 0.44f, lipBottomY,
                cx - w * 0.42f, flareY
            )
            // Left waist and shoulder
            cubicTo(
                cx - w * 0.22f, waistY,
                cx - w * 0.26f, domeTopY + h * 0.03f,
                cx, domeTopY
            )
            close()
        }

        drawPath(path = bellPath, brush = fillBrush)
        if (strokeBrush != null && strokeWidth > 0f) {
            drawPath(path = bellPath, brush = strokeBrush, style = Stroke(width = strokeWidth))
        }
    }
}

/**
 * Draws concentric radiating soundwave / chime wave arcs around the notification bell.
 */
private fun DrawScope.drawAcousticSoundwaves(
    center: Offset,
    bellWidth: Float,
    bellHeight: Float,
    scale: Float,
    color: Color,
    rotationDegrees: Float
) {
    rotate(degrees = rotationDegrees, pivot = center) {
        val cx = center.x
        val cy = center.y

        // Left Chime Wave Arc
        val leftArcRadius = (bellWidth * 0.52f) * scale
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - leftArcRadius - bellWidth * 0.15f, cy - leftArcRadius * 0.7f),
            size = Size(leftArcRadius * 2f, leftArcRadius * 1.4f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Right Chime Wave Arc
        val rightArcRadius = (bellWidth * 0.52f) * scale
        drawArc(
            color = color,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - rightArcRadius + bellWidth * 0.15f, cy - rightArcRadius * 0.7f),
            size = Size(rightArcRadius * 2f, rightArcRadius * 1.4f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Outer Secondary Wave Pulse
        val outerRadius = (bellWidth * 0.70f) * scale
        drawArc(
            color = color.copy(alpha = color.alpha * 0.5f),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - outerRadius - bellWidth * 0.22f, cy - outerRadius * 0.7f),
            size = Size(outerRadius * 2f, outerRadius * 1.4f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = color.copy(alpha = color.alpha * 0.5f),
            startAngle = -35f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - outerRadius + bellWidth * 0.22f, cy - outerRadius * 0.7f),
            size = Size(outerRadius * 2f, outerRadius * 1.4f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Custom glassmorphism modifier adding specular reflection rim and frosted gradient surface.
 */
fun Modifier.glassmorphic(
    shape: Shape = SquircleLarge,
    borderWidth: Dp = 1.dp,
    tintColor: Color? = null,
    isDark: Boolean = true
): Modifier = this
    .clip(shape)
    .drawBehind {
        val w = size.width
        val h = size.height

        val surfaceGradient = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    tintColor?.copy(alpha = 0.20f) ?: Color(0x33202235),
                    tintColor?.copy(alpha = 0.08f) ?: Color(0x1F141524)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    tintColor?.copy(alpha = 0.20f) ?: Color(0xEBFFFDF8),
                    tintColor?.copy(alpha = 0.09f) ?: Color(0xD6FAF2E4)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        }
        drawRect(brush = surfaceGradient)
    }
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color(0x55FFFFFF),
                    Color(0x18FFFFFF),
                    Color(0x0AFFFFFF),
                    Color(0x22FFFFFF)
                )
            } else {
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xD0FFF1D0),
                    Color(0x75FFD580),
                    Color(0xC5FFFFFF)
                )
            },
            start = Offset(0f, 0f),
            end = Offset(400f, 600f)
        ),
        shape = shape
    )

/**
 * Liquid Pulsing Halo ring for active status indicators.
 */
@Composable
fun LiquidPulsingHalo(
    modifier: Modifier = Modifier,
    glowColor: Color = AwayAssistTheme.colors.ringState,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = glowColor.copy(alpha = haloAlpha),
                radius = (size.minDimension / 2f) * haloScale
            )
        }
        content()
    }
}

/**
 * Liquid Glass Pill Badge.
 */
@Composable
fun LiquidPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    tintColor: Color = AwayAssistTheme.colors.accentSilent
) {
    Box(
        modifier = modifier
            .clip(SquirclePill)
            .background(tintColor.copy(alpha = 0.12f))
            .border(
                width = 0.8.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        tintColor.copy(alpha = 0.55f),
                        tintColor.copy(alpha = 0.20f)
                    )
                ),
                shape = SquirclePill
            )
            .padding(horizontal = 9.dp, vertical = 3.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp
            ),
            color = tintColor
        )
    }
}

/**
 * Liquid Segmented Theme Selector (System / Light / Dark).
 */
@Composable
fun LiquidThemeSelector(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors
    val isDark = colors.isDark

    val options = listOf(
        Triple(ThemeMode.SYSTEM, "System", Icons.Default.BrightnessAuto),
        Triple(ThemeMode.LIGHT, "Light", Icons.Default.LightMode),
        Triple(ThemeMode.DARK, "Dark", Icons.Default.DarkMode)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleMedium)
            .background(if (isDark) Color(0x1CFFFFFF) else Color(0x10000000))
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0x30FFFFFF), Color(0x0AFFFFFF)) else listOf(Color(0x60FFFFFF), Color(0x18000000))
                ),
                shape = SquircleMedium
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (mode, label, icon) ->
            val isSelected = currentTheme == mode
            val interactionSource = remember { MutableInteractionSource() }

            val itemBgColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) Color(0x40FFFFFF) else Color.White
                } else {
                    Color.Transparent
                },
                animationSpec = spring(),
                label = "themeSegmentBg"
            )

            val itemTextColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) Color.White else colors.accentSilent
                } else {
                    colors.textSecondary
                },
                animationSpec = spring(),
                label = "themeSegmentText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(SquircleMedium)
                    .background(itemBgColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 0.8.dp,
                                color = if (isDark) Color(0x55FFFFFF) else Color(0x28000000),
                                shape = SquircleMedium
                            )
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onThemeSelected(mode) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = itemTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Box(modifier = Modifier.padding(start = 5.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        color = itemTextColor
                    )
                }
            }
        }
    }
}
