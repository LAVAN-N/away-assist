package com.awayassist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.data.ThemeMode
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleMedium
import com.awayassist.app.ui.theme.SquirclePill
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Realistic Vintage Pull-Chain Light Switch Toggle.
 *
 * Simulates a classic Edison lamp fixture with a brass beaded pull-chain.
 * Features realistic drag physics, spring tension, harmonic oscillation bounce-back,
 * and responsive illumination effects for Light and Dark modes.
 */
@Composable
fun VintagePullLightToggle(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = AwayAssistTheme.colors.isDark
) {
    val colors = AwayAssistTheme.colors
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val isCurrentlyLit = currentTheme == ThemeMode.LIGHT

    // Physical measurements in px
    val maxPullPx = with(density) { 68.dp.toPx() }
    val thresholdPx = with(density) { 38.dp.toPx() }

    // Spring physics animatables
    val pullOffsetPx = remember { Animatable(0f) }
    val swingAngleDeg = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Glow filament breathing transition
    val infiniteTransition = rememberInfiniteTransition(label = "filamentWarmth")
    val filamentFlicker by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "filamentFlicker"
    )

    // Ambient glow colors
    val glowColor by animateColorAsState(
        targetValue = if (isCurrentlyLit) Color(0xFFFFB300) else Color(0xFF6366F1),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ambientThemeGlow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleMedium)
            .background(
                Brush.verticalGradient(
                    colors = if (isCurrentlyLit) {
                        listOf(Color(0xFFFFF9EE), Color(0xFFF3EAD8))
                    } else {
                        if (isDark) {
                            listOf(Color(0xFF161622), Color(0xFF0F0F17))
                        } else {
                            listOf(Color(0xFFF0F0F5), Color(0xFFE5E5EB))
                        }
                    }
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = if (isCurrentlyLit) {
                        listOf(Color(0xFFFFDF9E), Color(0x30E5B65A))
                    } else {
                        if (isDark) listOf(Color(0x30FFFFFF), Color(0x10FFFFFF)) else listOf(Color(0x60FFFFFF), Color(0x15000000))
                    }
                ),
                shape = SquircleMedium
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title & System Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(glowColor.copy(alpha = if (isCurrentlyLit) 0.22f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyLit) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = glowColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Appearance Mode",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isCurrentlyLit) "Warm Daylight (Illuminated)" else if (currentTheme == ThemeMode.DARK) "Deep Twilight (Extinguished)" else "Following System Atmosphere",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = colors.textSecondary
                        )
                    }
                }

                // System Default Capsule Button
                val isSystem = currentTheme == ThemeMode.SYSTEM
                Box(
                    modifier = Modifier
                        .clip(SquirclePill)
                        .background(if (isSystem) colors.accentSilent.copy(alpha = 0.16f) else if (isDark) Color(0x18FFFFFF) else Color(0x10000000))
                        .border(
                            0.7.dp,
                            if (isSystem) colors.accentSilent.copy(alpha = 0.45f) else Color.Transparent,
                            SquirclePill
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BrightnessAuto,
                            contentDescription = "System Mode",
                            tint = if (isSystem) colors.accentSilent else colors.textSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "System",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSystem) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.5.sp
                            ),
                            color = if (isSystem) colors.accentSilent else colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Vintage Pull-Lamp Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(SquircleMedium)
                    .background(if (isCurrentlyLit) Color(0x20FFD54F) else if (isDark) Color(0x22000000) else Color(0x0A000000)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Background Light Cone / Radial Illumination
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (isCurrentlyLit) {
                        // Wide ambient flare
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x60FFE082),
                                    Color(0x25FFCA28),
                                    Color(0x00FFB300)
                                ),
                                center = Offset(size.width / 2f, 38.dp.toPx()),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width / 2f, 38.dp.toPx())
                        )
                    }
                }

                // Vintage Edison Lamp & Brass Socket Visualizer (Centered Top)
                VintageEdisonBulbCanvas(
                    isLit = isCurrentlyLit,
                    flickerScale = if (isCurrentlyLit) filamentFlicker else 1.0f,
                    modifier = Modifier
                        .size(width = 84.dp, height = 76.dp)
                        .align(Alignment.TopCenter)
                )

                // Interactive Physics Pull Chain & Brass Acorn Pendant
                val currentPull = pullOffsetPx.value
                val pullProgress = (currentPull / thresholdPx).coerceIn(0f, 1.5f)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset { IntOffset(x = 18.dp.roundToPx(), y = 46.dp.roundToPx()) }
                        .rotate(swingAngleDeg.value)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        val reachedThreshold = pullOffsetPx.value >= thresholdPx
                                        coroutineScope.launch {
                                            if (reachedThreshold) {
                                                // Toggle theme
                                                val nextMode = if (isCurrentlyLit) ThemeMode.DARK else ThemeMode.LIGHT
                                                onThemeSelected(nextMode)
                                                // Trigger slight pendulum swing
                                                swingAngleDeg.animateTo(
                                                    targetValue = 6f,
                                                    animationSpec = tween(70, easing = FastOutSlowInEasing)
                                                )
                                                swingAngleDeg.animateTo(
                                                    targetValue = -4f,
                                                    animationSpec = tween(90, easing = FastOutSlowInEasing)
                                                )
                                                swingAngleDeg.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                )
                                            }
                                            // Spring recoil back to origin
                                            pullOffsetPx.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = 260f
                                                )
                                            )
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        coroutineScope.launch {
                                            pullOffsetPx.animateTo(0f, spring())
                                        }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val cur = pullOffsetPx.value
                                        // Dynamic spring resistance tension
                                        val resistance = 1f / (1f + (cur / maxPullPx) * 1.6f)
                                        val nextVal = (cur + dragAmount * resistance).coerceIn(0f, maxPullPx)
                                        coroutineScope.launch {
                                            pullOffsetPx.snapTo(nextVal)
                                        }
                                    }
                                )
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    // Programmatic realistic snap-pull on tap
                                    coroutineScope.launch {
                                        pullOffsetPx.animateTo(
                                            targetValue = thresholdPx * 1.15f,
                                            animationSpec = tween(120, easing = FastOutSlowInEasing)
                                        )
                                        val nextMode = if (isCurrentlyLit) ThemeMode.DARK else ThemeMode.LIGHT
                                        onThemeSelected(nextMode)
                                        swingAngleDeg.animateTo(4f, tween(60))
                                        swingAngleDeg.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                        pullOffsetPx.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = 240f
                                            )
                                        )
                                    }
                                }
                            )
                    ) {
                        // Beaded Metallic Pull Chain Canvas
                        val chainLengthDp = 30.dp + (currentPull / density.density).dp
                        BrassBeadedChainCanvas(
                            heightDp = chainLengthDp,
                            modifier = Modifier.width(10.dp)
                        )

                        // Brass Acorn / Pendant Finial Grip
                        VintageBrassAcornPendant(
                            isPulled = pullProgress >= 1.0f,
                            isLit = isCurrentlyLit
                        )
                    }
                }

                // Interactive Hint Label (Bottom of the Stage)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(SquirclePill)
                        .background(if (isCurrentlyLit) Color(0x40FFFFFF) else if (isDark) Color(0x33000000) else Color(0x18000000))
                        .border(0.6.dp, if (isCurrentlyLit) Color(0x60FFD54F) else Color(0x15FFFFFF), SquirclePill)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isCurrentlyLit) "PULL CORD TO TURN OFF" else "PULL CORD TO TURN ON",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isCurrentlyLit) Color(0xFFB45309) else colors.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Realistic Edison Lamp Canvas with Brass Socket, Glass Bulb, and Filament.
 */
@Composable
private fun VintageEdisonBulbCanvas(
    isLit: Boolean,
    flickerScale: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f

        // 1. Top Brass Socket Canopy Mount
        val brassGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF8D6E3F),
                Color(0xFFD4AF37),
                Color(0xFFFFDF79),
                Color(0xFFB8860B),
                Color(0xFF5C4018)
            )
        )

        // Mounting Bracket Base
        drawRoundRect(
            brush = brassGradient,
            topLeft = Offset(cx - 16.dp.toPx(), 0f),
            size = Size(32.dp.toPx(), 6.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Socket Collar with Knurled Rings
        drawRect(
            brush = brassGradient,
            topLeft = Offset(cx - 11.dp.toPx(), 6.dp.toPx()),
            size = Size(22.dp.toPx(), 10.dp.toPx())
        )

        // Grommet / Chain Exit Bushing on Right Side
        drawCircle(
            brush = brassGradient,
            radius = 3.dp.toPx(),
            center = Offset(cx + 18.dp.toPx(), 9.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF2B1D0E),
            radius = 1.4.dp.toPx(),
            center = Offset(cx + 18.dp.toPx(), 9.dp.toPx())
        )

        // 2. Glass Teardrop Edison Bulb Envelope
        val bulbTopY = 16.dp.toPx()
        val bulbRadius = 18.dp.toPx()
        val bulbCenter = Offset(cx, bulbTopY + bulbRadius)

        val bulbPath = Path().apply {
            moveTo(cx - 9.dp.toPx(), bulbTopY)
            lineTo(cx + 9.dp.toPx(), bulbTopY)
            // Curved glass body expanding out and rounding at bottom
            cubicTo(
                cx + 22.dp.toPx(), bulbTopY + 8.dp.toPx(),
                cx + 20.dp.toPx(), bulbTopY + 36.dp.toPx(),
                cx, bulbTopY + 44.dp.toPx()
            )
            cubicTo(
                cx - 20.dp.toPx(), bulbTopY + 36.dp.toPx(),
                cx - 22.dp.toPx(), bulbTopY + 8.dp.toPx(),
                cx - 9.dp.toPx(), bulbTopY
            )
            close()
        }

        // Glass Tint Fill
        if (isLit) {
            drawPath(
                path = bulbPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF176).copy(alpha = 0.55f * flickerScale),
                        Color(0xFFFFB300).copy(alpha = 0.35f),
                        Color(0xFFFF8F00).copy(alpha = 0.15f)
                    ),
                    center = bulbCenter,
                    radius = bulbRadius * 1.4f
                )
            )
        } else {
            drawPath(
                path = bulbPath,
                color = Color(0x18FFFFFF)
            )
        }

        // Glass Specular Rim Highlight
        drawPath(
            path = bulbPath,
            color = if (isLit) Color(0x60FFFFFF) else Color(0x35FFFFFF),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Glass Curved Reflection Arc
        drawArc(
            color = Color.White.copy(alpha = if (isLit) 0.5f else 0.25f),
            startAngle = 140f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - 16.dp.toPx(), bulbTopY + 4.dp.toPx()),
            size = Size(32.dp.toPx(), 34.dp.toPx()),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Vintage Squirrel-Cage Filament Wire
        val filamentPath = Path().apply {
            val startY = bulbTopY + 7.dp.toPx()
            moveTo(cx - 3.dp.toPx(), startY)
            lineTo(cx - 4.dp.toPx(), startY + 16.dp.toPx())
            lineTo(cx - 1.dp.toPx(), startY + 8.dp.toPx())
            lineTo(cx + 2.dp.toPx(), startY + 18.dp.toPx())
            lineTo(cx + 4.dp.toPx(), startY + 6.dp.toPx())
            lineTo(cx + 3.dp.toPx(), startY)
        }

        if (isLit) {
            // Intense Glowing Core
            drawPath(
                path = filamentPath,
                color = Color(0xFFFF9100).copy(alpha = 0.9f),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = filamentPath,
                color = Color(0xFFFFFDE7),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else {
            // Cold graphite/tungsten filament wire
            drawPath(
                path = filamentPath,
                color = Color(0xFF6B5848),
                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * High-detail Beaded Metallic Ball Chain Canvas.
 */
@Composable
private fun BrassBeadedChainCanvas(
    heightDp: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.height(heightDp)) {
        val cx = size.width / 2f
        val beadRadius = 2.0.dp.toPx()
        val spacing = 5.2.dp.toPx()
        val totalBeads = (size.height / spacing).toInt().coerceAtLeast(1)

        val beadBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF59D),
                Color(0xFFD4AF37),
                Color(0xFF8D6E3F),
                Color(0xFF422C10)
            ),
            center = Offset(cx - 0.7.dp.toPx(), 0f),
            radius = beadRadius * 1.5f
        )

        // Connecting metal wire
        drawLine(
            color = Color(0xFF8D6E3F),
            start = Offset(cx, 0f),
            end = Offset(cx, size.height),
            strokeWidth = 0.9.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Spherical metallic beads
        for (i in 0..totalBeads) {
            val cy = (i * spacing).coerceAtMost(size.height)
            drawCircle(
                brush = beadBrush,
                radius = beadRadius,
                center = Offset(cx, cy)
            )
            // Tiny white specular glint
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = 0.6.dp.toPx(),
                center = Offset(cx - 0.7.dp.toPx(), cy - 0.7.dp.toPx())
            )
        }
    }
}

/**
 * Vintage Lathed Brass Acorn / Pendant Pull Knob.
 */
@Composable
private fun VintageBrassAcornPendant(
    isPulled: Boolean,
    isLit: Boolean,
    modifier: Modifier = Modifier
) {
    val brassGold = Color(0xFFD4AF37)
    val highlight = if (isLit) Color(0xFFFFE082) else Color(0xFFFFF9C4)

    Box(
        modifier = modifier
            .size(width = 16.dp, height = 24.dp)
            .shadow(elevation = if (isPulled) 4.dp else 2.dp, shape = CircleShape)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f

            val pendantBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF7A5C28),
                    brassGold,
                    highlight,
                    Color(0xFFA67C1E),
                    Color(0xFF3E280C)
                )
            )

            // Top Collar Cap
            drawRoundRect(
                brush = pendantBrush,
                topLeft = Offset(cx - 3.5.dp.toPx(), 0f),
                size = Size(7.dp.toPx(), 3.5.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )

            // Lathed Ring
            drawRect(
                brush = pendantBrush,
                topLeft = Offset(cx - 5.5.dp.toPx(), 3.5.dp.toPx()),
                size = Size(11.dp.toPx(), 2.5.dp.toPx())
            )

            // Acorn / Bell Shaped Body
            val acornPath = Path().apply {
                moveTo(cx - 5.5.dp.toPx(), 6.dp.toPx())
                lineTo(cx + 5.5.dp.toPx(), 6.dp.toPx())
                cubicTo(
                    cx + 7.5.dp.toPx(), 11.dp.toPx(),
                    cx + 5.5.dp.toPx(), 18.dp.toPx(),
                    cx, 22.dp.toPx()
                )
                cubicTo(
                    cx - 5.5.dp.toPx(), 18.dp.toPx(),
                    cx - 7.5.dp.toPx(), 11.dp.toPx(),
                    cx - 5.5.dp.toPx(), 6.dp.toPx()
                )
                close()
            }

            drawPath(
                path = acornPath,
                brush = pendantBrush
            )

            // Bottom Teardrop Finial Tip
            drawCircle(
                brush = pendantBrush,
                radius = 1.6.dp.toPx(),
                center = Offset(cx, 22.5.dp.toPx())
            )

            // Specular Glint Reflection Line
            drawLine(
                color = Color.White.copy(alpha = 0.65f),
                start = Offset(cx - 1.5.dp.toPx(), 7.dp.toPx()),
                end = Offset(cx - 1.dp.toPx(), 18.dp.toPx()),
                strokeWidth = 1.0.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
