package com.awayassist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Renders a dynamic, animated liquid mesh background with floating fluid gradient orbs.
 */
@Composable
fun LiquidMeshBackground(
    modifier: Modifier = Modifier,
    activeColor: Color = AwayAssistTheme.colors.accentSilent,
    isDark: Boolean = AwayAssistTheme.colors.isDark,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquidMeshAnimation")

    val orb1OffsetX by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1X"
    )

    val orb1OffsetY by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1Y"
    )

    val orb2OffsetX by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2X"
    )

    val orb2OffsetY by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2Y"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val baseBg = AwayAssistTheme.colors.background
    val cyan = AwayAssistTheme.colors.cyanGlow
    val azure = AwayAssistTheme.colors.azureGlow

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                val w = size.width
                val h = size.height

                // Fluid Orb 1 (Active Color Glow - top area)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = if (isDark) 0.35f else 0.18f),
                            activeColor.copy(alpha = if (isDark) 0.12f else 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.3f + orb1OffsetX * 0.4f), h * (0.2f + orb1OffsetY * 0.3f)),
                        radius = (w * 0.8f) * pulseScale
                    )
                )

                // Fluid Orb 2 (Cyan / Indigo fluid blend - middle right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cyan.copy(alpha = if (isDark) 0.22f else 0.14f),
                            azure.copy(alpha = if (isDark) 0.08f else 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.7f + orb2OffsetX * 0.3f), h * (0.45f + orb2OffsetY * 0.3f)),
                        radius = (w * 0.75f) * pulseScale
                    )
                )

                // Fluid Orb 3 (Bottom ambient glow)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            azure.copy(alpha = if (isDark) 0.18f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.2f, h * 0.85f),
                        radius = w * 0.7f
                    )
                )
            }
    ) {
        content()
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
                    tintColor?.copy(alpha = 0.22f) ?: Color(0x33202235),
                    tintColor?.copy(alpha = 0.10f) ?: Color(0x1F141524)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    tintColor?.copy(alpha = 0.14f) ?: Color(0xEBFFFFFF),
                    tintColor?.copy(alpha = 0.06f) ?: Color(0xC7F5F6FC)
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
                    Color(0x99FFFFFF),
                    Color(0x40FFFFFF),
                    Color(0x20FFFFFF),
                    Color(0x60FFFFFF)
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
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.08f,
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
            .background(tintColor.copy(alpha = 0.15f))
            .border(
                width = 0.8.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        tintColor.copy(alpha = 0.6f),
                        tintColor.copy(alpha = 0.2f)
                    )
                ),
                shape = SquirclePill
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
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
            .background(if (isDark) Color(0x20FFFFFF) else Color(0x14000000))
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0x30FFFFFF), Color(0x0AFFFFFF)) else listOf(Color(0x60FFFFFF), Color(0x18000000))
                ),
                shape = SquircleMedium
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                    .height(36.dp)
                    .clip(SquircleMedium)
                    .background(itemBgColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 0.8.dp,
                                color = if (isDark) Color(0x60FFFFFF) else Color(0x30000000),
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
                        modifier = Modifier.size(16.dp)
                    )
                    Box(modifier = Modifier.padding(start = 6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = itemTextColor
                    )
                }
            }
        }
    }
}
