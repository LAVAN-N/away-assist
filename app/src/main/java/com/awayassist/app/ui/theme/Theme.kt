package com.awayassist.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = AwayAssistColors(
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

private val DarkColors = AwayAssistColors(
    background = BackgroundDark,
    cardSurface = CardSurfaceDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    divider = DividerDark,
    accentSilent = AccentSilent,
    ringState = RingState,
    warning = WarningColor,
    error = ErrorColor,
    switchOffTrack = SwitchOffTrackDark,
    isDark = true
)

private val MaterialLightColorScheme = lightColorScheme(
    primary = AccentSilent,
    background = BackgroundLight,
    surface = CardSurfaceLight,
    onPrimary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

private val MaterialDarkColorScheme = darkColorScheme(
    primary = AccentSilent,
    background = BackgroundDark,
    surface = CardSurfaceDark,
    onPrimary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

object AwayAssistTheme {
    val colors: AwayAssistColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAwayAssistColors.current
}

@Composable
fun AwayAssistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val customColors = if (darkTheme) DarkColors else LightColors
    val materialColors = if (darkTheme) MaterialDarkColorScheme else MaterialLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAwayAssistColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = AppTypography,
            content = content
        )
    }
}
