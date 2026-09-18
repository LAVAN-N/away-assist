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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.data.ThemeMode
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleLarge
import com.awayassist.app.ui.theme.SquircleMedium
import com.awayassist.app.ui.theme.SquirclePill
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Realistic Vintage Pull-Cord Light Switch Card with Natural Lamp Circular Pool of Light.
 *
 * - The card background remains an elegant glassmorphic surface.
 * - When lit, light radiates as a realistic circular lamp glow with soft radial falloff centered at the bulb.
 * - Beaded metal chain on the right side with comfortable spacing.
 * - Hardware gyroscope/accelerometer gravity physics causing natural real-time metal pendulum sway.
 * - 2D catenary rope physics with Bézier curve dynamics and 360° omnidirectional drag.
 * - Only the bottom antique brass acorn tip is interactable for dragging and tap rebound.
 * - Animated radial ripple wave burst originating directly from the bulb center on theme change.
 */
@Composable
fun VintagePullLightToggle(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = AwayAssistTheme.colors.isDark,
    onBulbPositioned: ((Offset) -> Unit)? = null
) {
    val colors = AwayAssistTheme.colors
    val density = LocalDensity.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isCurrentlyLit = !isDark
    val currentIsLit by rememberUpdatedState(isCurrentlyLit)
    val currentOnThemeSelected by rememberUpdatedState(onThemeSelected)

    // Pull Physics Measurements
    val maxPullXPx = with(density) { 85.dp.toPx() }
    val maxPullYPx = with(density) { 80.dp.toPx() }
    val thresholdDistPx = with(density) { 36.dp.toPx() }
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
                val gx = event.values[0]
                val gy = event.values[1]
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
            dampingRatio = 0.52f,
            stiffness = 135f
        ),
        label = "gyroTiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = rawTiltYPx,
        animationSpec = spring(
            dampingRatio = 0.52f,
            stiffness = 135f
        ),
        label = "gyroTiltY"
    )

    // Filament living warmth flicker
    val infiniteTransition = rememberInfiniteTransition(label = "filamentWarmth")
    val filamentFlicker by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "filamentFlicker"
    )

    // Animated Theme Change Radial Burst Wave (starts from bulb center)
    val lightBurstProgress = remember { Animatable(0f) }
    var previousLitState by remember { mutableStateOf(isCurrentlyLit) }

    LaunchedEffect(isCurrentlyLit) {
        if (isCurrentlyLit != previousLitState) {
            previousLitState = isCurrentlyLit
            try {
                lightBurstProgress.snapTo(0f)
                lightBurstProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
                )
            } finally {
                if (lightBurstProgress.value != 1f) {
                    lightBurstProgress.snapTo(1f)
                }
            }
        }
    }

    // Ambient glow colors
    val glowColor by animateColorAsState(
        targetValue = if (isCurrentlyLit) Color(0xFFFFB300) else Color(0xFF6366F1),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ambientThemeGlow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = SquircleLarge,
                tintColor = if (isCurrentlyLit) Color(0xFFFFB300) else null,
                isDark = isDark
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

            // Interactive Stage with Frosted Gradient Chamber
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(SquircleMedium)
                    .background(
                        if (isDark) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x22FFFFFF),
                                    Color(0x0CFFFFFF)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x30FFFFFF),
                                    Color(0x10FFFFFF)
                                )
                            )
                        }
                    )
                    .border(
                        width = 0.8.dp,
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color(0x30FFFFFF), Color(0x0CFFFFFF))
                            } else {
                                listOf(Color(0x80FFFFFF), Color(0x25FFFFFF))
                            }
                        ),
                        shape = SquircleMedium
                    )
                    .onGloballyPositioned { coordinates ->
                        val rootPos = coordinates.positionInRoot()
                        val bulbCx = coordinates.size.width / 2f - with(density) { 26.dp.toPx() }
                        val bulbCy = with(density) { 38.dp.toPx() }
                        onBulbPositioned?.invoke(Offset(rootPos.x + bulbCx, rootPos.y + bulbCy))
                    }
            ) {
                val stageWidthPx = with(density) { maxWidth.toPx() }
                val stageHeightPx = with(density) { maxHeight.toPx() }

                // Bulb position (Left-center of stage)
                val bulbCx = stageWidthPx / 2f - with(density) { 26.dp.toPx() }
                val bulbCenterY = with(density) { 38.dp.toPx() }
                val bulbCenter = Offset(bulbCx, bulbCenterY)

                // String mount position (Right side with extra gap)
                val anchorXPx = stageWidthPx / 2f + with(density) { 36.dp.toPx() }
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

                // 1. Realistic Circular Lamp Glow & Theme Transition Wave
                Canvas(modifier = Modifier.matchParentSize()) {
                    // Realistic Lamp Circular Illumination Glow (Concentrated Radial Falloff)
                    if (isCurrentlyLit) {
                        val flicker = filamentFlicker

                        // Layer A: Broad Soft Circular Ambient Light Cast on the Wall
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x38FFD54F).copy(alpha = 0.35f * flicker),
                                    Color(0x22FFB300).copy(alpha = 0.22f * flicker),
                                    Color(0x0CFF8F00).copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = bulbCenter,
                                radius = 120.dp.toPx()
                            ),
                            center = bulbCenter,
                            radius = 120.dp.toPx()
                        )

                        // Layer B: Focused Warm Circle of Light (Lamp Beam Pool)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x75FFE082).copy(alpha = 0.70f * flicker),
                                    Color(0x45FFB300).copy(alpha = 0.45f * flicker),
                                    Color(0x18FF8F00).copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                center = bulbCenter,
                                radius = 72.dp.toPx()
                            ),
                            center = bulbCenter,
                            radius = 72.dp.toPx()
                        )

                        // Layer C: Intense Hot Core Bloom around Filament
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xCCFFF9C4).copy(alpha = 0.85f * flicker),
                                    Color(0x80FFD54F).copy(alpha = 0.60f * flicker),
                                    Color(0x00FFB300)
                                ),
                                center = bulbCenter,
                                radius = 32.dp.toPx()
                            ),
                            center = bulbCenter,
                            radius = 32.dp.toPx()
                        )
                    }

                    // Theme Change Shockwave Pulse
                    val progress = lightBurstProgress.value
                    if (progress > 0f && progress < 1f) {
                        val maxRadius = hypot(stageWidthPx, stageHeightPx) * 1.1f
                        val currentRadius = maxRadius * progress
                        val alpha = (1f - progress).coerceIn(0f, 1f)

                        if (isCurrentlyLit) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x80FFE082).copy(alpha = 0.70f * alpha),
                                        Color(0x40FFB300).copy(alpha = 0.40f * alpha),
                                        Color.Transparent
                                    ),
                                    center = bulbCenter,
                                    radius = currentRadius.coerceAtLeast(10f)
                                ),
                                center = bulbCenter,
                                radius = currentRadius
                            )
                        } else {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x656366F1).copy(alpha = 0.50f * alpha),
                                        Color(0x254F46E5).copy(alpha = 0.25f * alpha),
                                        Color.Transparent
                                    ),
                                    center = bulbCenter,
                                    radius = currentRadius.coerceAtLeast(10f)
                                ),
                                center = bulbCenter,
                                radius = currentRadius
                            )
                        }
                    }
                }

                // 2. Coordinated Visual Canvas: Lamp + Separate Cord Mount on Right + Gyro Catenary Rope
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw Edison Bulb (Completely Independent, No Horizontal Pipe)
                    drawVintageEdisonBulb(
                        bulbCx = bulbCx,
                        isLit = isCurrentlyLit,
                        flickerScale = if (isCurrentlyLit) filamentFlicker else 1.0f
                    )

                    // Draw Separate Top Ceiling Brass Rosette Mount for the Cord on Right
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
                    val totalBeads = (straightDistance / beadSpacing).toInt().coerceAtLeast(6)
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

                var pullJob by remember { mutableStateOf<Job?>(null) }

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
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    pullJob?.cancel()
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val dist = hypot(offsetX.value, offsetY.value)
                                    val isTriggered = dist >= thresholdDistPx && (offsetY.value > 10f || abs(offsetX.value) > 18f)

                                    pullJob?.cancel()
                                    pullJob = coroutineScope.launch {
                                        try {
                                            if (isTriggered) {
                                                val nextMode = if (currentIsLit) ThemeMode.DARK else ThemeMode.LIGHT
                                                currentOnThemeSelected(nextMode)
                                            }

                                            // Soft, weighted beaded cord recoil physics: smooth ease-out and gentle natural settle
                                            launch {
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.72f,
                                                        stiffness = 50f
                                                    )
                                                )
                                            }
                                            launch {
                                                offsetY.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.76f,
                                                        stiffness = 55f
                                                    )
                                                )
                                            }
                                        } finally {
                                            if (!isDragging) {
                                                if (offsetX.value != 0f) offsetX.snapTo(0f)
                                                if (offsetY.value != 0f) offsetY.snapTo(0f)
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    pullJob?.cancel()
                                    pullJob = coroutineScope.launch {
                                        try {
                                            launch {
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 50f)
                                                )
                                            }
                                            launch {
                                                offsetY.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(dampingRatio = 0.76f, stiffness = 55f)
                                                )
                                            }
                                        } finally {
                                            if (!isDragging) {
                                                if (offsetX.value != 0f) offsetX.snapTo(0f)
                                                if (offsetY.value != 0f) offsetY.snapTo(0f)
                                            }
                                        }
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
                                pullJob?.cancel()
                                pullJob = coroutineScope.launch {
                                    try {
                                        val nextMode = if (currentIsLit) ThemeMode.DARK else ThemeMode.LIGHT
                                        currentOnThemeSelected(nextMode)
                                        launch {
                                            offsetX.snapTo(0f)
                                            offsetX.animateTo(10f, tween(160, easing = FastOutSlowInEasing))
                                            offsetX.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 50f))
                                        }
                                        launch {
                                            offsetY.snapTo(0f)
                                            offsetY.animateTo(thresholdDistPx * 1.1f, tween(160, easing = FastOutSlowInEasing))
                                            offsetY.animateTo(0f, spring(dampingRatio = 0.76f, stiffness = 55f))
                                        }
                                    } finally {
                                        if (!isDragging) {
                                            if (offsetX.value != 0f) offsetX.snapTo(0f)
                                            if (offsetY.value != 0f) offsetY.snapTo(0f)
                                        }
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
                        .background(
                            if (isCurrentlyLit) {
                                if (isDark) Color(0x35FFE082) else Color(0x24FFB300)
                            } else {
                                if (isDark) Color(0x22FFFFFF) else Color(0x14000000)
                            }
                        )
                        .border(
                            0.6.dp,
                            if (isCurrentlyLit) Color(0x50FFD54F) else if (isDark) Color(0x25FFFFFF) else Color(0x18000000),
                            SquirclePill
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isCurrentlyLit) "PULL BRASS TIP TO TURN OFF" else "PULL BRASS TIP TO TURN ON",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isCurrentlyLit) {
                            if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
                        } else {
                            colors.textSecondary
                        }
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
                    Color(0xFFFFF176).copy(alpha = 0.65f * flickerScale),
                    Color(0xFFFFB300).copy(alpha = 0.40f),
                    Color(0xFFFF8F00).copy(alpha = 0.18f)
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
        color = if (isLit) Color(0x75FFFFFF) else Color(0x35FFFFFF),
        style = Stroke(width = 1.2.dp.toPx())
    )

    // Glass Curved Reflection Arc
    drawArc(
        color = Color.White.copy(alpha = if (isLit) 0.6f else 0.25f),
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
            color = Color(0xFFFF9100).copy(alpha = 0.95f),
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
