package com.awayassist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleMedium

enum class OperationMode(val label: String, val icon: ImageVector) {
    AUTO("Auto", Icons.Default.Bolt),
    FORCE_RING("Ring", Icons.Default.NotificationsActive),
    PAUSE("Pause", Icons.Default.Schedule)
}

/**
 * Unified 3-way liquid segmented control for Auto, Ring, and Pause modes.
 * Selecting one automatically switches off the other two.
 */
@Composable
fun LiquidModeSelector(
    selectedMode: OperationMode,
    onModeSelected: (OperationMode) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = AwayAssistTheme.colors.isDark
) {
    val colors = AwayAssistTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleMedium)
            .background(if (isDark) Color(0x1EFFFFFF) else Color(0x14000000))
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0x35FFFFFF), Color(0x0CFFFFFF))
                    } else {
                        listOf(Color(0x80FFFFFF), Color(0x25FFFFFF))
                    }
                ),
                shape = SquircleMedium
            )
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        OperationMode.entries.forEach { mode ->
            val isSelected = selectedMode == mode
            val interactionSource = remember { MutableInteractionSource() }

            val activeColor = when (mode) {
                OperationMode.AUTO -> colors.accentSilent
                OperationMode.FORCE_RING -> colors.ringState
                OperationMode.PAUSE -> colors.warning
            }

            val itemBgColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) Color(0x45FFFFFF) else Color(0xC8FFFFFF)
                } else {
                    Color.Transparent
                },
                animationSpec = spring(),
                label = "modeSegmentBg"
            )

            val itemTextColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) Color.White else activeColor
                } else {
                    colors.textSecondary
                },
                animationSpec = spring(),
                label = "modeSegmentText"
            )

            val iconColor by animateColorAsState(
                targetValue = if (isSelected) {
                    activeColor
                } else {
                    colors.textSecondary.copy(alpha = 0.7f)
                },
                animationSpec = spring(),
                label = "modeSegmentIcon"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(SquircleMedium)
                    .background(itemBgColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 0.9.dp,
                                color = if (isDark) Color(0x60FFFFFF) else Color(0x95FFFFFF),
                                shape = SquircleMedium
                            )
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onModeSelected(mode) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = itemTextColor
                    )
                }
            }
        }
    }
}
