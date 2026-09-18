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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleMedium
import com.awayassist.app.ui.theme.SquirclePill
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-end Liquid Glassmorphic Custom Duration & Time Setter.
 * Lets users pick the exact pause duration (e.g., 5m, 10m, 25m, 1h, 2h)
 * with interactive steppers, quick presets, smooth slider, and live resume time calculation.
 */
@Composable
fun LiquidClockPicker(
    initialTargetTimestamp: Long,
    onDurationChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = AwayAssistTheme.colors.isDark
) {
    val colors = AwayAssistTheme.colors

    // Calculate initial duration in minutes (defaulting to 10m)
    val now = System.currentTimeMillis()
    val initialDurationMins = if (initialTargetTimestamp > now + 30_000L) {
        ((initialTargetTimestamp - now) / 60_000L).toInt().coerceIn(1, 1440)
    } else {
        10
    }

    var selectedHours by remember { mutableIntStateOf(initialDurationMins / 60) }
    var selectedMins by remember { mutableIntStateOf(initialDurationMins % 60) }

    fun emitDuration(hours: Int, mins: Int) {
        val totalMins = (hours * 60 + mins).coerceAtLeast(1)
        val durationMs = totalMins * 60_000L
        onDurationChanged(durationMs)
    }

    val totalDurationMins = selectedHours * 60 + selectedMins
    val calculatedResumeTimestamp = now + (totalDurationMins * 60_000L)
    val resumeTimeFormatted = remember(calculatedResumeTimestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(calculatedResumeTimestamp))
    }

    val durationLabel = when {
        selectedHours > 0 && selectedMins > 0 -> "${selectedHours}h ${selectedMins}m"
        selectedHours > 0 -> "${selectedHours}h"
        else -> "${selectedMins}m"
    }

    // Blinking colon transition
    val infiniteTransition = rememberInfiniteTransition(label = "clockColonBlink")
    val colonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colonAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(shape = SquircleMedium, tintColor = colors.warning, isDark = isDark)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Title & Calculated Target Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Custom Pause Duration",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        color = colors.warning
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(SquirclePill)
                        .background(colors.warning.copy(alpha = 0.15f))
                        .border(0.6.dp, colors.warning.copy(alpha = 0.4f), SquirclePill)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "Resumes at $resumeTimeFormatted",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        ),
                        color = colors.warning
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Duration Stepper Row: Hours & Minutes Dials + Visual Clock Face
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Digital Duration Steppers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hours Dial
                    DurationDigitDial(
                        value = "%02d".format(selectedHours),
                        label = "HOURS",
                        onIncrement = {
                            val newH = (selectedHours + 1).coerceAtMost(23)
                            selectedHours = newH
                            emitDuration(newH, selectedMins)
                        },
                        onDecrement = {
                            val newH = (selectedHours - 1).coerceAtLeast(0)
                            selectedHours = newH
                            emitDuration(newH, selectedMins)
                        },
                        isDark = isDark
                    )

                    // Blinking Colon
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = colors.textPrimary.copy(alpha = colonAlpha),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Minutes Dial (Increments / decrements in 5-minute steps)
                    DurationDigitDial(
                        value = "%02d".format(selectedMins),
                        label = "MINS",
                        onIncrement = {
                            var newM = ((selectedMins / 5) * 5) + 5
                            var newH = selectedHours
                            if (newM >= 60) {
                                newM = 0
                                newH = (newH + 1).coerceAtMost(23)
                            }
                            selectedHours = newH
                            selectedMins = newM
                            emitDuration(newH, newM)
                        },
                        onDecrement = {
                            var newM = ((selectedMins / 5) * 5) - 5
                            var newH = selectedHours
                            if (newM < 0) {
                                if (newH > 0) {
                                    newM = 55
                                    newH -= 1
                                } else {
                                    newM = 5 // Minimum 5 mins if 0 hrs
                                }
                            }
                            selectedHours = newH
                            selectedMins = newM
                            emitDuration(newH, newM)
                        },
                        isDark = isDark
                    )
                }

                // Live Analog Target Clock Face
                val targetCal = Calendar.getInstance().apply { timeInMillis = calculatedResumeTimestamp }
                val targetHour = targetCal.get(Calendar.HOUR)
                val targetMin = targetCal.get(Calendar.MINUTE)

                LiveAnalogClockFace(
                    hour = targetHour,
                    minute = targetMin,
                    isDark = isDark,
                    modifier = Modifier.size(62.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Preset Selection Chips (e.g., 5m, 10m, 15m, 25m, 45m, 2h)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val presets = listOf(
                    Pair("5m", 5),
                    Pair("10m", 10),
                    Pair("15m", 15),
                    Pair("25m", 25),
                    Pair("45m", 45),
                    Pair("2h", 120)
                )
                presets.forEach { (label, mins) ->
                    val isCurrent = totalDurationMins == mins
                    val chipBg by animateColorAsState(
                        targetValue = if (isCurrent) {
                            if (isDark) Color(0x55FFFFFF) else Color(0xC8FFFFFF)
                        } else {
                            if (isDark) Color(0x18FFFFFF) else Color(0x10000000)
                        },
                        animationSpec = spring(),
                        label = "presetChipBg"
                    )
                    val chipText by animateColorAsState(
                        targetValue = if (isCurrent) {
                            if (isDark) Color.White else colors.warning
                        } else {
                            colors.textSecondary
                        },
                        animationSpec = spring(),
                        label = "presetChipText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(SquircleMedium)
                            .background(chipBg)
                            .then(
                                if (isCurrent) {
                                    Modifier.border(0.8.dp, colors.warning.copy(alpha = 0.5f), SquircleMedium)
                                } else {
                                    Modifier.border(0.6.dp, if (isDark) Color(0x25FFFFFF) else Color(0x12000000), SquircleMedium)
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    val newH = mins / 60
                                    val newM = mins % 60
                                    selectedHours = newH
                                    selectedMins = newM
                                    emitDuration(newH, newM)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            ),
                            color = chipText
                        )
                    }
                }
            }
        }
    }
}

/**
 * Digit dial with interactive up/down steppers for hours or minutes.
 */
@Composable
private fun DurationDigitDial(
    value: String,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors

    Column(
        modifier = modifier
            .width(62.dp)
            .clip(SquircleMedium)
            .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
            .border(0.8.dp, if (isDark) Color(0x35FFFFFF) else Color(0x18000000), SquircleMedium)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Increment Button
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onIncrement
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increment",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Digit Value
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp
            ),
            color = colors.textPrimary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 8.5.sp,
                letterSpacing = 0.5.sp
            ),
            color = colors.textSecondary
        )

        // Decrement Button
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDecrement
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrement",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Real-time analog clock canvas showing target resume time.
 */
@Composable
private fun LiveAnalogClockFace(
    hour: Int,
    minute: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors
    val accentColor = colors.warning

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isDark) Color(0x20FFFFFF) else Color(0x15000000))
            .border(1.dp, if (isDark) Color(0x35FFFFFF) else Color(0x20000000), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // Hour tick marks
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30.0) - 90.0)
                val isMajor = i % 3 == 0
                val tickLen = if (isMajor) 4.5.dp.toPx() else 2.5.dp.toPx()
                val startX = center.x + (radius - tickLen) * cos(angle).toFloat()
                val startY = center.y + (radius - tickLen) * sin(angle).toFloat()
                val endX = center.x + radius * cos(angle).toFloat()
                val endY = center.y + radius * sin(angle).toFloat()

                drawLine(
                    color = if (isMajor) accentColor.copy(alpha = 0.8f) else colors.textSecondary.copy(alpha = 0.35f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Minute Hand
            val minuteAngle = Math.toRadians((minute * 6.0) - 90.0)
            val minuteHandLen = radius * 0.72f
            val minuteEndX = center.x + minuteHandLen * cos(minuteAngle).toFloat()
            val minuteEndY = center.y + minuteHandLen * sin(minuteAngle).toFloat()

            drawLine(
                color = colors.textPrimary,
                start = center,
                end = Offset(minuteEndX, minuteEndY),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Hour Hand
            val hourFraction = (hour % 12) + (minute / 60.0)
            val hourAngle = Math.toRadians((hourFraction * 30.0) - 90.0)
            val hourHandLen = radius * 0.48f
            val hourEndX = center.x + hourHandLen * cos(hourAngle).toFloat()
            val hourEndY = center.y + hourHandLen * sin(hourAngle).toFloat()

            drawLine(
                color = accentColor,
                start = center,
                end = Offset(hourEndX, hourEndY),
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center jewel
            drawCircle(
                color = accentColor,
                radius = 2.5.dp.toPx(),
                center = center
            )
        }
    }
}
