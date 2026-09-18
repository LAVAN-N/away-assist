package com.awayassist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.RingState

/**
 * Hand-rolled Liquid Glassmorphic Apple-style Switch.
 * Features spring-based motion, liquid glowing gradients, and specular highlights.
 */
@Composable
fun AppleStyleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = RingState
) {
    val trackWidth = 52.dp
    val trackHeight = 32.dp
    val thumbSize = 28.dp
    val thumbPadding = 2.dp

    val maxOffset = trackWidth - thumbSize - (thumbPadding * 2)

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "switchThumbOffset"
    )

    val isDark = AwayAssistTheme.colors.isDark
    val offTrackColor = AwayAssistTheme.colors.switchOffTrack
    val targetTrackColor = if (checked) activeColor else offTrackColor

    val trackColor by animateColorAsState(
        targetValue = if (enabled) targetTrackColor else offTrackColor.copy(alpha = 0.4f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switchTrackColor"
    )

    val trackGradient = if (checked) {
        Brush.horizontalGradient(
            colors = listOf(
                activeColor,
                activeColor.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                trackColor,
                trackColor.copy(alpha = 0.7f)
            )
        )
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(100.dp))
            .background(trackGradient)
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = if (checked) {
                        listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    } else {
                        listOf(
                            if (isDark) Color(0x35FFFFFF) else Color(0x60FFFFFF),
                            if (isDark) Color(0x10FFFFFF) else Color(0x20000000)
                        )
                    }
                ),
                shape = RoundedCornerShape(100.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch
            ) {
                onCheckedChange(!checked)
            }
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(
                    elevation = if (enabled) 3.dp else 0.dp,
                    shape = CircleShape,
                    clip = false
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF0F1F5)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape
                )
        )
    }
}
