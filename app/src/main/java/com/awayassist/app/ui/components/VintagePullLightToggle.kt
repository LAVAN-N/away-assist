package com.awayassist.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.platform.LocalContext
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
 * Realistic Vintage Pull-Cord Light Switch with Gyroscope/Accelerometer Gravity Sway.
 *
 * - Real-time device tilt & gravity dynamics: tilting the phone makes the beaded cord sway
 *   naturally like a real hanging metal chain.
 * - String hangs independently beside the bulb from its own ceiling rosette (no attachment to bulb).
 * - Catenary rope curvature & damped 2D harmonic spring physics.
 * - Only the bottom acorn tip is interactable for grabbing, dragging, and pulling in any direction.
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isCurrentlyLit = !isDark
    val currentIsLit by rememberUpdatedState(isCurrentlyLit)
    val currentOnThemeSelected by rememberUpdatedState(onThemeSelected)

    // Physical measurements in px
    val maxPullXPx = with(density) { 80.dp.toPx() }
    val maxPullYPx = with(density) { 75.dp.toPx() }
    val thresholdDistPx = with(density) { 34.dp.toPx() }
    val restLengthPx = with(density) { 72.dp.toPx() }

    // 2D Rope Physics Animatable Drag Offsets
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Gyroscope / Accelerometer Gravity Tilt State
    var rawTiltXPx by remember { mutableFloatStateOf(0f) }
    var rawTiltYPx by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val maxSwayXPx = with(density) { 48.dp.toPx() }
        val maxSwayYPx = with(density) { 16.dp.toPx() }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                // event.values[0]: X axis gravity (-9.8 to +9.8 m/s²)
                // event.values[1]: Y axis gravity
                val gx = event.values[0]
                val gy = event.values[1]
                // Boosted sensitivity: subtle 15°-30° tilt creates expressive real-time sway
                val normalizedTiltX = (-gx / 4.2f).coerceIn(-1.0f, 1.0f)
                val normalizedTiltY = ((9.81f - gy) / 5.5f).coerceIn(-0.4f, 0.8f)
                rawTiltXPx = normalizedTiltX * maxSwayXPx
                rawTiltYPx = normalizedTiltY * maxSwayYPx
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensor?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Reactive Spring-damped Gyro Pendulum Response
    val animatedTiltX by animateFloatAsState(
        targetValue = rawTiltXPx,
        animationSpec = spring(
            dampingRatio = 0.52f, // Bouncy natural metal pendulum sway
            stiffness = 140f
        ),
        label = "gyroTiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = rawTiltYPx,
        animationSpec = spring(
            dampingRatio = 0.52f,
            stiffness = 140f
        ),
        label = "gyroTiltY"
    )

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

            // Interactive Stage with Independent Lamp, Gyro Pendulum Physics, and Catenary Rope
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(SquircleMedium)
                    .background(if (isCurrentlyLit) Color(0x20FFD54F) else if (isDark) Color(0x22000000) else Color(0x0A000000))
            ) {
                val stageWidthPx = with(density) { maxWidth.toPx() }
                val bulbCx = stageWidthPx / 2f - with(density) { 24.dp.toPx() }
                val anchorXPx = stageWidthPx / 2f + with(density) { 26.dp.toPx() }
                val anchorYPx = with(density) { 6.dp.toPx() }

                val curX = offsetX.value
                val curY = offsetY.value

                // Combine Gyro Tilt with Interactive Drag
                val liveTiltX = if (isDragging) animatedTiltX * 0.25f else animatedTiltX
                val liveTiltY = if (isDragging) animatedTiltY * 0.25f else animatedTiltY

                // Acorn Tip Position
                val acornPoint = Offset(
                    x = anchorXPx + liveTiltX + curX,
                    y = anchorYPx + restLengthPx + liveTiltY + curY
                )

                // 1. Background Ambient Radial Glow
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (isCurrentlyLit) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x60FFE082),
                                    Color(0x25FFCA28),
                                    Color(0x00FFB300)
                                ),
                                center = Offset(bulbCx, 42.dp.toPx()),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(bulbCx, 42.dp.toPx())
                        )
                    }
                }

                // 2. Coordinated Visual Canvas: Lamp + Separate Cord Mount + Gyro Catenary Rope
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw Edison Bulb (Completely Independent, No Horizontal Pipe)
                    drawVintageEdisonBulb(
                        bulbCx = bulbCx,
                        isLit = isCurrentlyLit,
                        flickerScale = if (isCurrentlyLit) filamentFlicker else 1.0f
                    )

                    // Draw Separate Top Ceiling Brass Rosette Mount for the Cord
                    drawCordCeilingMount(
                        anchor = Offset(anchorXPx, anchorYPx)
                    )

                    // Catenary Rope Physics Computation
                    val anchorPoint = Offset(anchorXPx, anchorYPx)
                    val straightDistance = hypot(acornPoint.x - anchorPoint.x, acornPoint.y - anchorPoint.y)
                    val tautRatio = (straightDistance / restLengthPx).coerceIn(0.5f, 2.0f)

                    val midX = (anchorPoint.x + acornPoint.x) / 2f
                    val midY = (anchorPoint.y + acornPoint.y) / 2f

                    val gravitySag = if (tautRatio < 1.0f) {
                        (1.0f - tautRatio) * 14.dp.toPx() + 2.dp.toPx()
                    } else {
                        2.dp.toPx() / tautRatio
                    }

                    val lateralSag = if (abs(curX + liveTiltX) > 6f) {
                        -(curX + liveTiltX) * 0.12f
                    } else 0f

                    val controlPoint = Offset(
                        x = midX + lateralSag,
                        y = midY + gravitySag
                    )

                    // Draw Flexible Beaded Chain along Bézier Curve
                    val beadSpacing = 5.0.dp.toPx()
                    val totalBeads = (straightDistance / beadSpacing).toInt().coerceAtLeast(4)
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

                    val ropePath = Path().apply {
                        moveTo(anchorPoint.x, anchorPoint.y)
                        quadraticTo(controlPoint.x, controlPoint.y, acornPoint.x, acornPoint.y)
                    }

                    drawPath(
                        path = ropePath,
                        color = Color(0xFF8D6E3F),
                        style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Brass Beads along Catenary Curve
                    for (i in 0..totalBeads) {
                        val t = i.toFloat() / totalBeads.toFloat()
                        val u = 1f - t
                        val bx = u * u * anchorPoint.x + 2f * u * t * controlPoint.x + t * t * acornPoint.x
                        val by = u * u * anchorPoint.y + 2f * u * t * controlPoint.y + t * t * acornPoint.y

                        drawCircle(
                            brush = beadBrush,
                            radius = beadRadius,
                            center = Offset(bx, by)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.75f),
                            radius = 0.6.dp.toPx(),
                            center = Offset(bx - 0.7.dp.toPx(), by - 0.7.dp.toPx())
                        )
                    }

                    // Tangent angle at the acorn tip
                    val tangentX = 2f * (acornPoint.x - controlPoint.x)
                    val tangentY = 2f * (acornPoint.y - controlPoint.y)
                    val acornAngleRad = atan2(tangentX, tangentY)
                    val acornAngleDeg = (acornAngleRad * 180.0 / Math.PI).toFloat()

                    // Draw Antique Brass Acorn Pendant Handle at Tip
                    drawBrassAcornPendant(
                        center = acornPoint,
                        angleDegrees = -acornAngleDeg,
                        isLit = isCurrentlyLit
                    )
                }

                // 3. ONLY THE TIP IS INTERACTABLE (Acorn Handle Hit Target)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (acornPoint.x - with(density) { 26.dp.toPx() }).roundToInt(),
                                y = (acornPoint.y - with(density) { 26.dp.toPx() }).roundToInt()
                            )
                        }
                        .size(52.dp)
                        .clip(CircleShape)
                        .pointerInput(currentIsLit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val dist = hypot(offsetX.value, offsetY.value)
                                    val isTriggered = dist >= thresholdDistPx && (offsetY.value > 10f || abs(offsetX.value) > 20f)

                                    coroutineScope.launch {
                                        if (isTriggered) {
                                            val nextMode = if (currentIsLit) ThemeMode.DARK else ThemeMode.LIGHT
                                            currentOnThemeSelected(nextMode)
                                        }

                                        // 2D Spring physics recoil with damped harmonic pendulum wave
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
                                                    stiffness = 250f
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
                                    val damping = 1f / (1f + (curDist / maxPullYPx) * 1.4f)

                                    val nextX = (offsetX.value + dragAmount.x * damping).coerceIn(-maxPullXPx, maxPullXPx)
                                    val nextY = (offsetY.value + dragAmount.y * damping).coerceIn(-22f, maxPullYPx)

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
                                // Tap on the tip triggers instant pull-snap & spring rebound
                                coroutineScope.launch {
                                    launch {
                                        offsetX.animateTo(12f, tween(110, easing = FastOutSlowInEasing))
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                    }
                                    launch {
                                        offsetY.animateTo(thresholdDistPx * 1.2f, tween(110, easing = FastOutSlowInEasing))
                                        val nextMode = if (currentIsLit) ThemeMode.DARK else ThemeMode.LIGHT
                                        currentOnThemeSelected(nextMode)
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 250f))
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
                        text = if (isCurrentlyLit) "PULL BRASS TIP TO TURN OFF" else "PULL BRASS TIP TO TURN ON",
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
 * Draw Independent Top Brass Ceiling Rosette / Eyelet Mount for the Cord.
 */
private fun DrawScope.drawCordCeilingMount(anchor: Offset) {
    val brassGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF8D6E3F),
            Color(0xFFD4AF37),
            Color(0xFFFFDF79),
            Color(0xFFB8860B),
            Color(0xFF5C4018)
        ),
        startX = anchor.x - 8.dp.toPx(),
        endX = anchor.x + 8.dp.toPx()
    )

    // Ceiling Bracket Rosette
    drawRoundRect(
        brush = brassGradient,
        topLeft = Offset(anchor.x - 8.dp.toPx(), 0f),
        size = Size(16.dp.toPx(), 4.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )

    // Eyelet Bushing
    drawCircle(
        brush = brassGradient,
        radius = 3.2.dp.toPx(),
        center = anchor
    )
    drawCircle(
        color = Color(0xFF2B1D0E),
        radius = 1.4.dp.toPx(),
        center = anchor
    )
}

/**
 * Draw Vintage Edison Lamp (Completely Independent, No Outrigger Pipe).
 */
private fun DrawScope.drawVintageEdisonBulb(
    bulbCx: Float,
    isLit: Boolean,
    flickerScale: Float
) {
    val brassGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF8D6E3F),
            Color(0xFFD4AF37),
            Color(0xFFFFDF79),
            Color(0xFFB8860B),
            Color(0xFF5C4018)
        )
    )

    // 1. Top Brass Socket Mounting Bracket
    drawRoundRect(
        brush = brassGradient,
        topLeft = Offset(bulbCx - 16.dp.toPx(), 0f),
        size = Size(32.dp.toPx(), 6.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Socket Collar with Knurled Detailing
    drawRect(
        brush = brassGradient,
        topLeft = Offset(bulbCx - 11.dp.toPx(), 6.dp.toPx()),
        size = Size(22.dp.toPx(), 10.dp.toPx())
    )

    // 2. Glass Teardrop Edison Bulb Envelope
    val bulbTopY = 16.dp.toPx()
    val bulbRadius = 18.dp.toPx()
    val bulbCenter = Offset(bulbCx, bulbTopY + bulbRadius)

    val bulbPath = Path().apply {
        moveTo(bulbCx - 9.dp.toPx(), bulbTopY)
        lineTo(bulbCx + 9.dp.toPx(), bulbTopY)
        cubicTo(
            bulbCx + 22.dp.toPx(), bulbTopY + 8.dp.toPx(),
            bulbCx + 20.dp.toPx(), bulbTopY + 36.dp.toPx(),
            bulbCx, bulbTopY + 44.dp.toPx()
        )
        cubicTo(
            bulbCx - 20.dp.toPx(), bulbTopY + 36.dp.toPx(),
            bulbCx - 22.dp.toPx(), bulbTopY + 8.dp.toPx(),
            bulbCx - 9.dp.toPx(), bulbTopY
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
        topLeft = Offset(bulbCx - 16.dp.toPx(), bulbTopY + 4.dp.toPx()),
        size = Size(32.dp.toPx(), 34.dp.toPx()),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
    )

    // 3. Vintage Squirrel-Cage Filament Wire
    val filamentPath = Path().apply {
        val startY = bulbTopY + 7.dp.toPx()
        moveTo(bulbCx - 3.dp.toPx(), startY)
        lineTo(bulbCx - 4.dp.toPx(), startY + 16.dp.toPx())
        lineTo(bulbCx - 1.dp.toPx(), startY + 8.dp.toPx())
        lineTo(bulbCx + 2.dp.toPx(), startY + 18.dp.toPx())
        lineTo(bulbCx + 4.dp.toPx(), startY + 6.dp.toPx())
        lineTo(bulbCx + 3.dp.toPx(), startY)
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
 * Draw Lathed Antique Brass Acorn Pendant Handle.
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
