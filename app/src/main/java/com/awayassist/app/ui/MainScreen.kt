package com.awayassist.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.R
import com.awayassist.app.data.AppState
import com.awayassist.app.data.RingerState
import com.awayassist.app.ui.components.AppleButtonStyle
import com.awayassist.app.ui.components.AppleStyleButton
import com.awayassist.app.ui.components.AppleStyleSwitch
import com.awayassist.app.ui.components.GroupedListCard
import com.awayassist.app.ui.components.GroupedListRow
import com.awayassist.app.ui.components.LiquidMeshBackground
import com.awayassist.app.ui.components.LiquidPillBadge
import com.awayassist.app.ui.components.LiquidPulsingHalo
import com.awayassist.app.ui.components.glassmorphic
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
    val isDark = colors.isDark

    // Active ambient glow color corresponding to current ringer / app state
    val targetAmbientColor = when {
        !hasNotificationPolicyAccess -> colors.error
        !appState.isEnabled -> colors.textSecondary
        appState.isPaused -> colors.warning
        appState.overrideMode != null -> colors.azureGlow
        appState.currentMode == RingerState.RING -> colors.ringState
        else -> colors.accentSilent
    }

    val animatedAmbientColor by animateColorAsState(
        targetValue = targetAmbientColor,
        animationSpec = spring(),
        label = "ambientMeshColor"
    )

    LiquidMeshBackground(
        activeColor = animatedAmbientColor,
        isDark = isDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header with Glassmorphic App Icon & Liquid Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Glassmorphic App Icon Logo
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(SquircleMedium)
                            .shadow(8.dp, SquircleMedium)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0x80FFFFFF), Color(0x20FFFFFF))
                                ),
                                shape = SquircleMedium
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Away-Assist App Icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Away-Assist",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Liquid Lock-Aware Audio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                // Status Pill Badge
                val badgeText = when {
                    !hasNotificationPolicyAccess -> "Setup"
                    !appState.isEnabled -> "Disabled"
                    appState.isPaused -> "Paused"
                    appState.overrideMode != null -> "Override"
                    appState.currentMode == RingerState.RING -> "Ring"
                    else -> "Vibrate"
                }
                LiquidPillBadge(
                    text = badgeText,
                    tintColor = animatedAmbientColor
                )
            }

            // Permission Warning Card if missing
            AnimatedVisibility(
                visible = !hasNotificationPolicyAccess,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PermissionRequiredCard(
                    onRequestPolicyAccess = onRequestPolicyAccess,
                    isDark = isDark,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // Hero Live Status Glassmorphic Card
            LiveStatusCard(
                appState = appState,
                hasPolicyAccess = hasNotificationPolicyAccess,
                onForceRing = onForceRing,
                onForceSilent = onForceSilent,
                onPause1h = onPause1h,
                onResumeAutomation = onResumeAutomation,
                isDark = isDark,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Master Automation Toggle Card
            GroupedListCard(
                header = "Automation Engine",
                footer = "Away-Assist continuously listens for hardware lock/unlock events with zero battery polling.",
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                GroupedListRow(
                    title = "Enable Automation",
                    subtitle = if (appState.isEnabled) "Service running in background" else "Automation paused",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.accentSilent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (appState.isEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = colors.accentSilent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    trailingContent = {
                        AppleStyleSwitch(
                            checked = appState.isEnabled,
                            onCheckedChange = onToggleEnabled,
                            activeColor = colors.ringState
                        )
                    }
                )
            }

            // Automation Rules Glassmorphic Card
            GroupedListCard(
                header = "State Rules",
                footer = "Deterministic switching: Screen locked triggers Normal audible mode; screen unlocked restores Vibrate mode.",
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                GroupedListRow(
                    title = "Screen Locked",
                    subtitle = "When phone is put away on desk or in pocket",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.ringState.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = colors.ringState,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    trailingContent = {
                        LiquidPillBadge(
                            text = "Ring (Normal)",
                            tintColor = colors.ringState
                        )
                    },
                    showDivider = true
                )
                GroupedListRow(
                    title = "Screen Unlocked",
                    subtitle = "When phone is actively being handled",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.accentSilent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = colors.accentSilent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    trailingContent = {
                        LiquidPillBadge(
                            text = "Vibrate",
                            tintColor = colors.accentSilent
                        )
                    }
                )
            }

            // System & Permissions Status Card
            GroupedListCard(
                header = "System Integrity",
                modifier = Modifier.padding(bottom = 36.dp)
            ) {
                GroupedListRow(
                    title = "Do Not Disturb Access",
                    subtitle = if (hasNotificationPolicyAccess) "Granted • Ringer switching authorized" else "Required • Tap to grant in Settings",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasNotificationPolicyAccess) colors.ringState.copy(alpha = 0.15f) else colors.error.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (hasNotificationPolicyAccess) colors.ringState else colors.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
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
                    title = "Background Foreground Service",
                    subtitle = if (appState.isEnabled) "Active with low-priority notification" else "Service stopped",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.azureGlow.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = colors.azureGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
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
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors

    val (statusColor, statusTitle, statusSubtitle, statusIcon) = when {
        !hasPolicyAccess -> {
            Quad(
                colors.error,
                "Permission Required",
                "Grant Notification Policy Access so Away-Assist can switch ringer modes",
                Icons.Default.Warning
            )
        }
        !appState.isEnabled -> {
            Quad(
                colors.textSecondary,
                "Automation Disabled",
                "Enable the toggle below to activate automatic ringer switching",
                Icons.Default.NotificationsOff
            )
        }
        appState.isPaused -> {
            val remainingMins = ((appState.pauseUntilTimestamp - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
            Quad(
                colors.warning,
                "Automation Paused",
                "Paused for next ${remainingMins}m • Tap Resume to reactivate",
                Icons.Default.PauseCircle
            )
        }
        appState.overrideMode != null -> {
            when (appState.overrideMode) {
                RingerState.RING -> Quad(
                    colors.ringState,
                    "Force Ring Active",
                    "Manual override • Resets on next screen unlock",
                    Icons.Default.Notifications
                )
                RingerState.VIBRATE, RingerState.SILENT -> Quad(
                    colors.accentSilent,
                    "Force Silent Active",
                    "Manual override • Resets on next screen lock",
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
                "Ring Mode Active",
                "Screen locked • Incoming calls and alerts are audible",
                Icons.Default.Notifications
            )
        }
        appState.currentMode == RingerState.VIBRATE -> {
            Quad(
                colors.accentSilent,
                "Vibrate Mode Active",
                "Screen unlocked • Silent vibration during active use",
                Icons.Default.Vibration
            )
        }
        else -> {
            Quad(
                colors.accentSilent,
                "Monitoring Lock State",
                "Ready to switch ringer mode on lock/unlock events",
                Icons.Default.Notifications
            )
        }
    }

    val animatedStatusColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = spring(),
        label = "heroStatusColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = SquircleLarge,
                tintColor = animatedStatusColor,
                isDark = isDark
            )
            .padding(22.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Liquid Pulsing Halo around status icon
                LiquidPulsingHalo(glowColor = animatedStatusColor) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        animatedStatusColor.copy(alpha = 0.35f),
                                        animatedStatusColor.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = animatedStatusColor.copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = animatedStatusColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            // Glassmorphic Quick Action Buttons
            if (hasPolicyAccess && appState.isEnabled) {
                Spacer(modifier = Modifier.height(20.dp))

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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp)
                            )
                        } else {
                            AppleStyleButton(
                                text = "Force Ring",
                                onClick = onForceRing,
                                style = AppleButtonStyle.ACCENT,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp)
                            )
                        }

                        AppleStyleButton(
                            text = "Pause 1h",
                            onClick = onPause1h,
                            style = AppleButtonStyle.GLASS,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp)
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
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = SquircleLarge,
                tintColor = colors.error,
                isDark = isDark
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.error.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Permission Setup Required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Android requires Notification Policy Access (Do Not Disturb access) so Away-Assist can switch between Ring and Vibrate modes silently in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

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
