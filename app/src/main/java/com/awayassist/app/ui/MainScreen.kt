package com.awayassist.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.awayassist.app.data.ThemeMode
import com.awayassist.app.ui.components.AppleButtonStyle
import com.awayassist.app.ui.components.AppleStyleButton
import com.awayassist.app.ui.components.AppleStyleSwitch
import com.awayassist.app.ui.components.GroupedListCard
import com.awayassist.app.ui.components.GroupedListRow
import com.awayassist.app.ui.components.LiquidMeshBackground
import com.awayassist.app.ui.components.LiquidPillBadge
import com.awayassist.app.ui.components.LiquidPulsingHalo
import com.awayassist.app.ui.components.LiquidThemeSelector
import com.awayassist.app.ui.components.glassmorphic
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.SquircleLarge
import com.awayassist.app.ui.theme.SquircleMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    appState: AppState,
    hasNotificationPolicyAccess: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onForceRing: () -> Unit,
    onForceSilent: () -> Unit,
    onPause1h: () -> Unit,
    onResumeAutomation: () -> Unit,
    onRequestPolicyAccess: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit
) {
    val scrollState = rememberScrollState()
    val colors = AwayAssistTheme.colors
    val isDark = colors.isDark

    var showInfoSheet by remember { mutableStateOf(false) }

    // Active ambient glow color corresponding to current ringer / app state
    val targetAmbientColor = when {
        !hasNotificationPolicyAccess -> colors.error
        appState.overrideMode != null -> colors.azureGlow
        appState.isPaused -> colors.warning
        !appState.isEnabled -> colors.textSecondary
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
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // Compact Header: Logo, Title, Badge & Info Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(SquircleMedium)
                            .shadow(6.dp, SquircleMedium)
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
                            contentDescription = "App Icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Away Assist",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (appState.isEnabled) "Automated Ringer Active" else "Automation Disabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeText = when {
                        !hasNotificationPolicyAccess -> "Setup"
                        appState.overrideMode != null -> "Override"
                        appState.isPaused -> "Paused"
                        !appState.isEnabled -> "Off"
                        appState.currentMode == RingerState.RING -> "Ring"
                        else -> "Vibrate"
                    }
                    LiquidPillBadge(
                        text = badgeText,
                        tintColor = animatedAmbientColor
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Info (i) button for clean informative details
                    IconButton(
                        onClick = { showInfoSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x20FFFFFF) else Color(0x15000000))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About and Help",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Compact Missing Permission Card
            AnimatedVisibility(
                visible = !hasNotificationPolicyAccess,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CompactPermissionCard(
                    onRequestPolicyAccess = onRequestPolicyAccess,
                    isDark = isDark,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // Compact Hero Status Card with Glanceable Actions
            CompactStatusCard(
                appState = appState,
                hasPolicyAccess = hasNotificationPolicyAccess,
                onForceRing = onForceRing,
                onForceSilent = onForceSilent,
                onPause1h = onPause1h,
                onResumeAutomation = onResumeAutomation,
                isDark = isDark,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Compact 2-Tile Rules Grid (Screen Locked vs Screen Unlocked)
            CompactRulesGrid(
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Settings & Controls Card (Toggle & Theme)
            GroupedListCard(
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                GroupedListRow(
                    title = "Automation",
                    subtitle = if (appState.isEnabled) "Running in background" else "Paused",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colors.accentSilent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (appState.isEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = colors.accentSilent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    trailingContent = {
                        AppleStyleSwitch(
                            checked = appState.isEnabled,
                            onCheckedChange = onToggleEnabled,
                            activeColor = colors.ringState
                        )
                    },
                    showDivider = true
                )

                // Compact Theme Selector Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.cyanGlow.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = colors.cyanGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = colors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LiquidThemeSelector(
                        currentTheme = appState.themeMode,
                        onThemeSelected = onSelectTheme
                    )
                }
            }

            // System Status Compact Row
            GroupedListCard(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                GroupedListRow(
                    title = "Do Not Disturb Access",
                    subtitle = if (hasNotificationPolicyAccess) "Access granted" else "Tap to grant",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
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
                                modifier = Modifier.size(16.dp)
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    },
                    onClick = onRequestPolicyAccess
                )
            }
        }

        // Informative Info Modal Bottom Sheet
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = if (isDark) Color(0xFF141520) else Color(0xFFFAFAFC),
                dragHandle = null
            ) {
                InfoBottomSheetContent(onClose = { showInfoSheet = false })
            }
        }
    }
}

/**
 * Compact Glanceable Hero Status Card
 */
@Composable
private fun CompactStatusCard(
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
                "Grant DND access to enable automated ringer switching",
                Icons.Default.Warning
            )
        }
        appState.overrideMode != null -> {
            when (appState.overrideMode) {
                RingerState.RING -> Quad(
                    colors.ringState,
                    "Force Ring Active",
                    "Automation disabled • Tap Resume to reactivate",
                    Icons.Default.Notifications
                )
                RingerState.VIBRATE, RingerState.SILENT -> Quad(
                    colors.accentSilent,
                    "Force Silent Active",
                    "Automation disabled • Tap Resume to reactivate",
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
        appState.isPaused -> {
            val remainingMins = ((appState.pauseUntilTimestamp - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
            Quad(
                colors.warning,
                "Paused (${remainingMins}m)",
                "Automation temporarily paused",
                Icons.Default.PauseCircle
            )
        }
        !appState.isEnabled -> {
            Quad(
                colors.textSecondary,
                "Automation Off",
                "Enable toggle below to automate ringer",
                Icons.Default.NotificationsOff
            )
        }
        appState.currentMode == RingerState.RING -> {
            Quad(
                colors.ringState,
                "Ring Mode Active",
                "Screen locked • Calls & alerts audible",
                Icons.Default.Notifications
            )
        }
        appState.currentMode == RingerState.VIBRATE -> {
            Quad(
                colors.accentSilent,
                "Vibrate Mode Active",
                "Screen unlocked • Silent vibration in use",
                Icons.Default.Vibration
            )
        }
        else -> {
            Quad(
                colors.accentSilent,
                "Ready",
                "Monitoring lock/unlock transitions",
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
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidPulsingHalo(glowColor = animatedStatusColor) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = colors.textSecondary
                    )
                }
            }

            // Compact Quick Actions
            if (hasPolicyAccess) {
                if (appState.isPaused || appState.overrideMode != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    AppleStyleButton(
                        text = "Resume Automation",
                        onClick = onResumeAutomation,
                        style = AppleButtonStyle.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                    )
                } else if (appState.isEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (appState.currentMode == RingerState.RING) {
                            AppleStyleButton(
                                text = "Force Silent",
                                onClick = onForceSilent,
                                style = AppleButtonStyle.PRIMARY,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
                            )
                        } else {
                            AppleStyleButton(
                                text = "Force Ring",
                                onClick = onForceRing,
                                style = AppleButtonStyle.ACCENT,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
                            )
                        }

                        AppleStyleButton(
                            text = "Pause 1h",
                            onClick = onPause1h,
                            style = AppleButtonStyle.GLASS,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact 2-Tile Rules Grid
 */
@Composable
private fun CompactRulesGrid(modifier: Modifier = Modifier) {
    val colors = AwayAssistTheme.colors
    val isDark = colors.isDark

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tile 1: Locked -> Ring
        Box(
            modifier = Modifier
                .weight(1f)
                .glassmorphic(shape = SquircleMedium, tintColor = colors.ringState, isDark = isDark)
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    LiquidPillBadge(text = "Ring", tintColor = colors.ringState)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Screen Locked",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = colors.textPrimary
                )
                Text(
                    text = "Audible calls",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // Tile 2: Unlocked -> Vibrate
        Box(
            modifier = Modifier
                .weight(1f)
                .glassmorphic(shape = SquircleMedium, tintColor = colors.accentSilent, isDark = isDark)
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    LiquidPillBadge(text = "Vibrate", tintColor = colors.accentSilent)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Screen Unlocked",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = colors.textPrimary
                )
                Text(
                    text = "Silent in use",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

/**
 * Compact Missing Permission Banner
 */
@Composable
private fun CompactPermissionCard(
    onRequestPolicyAccess: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(shape = SquircleMedium, tintColor = colors.error, isDark = isDark)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DND Permission Needed",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = colors.error
                )
                Text(
                    text = "Required to switch ringer mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary
                )
            }
            AppleStyleButton(
                text = "Grant",
                onClick = onRequestPolicyAccess,
                style = AppleButtonStyle.DESTRUCTIVE,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Clean Info Bottom Sheet with full details when (i) is tapped
 */
@Composable
private fun InfoBottomSheetContent(onClose: () -> Unit) {
    val colors = AwayAssistTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colors.accentSilent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "How Away Assist Works",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        InfoItem(
            title = "Deterministic Switching",
            description = "When you lock your phone, it switches to Normal Audible Ring mode so you never miss a call while away. When you unlock it, it restores Vibrate mode to prevent disturbances during active use."
        )

        Spacer(modifier = Modifier.height(14.dp))

        InfoItem(
            title = "Zero Battery Cost & No Polling",
            description = "Away Assist uses event-driven hardware broadcast triggers (ACTION_SCREEN_OFF / ACTION_USER_PRESENT). It never runs background polling loops or periodic battery-draining alarms."
        )

        Spacer(modifier = Modifier.height(14.dp))

        InfoItem(
            title = "100% On-Device & Privacy Safe",
            description = "No internet permissions requested. All logic and preferences remain strictly on your device."
        )

        Spacer(modifier = Modifier.height(20.dp))

        AppleStyleButton(
            text = "Got it",
            onClick = onClose,
            style = AppleButtonStyle.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoItem(title: String, description: String) {
    val colors = AwayAssistTheme.colors
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = colors.textSecondary
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
