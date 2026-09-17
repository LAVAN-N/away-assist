package com.awayassist.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleMedium

enum class AppleButtonStyle {
    PRIMARY,
    SECONDARY,
    ACCENT,
    DESTRUCTIVE,
    GHOST
}

/**
 * Hand-rolled Apple-style button with press-opacity feedback (no ripple, no elevation).
 */
@Composable
fun AppleStyleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppleButtonStyle = AppleButtonStyle.PRIMARY,
    enabled: Boolean = true,
    shape: Shape = SquircleMedium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.55f else 1.0f,
        animationSpec = spring(),
        label = "buttonPressAlpha"
    )

    val colors = AwayAssistTheme.colors
    val backgroundColor = when (style) {
        AppleButtonStyle.PRIMARY -> colors.accentSilent
        AppleButtonStyle.ACCENT -> colors.ringState
        AppleButtonStyle.SECONDARY -> colors.cardSurface
        AppleButtonStyle.DESTRUCTIVE -> colors.error
        AppleButtonStyle.GHOST -> Color.Transparent
    }

    val textColor = when (style) {
        AppleButtonStyle.PRIMARY,
        AppleButtonStyle.ACCENT,
        AppleButtonStyle.DESTRUCTIVE -> Color.White
        AppleButtonStyle.SECONDARY -> colors.textPrimary
        AppleButtonStyle.GHOST -> colors.accentSilent
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) pressedAlpha else 0.4f)
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Apple style: press-opacity feedback only, no ripple
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
