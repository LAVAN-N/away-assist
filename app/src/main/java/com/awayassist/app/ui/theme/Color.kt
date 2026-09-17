package com.awayassist.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette (Calm Indigo)
val BackgroundLight = Color(0xFFFAFAFC)
val CardSurfaceLight = Color(0xFFF0F0F5)
val TextPrimaryLight = Color(0xFF1C1C1E)
val TextSecondaryLight = Color(0xFF6E6E73)
val DividerLight = Color(0xFFE5E5EA)

// Dark Palette
val BackgroundDark = Color(0xFF0B0B0F)
val CardSurfaceDark = Color(0xFF18181D)
val TextPrimaryDark = Color(0xFFF2F2F7)
val TextSecondaryDark = Color(0xFF8E8E93)
val DividerDark = Color(0xFF2C2C2E)

// Shared / State Colors
val AccentSilent = Color(0xFF5E5CE6) // Silent / Vibrate state (Calm Indigo)
val RingState = Color(0xFF30D158)    // Ring state (Apple Green)
val WarningColor = Color(0xFFFF9F0A) // Amber / Warning
val ErrorColor = Color(0xFFFF453A)   // Red / Permission Missing
val SwitchOffTrackLight = Color(0xFFE5E5EA)
val SwitchOffTrackDark = Color(0xFF3A3A3C)

@Immutable
data class AwayAssistColors(
    val background: Color,
    val cardSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val accentSilent: Color,
    val ringState: Color,
    val warning: Color,
    val error: Color,
    val switchOffTrack: Color,
    val isDark: Boolean
)

val LocalAwayAssistColors = staticCompositionLocalOf {
    AwayAssistColors(
        background = BackgroundLight,
        cardSurface = CardSurfaceLight,
        textPrimary = TextPrimaryLight,
        textSecondary = TextSecondaryLight,
        divider = DividerLight,
        accentSilent = AccentSilent,
        ringState = RingState,
        warning = WarningColor,
        error = ErrorColor,
        switchOffTrack = SwitchOffTrackLight,
        isDark = false
    )
}
