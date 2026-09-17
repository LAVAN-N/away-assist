package com.awayassist.app.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.data.AppState
import com.awayassist.app.data.RingerState
import com.awayassist.app.ui.components.AppleButtonStyle
import com.awayassist.app.ui.components.AppleStyleButton
import com.awayassist.app.ui.components.AppleStyleSwitch
import com.awayassist.app.ui.components.GroupedListCard
import com.awayassist.app.ui.components.GroupedListRow
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleLarge
import com.awayassist.app.ui.theme.SquircleMedium

@Composable
fun MainScreen(
    appState: AppState,
    hasNotificationPolicyAccess: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onForceRing: () -> Unit,
    onForceSilent: () -> Unit,
    onPause1h: () -> Unit,
    onResumeAutomation: () -> Unit,
    onRequestPolicyAccess: () -> Unit
) {
    val scrollState = rememberScrollState()
    val colors = AwayAssistTheme.colors

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header
            Text(
                text = "Away-Assist",
                style = MaterialTheme.typography.displayLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Text(
                text = "Smart lock-aware ringer switching",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Permission Warning Banner if missing
            AnimatedVisibility(
                visible = !hasNotificationPolicyAccess,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PermissionRequiredCard(
                    onRequestPolicyAccess = onRequestPolicyAccess,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // Live Status Card
            LiveStatusCard(
                appState = appState,
                hasPolicyAccess = hasNotificationPolicyAccess,
                onForceRing = onForceRing,
                onForceSilent = onForceSilent,
                onPause1h = onPause1h,
                onResumeAutomation = onResumeAutomation,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Master Toggle Grouped Card
            GroupedListCard(
                header = "Automation",
                footer = "When enabled, Away-Assist switches your phone to Ring mode whenever the screen is locked, and back to Vibrate when unlocked.",
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                GroupedListRow(
                    title = "Enable Automation",
                    subtitle = if (appState.isEnabled) "Service running in background" else "Automation paused",
                    trailingContent = {
                        AppleStyleSwitch(
                            checked = appState.isEnabled,
                            onCheckedChange = onToggleEnabled,
                            activeColor = colors.ringState
                        )
                    }
                )
            }

            // Rules Card (Read-only Info)
            GroupedListCard(
                header = "Automation Rules",
                footer = "Fixed configuration for v1. Ringer switches automatically on hardware screen lock / unlock events.",
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                GroupedListRow(
                    title = "On Screen Lock",
                    subtitle = "When device is put away / locked",
                    trailingContent = {
                        Text(
                            text = "Ring (Audible)",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.ringState
                        )
                    },
                    showDivider = true
                )
                GroupedListRow(
                    title = "On Screen Unlock",
                    subtitle = "When device is actively in use",
                    trailingContent = {
                        Text(
                            text = "Vibrate",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accentSilent
                        )
                    }
                )
            }

            // System & Permissions Card
            GroupedListCard(
                header = "System Status",
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                GroupedListRow(
                    title = "Notification Policy Access",
                    subtitle = if (hasNotificationPolicyAccess) "Required to change ringer mode" else "Tap to grant in Android Settings",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hasNotificationPolicyAccess) "Granted" else "Required",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (hasNotificationPolicyAccess) colors.ringState else colors.error
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    },
                    onClick = onRequestPolicyAccess,
                    showDivider = true
                )
                GroupedListRow(
                    title = "Foreground Service",
                    subtitle = if (appState.isEnabled) "Active with low-priority notification" else "Stopped",
                    trailingContent = {
                        Text(
                            text = if (appState.isEnabled) "Active" else "Stopped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (appState.isEnabled) colors.ringState else colors.textSecondary
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveStatusCard(
    appState: AppState,
    hasPolicyAccess: Boolean,
    onForceRing: () -> Unit,
    onForceSilent: () -> Unit,
    onPause1h: () -> Unit,
    onResumeAutomation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors

    val (statusColor, statusTitle, statusSubtitle, statusIcon) = when {
        !hasPolicyAccess -> {
            Quad(
                colors.error,
                "Permission Missing",
                "Grant Notification Policy Access to enable ringer changes",
                Icons.Default.Warning
            )
        }
        !appState.isEnabled -> {
            Quad(
                colors.textSecondary,
                "Automation Off",
                "Enable the toggle below to activate smart switching",
                Icons.Default.NotificationsOff
            )
        }
        appState.isPaused -> {
            val remainingMins = ((appState.pauseUntilTimestamp - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
            Quad(
                colors.warning,
                "Paused",
                "Automation paused for next ${remainingMins}m",
                Icons.Default.PauseCircle
            )
        }
        appState.overrideMode != null -> {
            when (appState.overrideMode) {
                RingerState.RING -> Quad(
                    colors.ringState,
                    "Force Ring Active",
                    "Manual override • Will reset on next screen unlock",
                    Icons.Default.Notifications
                )
                RingerState.VIBRATE, RingerState.SILENT -> Quad(
                    colors.accentSilent,
                    "Force Silent Active",
                    "Manual override • Will reset on next screen lock",
                    Icons.Default.Vibration
                )
                else -> Quad(
                    colors.warning,
                    "Override Active",
                    "Manual override active",
                    Icons.Default.Notifications
                )
            }
        }
        appState.currentMode == RingerState.RING -> {
            Quad(
                colors.ringState,
                "Ring Mode",
                "Screen locked • Calls and alerts are audible",
                Icons.Default.Notifications
            )
        }
        appState.currentMode == RingerState.VIBRATE -> {
            Quad(
                colors.accentSilent,
                "Vibrate Mode",
                "Screen unlocked • Silent vibration during active use",
                Icons.Default.Vibration
            )
        }
        else -> {
            Quad(
                colors.accentSilent,
                "Ready",
                "Listening for screen lock/unlock events",
                Icons.Default.Notifications
            )
        }
    }

    val animatedAccentColor = animateColorAsState(
        targetValue = statusColor,
        animationSpec = spring(),
        label = "statusCardColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleLarge)
            .background(colors.cardSurface)
            .padding(20.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(animatedAccentColor.value.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = animatedAccentColor.value,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            // Quick action buttons if automation is active
            if (hasPolicyAccess && appState.isEnabled) {
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (appState.isPaused || appState.overrideMode != null) {
                        AppleStyleButton(
                            text = "Resume Automation",
                            onClick = onResumeAutomation,
                            style = AppleButtonStyle.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        if (appState.currentMode == RingerState.RING) {
                            AppleStyleButton(
                                text = "Force Silent",
                                onClick = onForceSilent,
                                style = AppleButtonStyle.PRIMARY,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            )
                        } else {
                            AppleStyleButton(
                                text = "Force Ring",
                                onClick = onForceRing,
                                style = AppleButtonStyle.ACCENT,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }

                        AppleStyleButton(
                            text = "Pause 1h",
                            onClick = onPause1h,
                            style = AppleButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    onRequestPolicyAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleLarge)
            .background(colors.error.copy(alpha = 0.10f))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Permission Setup Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Android requires Notification Policy Access (Do Not Disturb access) so Away-Assist can switch between Ring and Vibrate modes in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            AppleStyleButton(
                text = "Grant Permission in Settings",
                onClick = onRequestPolicyAccess,
                style = AppleButtonStyle.DESTRUCTIVE,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
