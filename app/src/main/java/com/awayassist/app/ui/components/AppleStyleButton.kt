package com.awayassist.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleMedium

enum class AppleButtonStyle {
    PRIMARY,
    ACCENT,
    SECONDARY,
    DESTRUCTIVE,
    GHOST,
    GLASS
}

/**
 * Hand-rolled Liquid Glassmorphic Button with fluid press scaling and specular highlights.
 */
@Composable
fun AppleStyleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppleButtonStyle = AppleButtonStyle.PRIMARY,
    enabled: Boolean = true,
    shape: Shape = SquircleMedium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.65f else 1.0f,
        animationSpec = spring(),
        label = "buttonPressAlpha"
    )

    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(),
        label = "buttonPressScale"
    )

    val colors = AwayAssistTheme.colors
    val isDark = colors.isDark

    val backgroundBrush = when (style) {
        AppleButtonStyle.PRIMARY -> Brush.horizontalGradient(
            colors = listOf(
                colors.accentSilent,
                colors.azureGlow
            )
        )
        AppleButtonStyle.ACCENT -> Brush.horizontalGradient(
            colors = listOf(
                colors.ringState,
                colors.cyanGlow
            )
        )
        AppleButtonStyle.SECONDARY -> Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0x35FFFFFF), Color(0x18FFFFFF))
            } else {
                listOf(Color(0x90FFFFFF), Color(0x60FFFFFF))
            }
        )
        AppleButtonStyle.GLASS -> Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0x28FFFFFF), Color(0x10FFFFFF))
            } else {
                listOf(Color(0x70FFFFFF), Color(0x40FFFFFF))
            }
        )
        AppleButtonStyle.DESTRUCTIVE -> Brush.horizontalGradient(
            colors = listOf(
                colors.error,
                Color(0xFFFF6961)
            )
        )
        AppleButtonStyle.GHOST -> Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }

    val borderBrush = when (style) {
        AppleButtonStyle.SECONDARY, AppleButtonStyle.GLASS -> Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0x50FFFFFF), Color(0x10FFFFFF))
            } else {
                listOf(Color(0xA0FFFFFF), Color(0x30FFFFFF))
            }
        )
        AppleButtonStyle.PRIMARY, AppleButtonStyle.ACCENT, AppleButtonStyle.DESTRUCTIVE -> Brush.linearGradient(
            colors = listOf(Color(0x60FFFFFF), Color(0x15FFFFFF))
        )
        AppleButtonStyle.GHOST -> null
    }

    val textColor = when (style) {
        AppleButtonStyle.PRIMARY,
        AppleButtonStyle.ACCENT,
        AppleButtonStyle.DESTRUCTIVE -> Color.White
        AppleButtonStyle.SECONDARY,
        AppleButtonStyle.GLASS -> colors.textPrimary
        AppleButtonStyle.GHOST -> colors.accentSilent
    }

    Box(
        modifier = modifier
            .scale(if (enabled) pressedScale else 1.0f)
            .alpha(if (enabled) pressedAlpha else 0.4f)
            .clip(shape)
            .background(backgroundBrush)
            .then(
                if (borderBrush != null) {
                    Modifier.border(width = 0.8.dp, brush = borderBrush, shape = shape)
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Box(modifier = Modifier.padding(end = 8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
        }
    }
}
