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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-end Liquid Glassmorphic Real-Time Clock Time Picker.
 * Lets users pick the exact resume time with interactive digital dials,
 * quick duration increments, and a live synchronized analog clock badge.
 */
@Composable
fun LiquidClockPicker(
    initialTargetTimestamp: Long,
    onDurationChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = AwayAssistTheme.colors.isDark
) {
    val colors = AwayAssistTheme.colors

    // Initialize calendar to current pause target or (now + 1 hour)
    val cal = remember(initialTargetTimestamp) {
        Calendar.getInstance().apply {
            val target = if (initialTargetTimestamp > System.currentTimeMillis()) {
                initialTargetTimestamp
            } else {
                System.currentTimeMillis() + 3600_000L
            }
            timeInMillis = target
        }
    }

    var hour24 by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }

    fun computeAndEmitDuration(newHour24: Int, newMinute: Int) {
        val now = System.currentTimeMillis()
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, newHour24)
            set(Calendar.MINUTE, newMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val durationMs = (targetCal.timeInMillis - now).coerceAtLeast(300_000L) // at least 5 mins
        onDurationChanged(durationMs)
    }

    val displayHour = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val isPm = hour24 >= 12

    val now = System.currentTimeMillis()
    val targetCal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, hour24)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val remainingMs = (targetCal.timeInMillis - now).coerceAtLeast(0L)
    val remainingHours = remainingMs / 3600_000L
    val remainingMins = (remainingMs % 3600_000L) / 60_000L

    val durationText = when {
        remainingHours > 0 && remainingMins > 0 -> "${remainingHours}h ${remainingMins}m from now"
        remainingHours > 0 -> "${remainingHours}h from now"
        else -> "${remainingMins}m from now"
    }

    // Blinking separator transition
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
            // Header Row: Clock Title & Real-Time Offset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resume Clock",
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
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        ),
                        color = colors.warning
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Clock Row: Interactive Digital Dials & Live Analog Watch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Digital Interactive Time Dials
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Hour Dial
                    ClockDigitDial(
                        value = "%02d".format(displayHour),
                        label = "HOUR",
                        onIncrement = {
                            hour24 = (hour24 + 1) % 24
                            computeAndEmitDuration(hour24, minute)
                        },
                        onDecrement = {
                            hour24 = (hour24 - 1 + 24) % 24
                            computeAndEmitDuration(hour24, minute)
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
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Minute Dial
                    ClockDigitDial(
                        value = "%02d".format(minute),
                        label = "MIN",
                        onIncrement = {
                            minute = ((minute / 5) * 5 + 5) % 60
                            computeAndEmitDuration(hour24, minute)
                        },
                        onDecrement = {
                            minute = ((minute / 5) * 5 - 5 + 60) % 60
                            computeAndEmitDuration(hour24, minute)
                        },
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // AM / PM Segmented Toggle
                    Column(
                        modifier = Modifier
                            .height(68.dp)
                            .clip(SquircleMedium)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x12000000))
                            .border(0.8.dp, if (isDark) Color(0x35FFFFFF) else Color(0x18000000), SquircleMedium)
                            .padding(2.5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AmPmButton(
                            text = "AM",
                            isSelected = !isPm,
                            onClick = {
                                if (isPm) {
                                    hour24 -= 12
                                    computeAndEmitDuration(hour24, minute)
                                }
                            },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        AmPmButton(
                            text = "PM",
                            isSelected = isPm,
                            onClick = {
                                if (!isPm) {
                                    hour24 += 12
                                    computeAndEmitDuration(hour24, minute)
                                }
                            },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Live Analog Clock Visualizer Badge
                LiveAnalogClockFace(
                    hour = displayHour,
                    minute = minute,
                    isDark = isDark,
                    modifier = Modifier.size(62.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Delta Adjustment Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val chips = listOf(
                    Pair("+15m", 15),
                    Pair("+30m", 30),
                    Pair("+1h", 60),
                    Pair("+2h", 120)
                )
                chips.forEach { (label, deltaMins) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(SquircleMedium)
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x10000000))
                            .border(0.6.dp, if (isDark) Color(0x30FFFFFF) else Color(0x15000000), SquircleMedium)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    val newTarget = System.currentTimeMillis() + (deltaMins * 60_000L)
                                    val c = Calendar.getInstance().apply { timeInMillis = newTarget }
                                    hour24 = c.get(Calendar.HOUR_OF_DAY)
                                    minute = (c.get(Calendar.MINUTE) / 5) * 5
                                    computeAndEmitDuration(hour24, minute)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Digit dial with interactive up/down steppers.
 */
@Composable
private fun ClockDigitDial(
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
            .width(52.dp)
            .clip(SquircleMedium)
            .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
            .border(0.8.dp, if (isDark) Color(0x35FFFFFF) else Color(0x18000000), SquircleMedium)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Increment Button
        Box(
            modifier = Modifier
                .size(22.dp)
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
                modifier = Modifier.size(16.dp)
            )
        }

        // Digit Value
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp
            ),
            color = colors.textPrimary
        )

        // Decrement Button
        Box(
            modifier = Modifier
                .size(22.dp)
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
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * AM/PM Pill Toggle Button
 */
@Composable
private fun AmPmButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors

    val bg by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) Color(0x55FFFFFF) else Color.White
        } else Color.Transparent,
        animationSpec = spring(),
        label = "ampmBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) Color.White else colors.warning
        } else colors.textSecondary,
        animationSpec = spring(),
        label = "ampmText"
    )

    Box(
        modifier = modifier
            .width(34.dp)
            .clip(SquircleMedium)
            .background(bg)
            .then(
                if (isSelected) {
                    Modifier.border(0.6.dp, if (isDark) Color(0x60FFFFFF) else Color(0x25000000), SquircleMedium)
                } else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp
            ),
            color = textColor
        )
    }
}

/**
 * Real-time analog clock canvas showing hour and minute hands.
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
