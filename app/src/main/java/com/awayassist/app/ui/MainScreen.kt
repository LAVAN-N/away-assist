package com.awayassist.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.awayassist.app.ui.theme.SquirclePill

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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Sophisticated Editorial Glass Header
            EditorialHeader(
                appState = appState,
                hasPolicyAccess = hasNotificationPolicyAccess,
                statusColor = animatedAmbientColor,
                isDark = isDark,
                onInfoClick = { showInfoSheet = true }
            )

            // Compact Missing Permission Card
            AnimatedVisibility(
                visible = !hasNotificationPolicyAccess,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CompactPermissionCard(
                    onRequestPolicyAccess = onRequestPolicyAccess,
                    isDark = isDark,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Compact Hero Status Card
            CompactStatusCard(
                appState = appState,
                hasPolicyAccess = hasNotificationPolicyAccess,
                onForceRing = onForceRing,
                onForceSilent = onForceSilent,
                onPause1h = onPause1h,
                onResumeAutomation = onResumeAutomation,
                isDark = isDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 2-Tile Rules Grid (Screen Locked vs Screen Unlocked)
            CompactRulesGrid(
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Combined Controls Card (Automation Toggle + Appearance Segment)
            GroupedListCard(
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                GroupedListRow(
                    title = "Automation",
                    subtitle = if (appState.isEnabled) "Automating on lock / unlock" else "Paused",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.accentSilent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (appState.isEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = colors.accentSilent,
                                modifier = Modifier.size(15.dp)
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

                // Theme Mode Segment Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.cyanGlow.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = colors.cyanGlow,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LiquidThemeSelector(
                        currentTheme = appState.themeMode,
                        onThemeSelected = onSelectTheme
                    )
                }
            }

            // System Status Card
            GroupedListCard(
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                GroupedListRow(
                    title = "Do Not Disturb Access",
                    subtitle = if (hasNotificationPolicyAccess) "Access granted" else "Required to modify ringer",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
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
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hasNotificationPolicyAccess) "Granted" else "Grant",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = if (hasNotificationPolicyAccess) colors.ringState else colors.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    },
                    onClick = onRequestPolicyAccess
                )
            }
        }

        // Info Modal Bottom Sheet
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = if (isDark) Color(0xFF141520) else Color(0xFFFAFAFC),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = null
            ) {
                InfoBottomSheetContent(onClose = { showInfoSheet = false })
            }
        }
    }
}

/**
 * Sophisticated Editorial Glass Header
 */
@Composable
private fun EditorialHeader(
    appState: AppState,
    hasPolicyAccess: Boolean,
    statusColor: Color,
    isDark: Boolean,
    onInfoClick: () -> Unit
) {
    val colors = AwayAssistTheme.colors

    val infiniteTransition = rememberInfiniteTransition(label = "beaconPulse")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Title & Live Beacon
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Away Assist",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.6).sp
                    ),
                    color = colors.textPrimary
                )

                if (appState.isEnabled && hasPolicyAccess) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(beaconAlpha)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
            }

            Text(
                text = "Dynamic Lock & Unlock Assist",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp
                ),
                color = colors.textSecondary
            )
        }

        // Dynamic Glass Island Capsule (Status Pill + Seamless Info Touchpoint)
        val infoInteractionSource = remember { MutableInteractionSource() }

        val badgeText = when {
            !hasPolicyAccess -> "Setup"
            appState.overrideMode != null -> "Override"
            appState.isPaused -> "Paused"
            !appState.isEnabled -> "Off"
            appState.currentMode == RingerState.RING -> "Ring"
            else -> "Vibrate"
        }

        Box(
            modifier = Modifier
                .clip(SquirclePill)
                .background(if (isDark) Color(0x22FFFFFF) else Color(0x0E000000))
                .border(
                    width = 0.8.dp,
                    brush = Brush.horizontalGradient(
                        colors = if (isDark) {
                            listOf(Color(0x35FFFFFF), Color(0x10FFFFFF))
                        } else {
                            listOf(Color(0x50FFFFFF), Color(0x15000000))
                        }
                    ),
                    shape = SquirclePill
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Live status dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )

                Text(
                    text = badgeText.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.7.sp
                    ),
                    color = statusColor
                )

                // Hairline Divider
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 11.dp)
                        .background(if (isDark) Color(0x30FFFFFF) else Color(0x20000000))
                )

                // Seamless Info Icon
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About",
                    tint = colors.textPrimary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(13.dp)
                        .clickable(
                            interactionSource = infoInteractionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = onInfoClick
                        )
                )
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
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidPulsingHalo(glowColor = animatedStatusColor) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.5.sp
                        ),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = colors.textSecondary
                    )
                }
            }

            // Compact Quick Actions
            if (hasPolicyAccess) {
                if (appState.isPaused || appState.overrideMode != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppleStyleButton(
                        text = "Resume Automation",
                        onClick = onResumeAutomation,
                        style = AppleButtonStyle.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                    )
                } else if (appState.isEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

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
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.5.dp)
                            )
                        } else {
                            AppleStyleButton(
                                text = "Force Ring",
                                onClick = onForceRing,
                                style = AppleButtonStyle.ACCENT,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.5.dp)
                            )
                        }

                        AppleStyleButton(
                            text = "Pause 1h",
                            onClick = onPause1h,
                            style = AppleButtonStyle.GLASS,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2-Tile Compact Rules Grid
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
                        modifier = Modifier.size(15.dp)
                    )
                    LiquidPillBadge(text = "Ring", tintColor = colors.ringState)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Screen Locked",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    ),
                    color = colors.textPrimary
                )
                Text(
                    text = "Audible calls",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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
                        modifier = Modifier.size(15.dp)
                    )
                    LiquidPillBadge(text = "Vibrate", tintColor = colors.accentSilent)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Screen Unlocked",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    ),
                    color = colors.textPrimary
                )
                Text(
                    text = "Silent in use",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DND Access Required",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    ),
                    color = colors.error
                )
                Text(
                    text = "Needed to change ringer mode",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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
 * Info Bottom Sheet Content
 */
@Composable
private fun InfoBottomSheetContent(onClose: () -> Unit) {
    val colors = AwayAssistTheme.colors
    val isDark = colors.isDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x40FFFFFF) else Color(0x30000000))
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = colors.accentSilent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "How Away Assist Works",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCardItem(
            icon = Icons.Default.Bolt,
            tint = colors.ringState,
            title = "Deterministic Switching",
            description = "Switches to Normal Audible Ring mode when your screen locks, and restores Vibrate mode the moment you unlock it.",
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        InfoCardItem(
            icon = Icons.Default.Security,
            tint = colors.azureGlow,
            title = "Zero Polling & 100% On-Device",
            description = "Uses hardware broadcast triggers only. No background battery loops, no internet connection, completely private.",
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(18.dp))

        AppleStyleButton(
            text = "Done",
            onClick = onClose,
            style = AppleButtonStyle.PRIMARY,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp)
        )
    }
}

@Composable
private fun InfoCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    description: String,
    isDark: Boolean
) {
    val colors = AwayAssistTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(shape = SquircleMedium, tintColor = tint, isDark = isDark)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = colors.textSecondary
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
