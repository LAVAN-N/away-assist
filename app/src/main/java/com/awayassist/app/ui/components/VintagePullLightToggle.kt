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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Realistic Vintage Pull-Chain Light Switch Toggle with Omnidirectional 2D Physics.
 *
 * The chain is anchored directly to the top brass fixture of the Edison bulb.
 * Supports natural pulling in any direction (down, left, right, diagonally) with
 * non-linear 2D spring tension, dynamic angle alignment, and harmonic pendulum recoil.
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
    val maxPullXPx = with(density) { 75.dp.toPx() }
    val maxPullYPx = with(density) { 70.dp.toPx() }
    val thresholdDistPx = with(density) { 36.dp.toPx() }
    val restLengthPx = with(density) { 68.dp.toPx() }

    // 2D Omnidirectional spring physics
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Filament breathing transition
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

            // Interactive Vintage Pull-Lamp Stage (Omnidirectional 2D Physics)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(162.dp)
                    .clip(SquircleMedium)
                    .background(if (isCurrentlyLit) Color(0x20FFD54F) else if (isDark) Color(0x22000000) else Color(0x0A000000))
            ) {
                // Background Light Glow Cone
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (isCurrentlyLit) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x60FFE082),
                                    Color(0x25FFCA28),
                                    Color(0x00FFB300)
                                ),
                                center = Offset(size.width / 2f, 40.dp.toPx()),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width / 2f, 40.dp.toPx())
                        )
                    }
                }

                // Full Coordinated Canvas: Bulb, Filament, Top-Anchored Omnidirectional Beaded Chain, Acorn Finial
                val curX = offsetX.value
                val curY = offsetY.value

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f

                    // 1. Draw Vintage Edison Bulb with Top Brass Socket Canopy
                    drawVintageEdisonBulb(
                        cx = cx,
                        isLit = isCurrentlyLit,
                        flickerScale = if (isCurrentlyLit) filamentFlicker else 1.0f
                    )

                    // 2. Chain Anchor at the TOP OF THE BULB FIXTURE
                    val anchorPoint = Offset(cx + 14.dp.toPx(), 9.dp.toPx())

                    // 3. Current Acorn Finial Position
                    val acornPoint = Offset(
                        x = anchorPoint.x + curX,
                        y = anchorPoint.y + restLengthPx + curY
                    )

                    // 4. Vector from Anchor to Acorn
                    val deltaX = acornPoint.x - anchorPoint.x
                    val deltaY = acornPoint.y - anchorPoint.y
                    val chainDistance = hypot(deltaX, deltaY)

                    // Pull Angle in Radians (0 = straight down)
                    val pullAngleRad = atan2(deltaX, deltaY)
                    val pullAngleDeg = (pullAngleRad * 180.0 / Math.PI).toFloat()

                    // 5. Draw Beaded Metallic Brass Chain along the 2D Vector
                    val beadSpacing = 5.2.dp.toPx()
                    val totalBeads = (chainDistance / beadSpacing).toInt().coerceAtLeast(2)
                    val beadRadius = 2.0.dp.toPx()

                    val beadBrush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF59D),
                            Color(0xFFD4AF37),
                            Color(0xFF8D6E3F),
                            Color(0xFF422C10)
                        ),
                        center = Offset(-0.6.dp.toPx(), -0.6.dp.toPx()),
                        radius = beadRadius * 1.5f
                    )

                    // Underlying chain link wire
                    drawLine(
                        color = Color(0xFF8D6E3F),
                        start = anchorPoint,
                        end = acornPoint,
                        strokeWidth = 0.9.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Spherical Brass Beads along Vector
                    for (i in 0..totalBeads) {
                        val fraction = i.toFloat() / totalBeads.toFloat()
                        val bx = anchorPoint.x + deltaX * fraction
                        val by = anchorPoint.y + deltaY * fraction

                        drawCircle(
                            brush = beadBrush,
                            radius = beadRadius,
                            center = Offset(bx, by)
                        )
                        // Specular highlight glint
                        drawCircle(
                            color = Color.White.copy(alpha = 0.75f),
                            radius = 0.6.dp.toPx(),
                            center = Offset(bx - 0.7.dp.toPx(), by - 0.7.dp.toPx())
                        )
                    }

                    // 6. Draw Antique Brass Acorn Pendant rotated along Pull Angle
                    drawBrassAcornPendant(
                        center = acornPoint,
                        angleDegrees = -pullAngleDeg,
                        isLit = isCurrentlyLit
                    )
                }

                // Interactive 2D Drag & Tap Hit Target overlay on the Acorn
                val anchorXDp = with(density) { 14.dp }
                val anchorYDp = with(density) { 9.dp }
                val restLengthDp = with(density) { restLengthPx.toDp() }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset {
                            IntOffset(
                                x = (anchorXDp.roundToPx() + curX).roundToInt() - 24.dp.roundToPx(),
                                y = (anchorYDp.roundToPx() + restLengthDp.roundToPx() + curY).roundToInt() - 24.dp.roundToPx()
                            )
                        }
                        .size(52.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val dist = hypot(offsetX.value, offsetY.value)
                                    // Trigger if pulled far enough with downward or lateral displacement
                                    val isTriggered = dist >= thresholdDistPx && (offsetY.value > 12f || abs(offsetX.value) > 22f)

                                    coroutineScope.launch {
                                        if (isTriggered) {
                                            val nextMode = if (isCurrentlyLit) ThemeMode.DARK else ThemeMode.LIGHT
                                            onThemeSelected(nextMode)
                                        }

                                        // 2D Harmonic spring recoil & pendulum swing back
                                        launch {
                                            offsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                        launch {
                                            offsetY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = 240f
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    coroutineScope.launch {
                                        launch { offsetX.animateTo(0f, spring()) }
                                        launch { offsetY.animateTo(0f, spring()) }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val curDist = hypot(offsetX.value, offsetY.value)
                                    val damping = 1f / (1f + (curDist / maxPullYPx) * 1.5f)

                                    val nextX = (offsetX.value + dragAmount.x * damping).coerceIn(-maxPullXPx, maxPullXPx)
                                    val nextY = (offsetY.value + dragAmount.y * damping).coerceIn(-20f, maxPullYPx)

                                    coroutineScope.launch {
                                        offsetX.snapTo(nextX)
                                        offsetY.snapTo(nextY)
                                    }
                                }
                            )
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                // Tap triggers a natural 2D pull-snap and pendulum oscillation
                                coroutineScope.launch {
                                    launch {
                                        offsetX.animateTo(14f, tween(110, easing = FastOutSlowInEasing))
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                    }
                                    launch {
                                        offsetY.animateTo(thresholdDistPx * 1.18f, tween(110, easing = FastOutSlowInEasing))
                                        val nextMode = if (isCurrentlyLit) ThemeMode.DARK else ThemeMode.LIGHT
                                        onThemeSelected(nextMode)
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 240f))
                                    }
                                }
                            }
                        )
                )

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
                        text = if (isCurrentlyLit) "PULL STRING IN ANY DIRECTION TO TURN OFF" else "PULL STRING IN ANY DIRECTION TO TURN ON",
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
 * Draw Vintage Edison Lamp with Top Brass Socket Canopy and Filament.
 */
private fun DrawScope.drawVintageEdisonBulb(
    cx: Float,
    isLit: Boolean,
    flickerScale: Float
) {
    // 1. Top Brass Socket Canopy Mount (Origin of String)
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

    // Brass Grommet / Eyelet on top right where string is anchored
    drawCircle(
        brush = brassGradient,
        radius = 3.2.dp.toPx(),
        center = Offset(cx + 14.dp.toPx(), 9.dp.toPx())
    )
    drawCircle(
        color = Color(0xFF2B1D0E),
        radius = 1.4.dp.toPx(),
        center = Offset(cx + 14.dp.toPx(), 9.dp.toPx())
    )

    // 2. Glass Teardrop Edison Bulb Envelope
    val bulbTopY = 16.dp.toPx()
    val bulbRadius = 18.dp.toPx()
    val bulbCenter = Offset(cx, bulbTopY + bulbRadius)

    val bulbPath = Path().apply {
        moveTo(cx - 9.dp.toPx(), bulbTopY)
        lineTo(cx + 9.dp.toPx(), bulbTopY)
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

    // Glass Specular Rim
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
        drawPath(
            path = filamentPath,
            color = Color(0xFF6B5848),
            style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Draw Lathed Antique Brass Acorn Pendant rotated along Pull Vector.
 */
private fun DrawScope.drawBrassAcornPendant(
    center: Offset,
    angleDegrees: Float,
    isLit: Boolean
) {
    rotate(degrees = angleDegrees, pivot = center) {
        val brassGold = Color(0xFFD4AF37)
        val highlight = if (isLit) Color(0xFFFFE082) else Color(0xFFFFF9C4)

        val pendantBrush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF7A5C28),
                brassGold,
                highlight,
                Color(0xFFA67C1E),
                Color(0xFF3E280C)
            ),
            startX = center.x - 8.dp.toPx(),
            endX = center.x + 8.dp.toPx()
        )

        val px = center.x
        val py = center.y

        // Top Collar Cap
        drawRoundRect(
            brush = pendantBrush,
            topLeft = Offset(px - 3.5.dp.toPx(), py - 12.dp.toPx()),
            size = Size(7.dp.toPx(), 3.5.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // Lathed Ridge Ring
        drawRect(
            brush = pendantBrush,
            topLeft = Offset(px - 5.5.dp.toPx(), py - 8.5.dp.toPx()),
            size = Size(11.dp.toPx(), 2.5.dp.toPx())
        )

        // Acorn / Bell Shaped Body
        val acornPath = Path().apply {
            moveTo(px - 5.5.dp.toPx(), py - 6.dp.toPx())
            lineTo(px + 5.5.dp.toPx(), py - 6.dp.toPx())
            cubicTo(
                px + 7.5.dp.toPx(), py - 1.dp.toPx(),
                px + 5.5.dp.toPx(), py + 6.dp.toPx(),
                px, py + 10.dp.toPx()
            )
            cubicTo(
                px - 5.5.dp.toPx(), py + 6.dp.toPx(),
                px - 7.5.dp.toPx(), py - 1.dp.toPx(),
                px - 5.5.dp.toPx(), py - 6.dp.toPx()
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
            center = Offset(px, py + 10.5.dp.toPx())
        )

        // Specular Glint Reflection Line
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(px - 1.5.dp.toPx(), py - 5.dp.toPx()),
            end = Offset(px - 1.dp.toPx(), py + 6.dp.toPx()),
            strokeWidth = 1.0.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
