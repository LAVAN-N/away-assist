package com.awayassist.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Light Palette (Warm Sunlit Champagne & Fluid Frost)
val BackgroundLight = Color(0xFFF9F6F0)
val CardSurfaceLight = Color(0xD6FFFFFF) // Translucent for glassmorphism
val CardBorderLight = Color(0x65FFE8B2)
val TextPrimaryLight = Color(0xFF1C1917)
val TextSecondaryLight = Color(0xFF78716C)
val DividerLight = Color(0x18423420)

// Dark Palette (Midnight Glass)
val BackgroundDark = Color(0xFF090A10)
val CardSurfaceDark = Color(0x99161722) // Translucent midnight glass
val CardBorderDark = Color(0x28FFFFFF)
val TextPrimaryDark = Color(0xFFF5F5FA)
val TextSecondaryDark = Color(0xFF9090A0)
val DividerDark = Color(0x1FFFFFFF)

// Shared / State Colors & Liquid Accents
val AccentSilent = Color(0xFF5E5CE6)  // Calm Indigo (Vibrate/Silent state)
val RingState = Color(0xFF30D158)     // Apple Neon Green (Ring state)
val CyanGlow = Color(0xFF00D2FF)      // Fluid Cyan Accent
val AzureGlow = Color(0xFF0A84FF)     // Electric Azure Accent
val AmberGlow = Color(0xFFFF9F0A)     // Liquid Amber (Pause)
val CoralGlow = Color(0xFFFF453A)     // Liquid Coral (Permission/Error)

val SwitchOffTrackLight = Color(0x28787880)
val SwitchOffTrackDark = Color(0x38FFFFFF)

@Immutable
data class AwayAssistColors(
    val background: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val accentSilent: Color,
    val ringState: Color,
    val cyanGlow: Color,
    val azureGlow: Color,
    val warning: Color,
    val error: Color,
    val switchOffTrack: Color,
    val isDark: Boolean
)

val LocalAwayAssistColors = staticCompositionLocalOf {
    AwayAssistColors(
        background = BackgroundLight,
        cardSurface = CardSurfaceLight,
        cardBorder = CardBorderLight,
        textPrimary = TextPrimaryLight,
        textSecondary = TextSecondaryLight,
        divider = DividerLight,
        accentSilent = AccentSilent,
        ringState = RingState,
        cyanGlow = CyanGlow,
        azureGlow = AzureGlow,
        warning = AmberGlow,
        error = CoralGlow,
        switchOffTrack = SwitchOffTrackLight,
        isDark = false
    )
}
