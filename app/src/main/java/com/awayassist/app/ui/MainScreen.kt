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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    appState: AppState,
    hasNotificationPolicyAccess: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleForceRing: (Boolean) -> Unit,
    onTogglePause: (Boolean, Long) -> Unit,
    onPauseForDuration: (Long) -> Unit,
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
                onResumeAutomation = onResumeAutomation,
                isDark = isDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 2-Tile Rules Grid (Screen Locked vs Screen Unlocked)
            CompactRulesGrid(
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Controls Card with Toggles & Inline Expandable Custom Pause
            ControlsCard(
                appState = appState,
                hasPolicyAccess = hasNotificationPolicyAccess,
                onToggleEnabled = onToggleEnabled,
                onToggleForceRing = onToggleForceRing,
                onTogglePause = onTogglePause,
                onPauseForDuration = onPauseForDuration,
                onSelectTheme = onSelectTheme,
                isDark = isDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // System Status Card (DND Permission)
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
                text = "Dynamic Lock & Unlock Automation",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp
                ),
                color = colors.textSecondary
            )
        }

        // Dynamic Glass Island Capsule
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

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 11.dp)
                        .background(if (isDark) Color(0x30FFFFFF) else Color(0x20000000))
                )

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
 * Compact Hero Status Card
 */
@Composable
private fun CompactStatusCard(
    appState: AppState,
    hasPolicyAccess: Boolean,
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
                    "Continuous audible ring • Tap Resume or turn off toggle",
                    Icons.Default.NotificationsActive
                )
                RingerState.VIBRATE, RingerState.SILENT -> Quad(
                    colors.accentSilent,
                    "Force Silent Active",
                    "Continuous silent vibration • Tap Resume to reactivate",
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
                "Paused (${remainingMins}m remaining)",
                "Automation suspended • Resumes automatically",
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

            // Quick Resume Button when Override or Pause is active
            if (hasPolicyAccess && (appState.isPaused || appState.overrideMode != null)) {
                Spacer(modifier = Modifier.height(12.dp))
                AppleStyleButton(
                    text = "Resume Automation",
                    onClick = onResumeAutomation,
                    style = AppleButtonStyle.PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.5.dp)
                )
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

enum class PausePreset(val label: String, val minutes: Int) {
    M10("10m", 10),
    M30("30m", 30),
    H1("1h", 60),
    CUSTOM("Custom", -1)
}

/**
 * Controls Card containing:
 * 1. Automation Toggle
 * 2. Force Ring Toggle
 * 3. Pause Automation Toggle + Collapsible Inline Custom Duration Picker
 * 4. Appearance Selector
 */
@Composable
private fun ControlsCard(
    appState: AppState,
    hasPolicyAccess: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleForceRing: (Boolean) -> Unit,
    onTogglePause: (Boolean, Long) -> Unit,
    onPauseForDuration: (Long) -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AwayAssistTheme.colors

    var selectedPreset by remember { mutableStateOf(PausePreset.H1) }
    var customMinutes by remember { mutableIntStateOf(45) }

    val activeMinutes = if (selectedPreset == PausePreset.CUSTOM) customMinutes else selectedPreset.minutes
    val currentDurationMs = activeMinutes * 60_000L

    val isForceRingActive = appState.overrideMode == RingerState.RING

    GroupedListCard(modifier = modifier) {
        // Row 1: Automation Toggle
        GroupedListRow(
            title = "Automation",
            subtitle = if (appState.isEnabled) "Automating on lock / unlock" else "Off",
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

        // Row 2: Force Ring Toggle
        GroupedListRow(
            title = "Force Ring",
            subtitle = if (isForceRingActive) "Always audible • Lock automation suspended" else "Keep ringer audible always",
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isForceRingActive) colors.ringState.copy(alpha = 0.15f) else colors.textSecondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (isForceRingActive) colors.ringState else colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            },
            trailingContent = {
                AppleStyleSwitch(
                    checked = isForceRingActive,
                    onCheckedChange = onToggleForceRing,
                    activeColor = colors.ringState
                )
            },
            showDivider = true
        )

        // Row 3: Pause Automation Toggle
        val remainingMins = if (appState.isPaused) {
            ((appState.pauseUntilTimestamp - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
        } else 0L

        GroupedListRow(
            title = "Pause",
            subtitle = if (appState.isPaused) "Suspended for ${remainingMins}m" else "Temporarily pause automation",
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (appState.isPaused) colors.warning.copy(alpha = 0.18f) else colors.textSecondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (appState.isPaused) colors.warning else colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            },
            trailingContent = {
                AppleStyleSwitch(
                    checked = appState.isPaused,
                    onCheckedChange = { checked ->
                        onTogglePause(checked, currentDurationMs)
                    },
                    activeColor = colors.warning
                )
            },
            showDivider = !appState.isPaused
        )

        // Animated Inline Custom Duration Section (Appears ONLY on Pause)
        AnimatedVisibility(
            visible = appState.isPaused,
            enter = fadeIn(spring()) + expandVertically(spring()),
            exit = fadeOut(spring()) + shrinkVertically(spring())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.warning.copy(alpha = if (isDark) 0.08f else 0.05f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pause Duration",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = colors.warning
                        )
                    }

                    val expiryTime = remember(appState.pauseUntilTimestamp) {
                        if (appState.pauseUntilTimestamp > 0L) {
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(appState.pauseUntilTimestamp))
                        } else ""
                    }
                    if (expiryTime.isNotEmpty()) {
                        Text(
                            text = "Resumes at $expiryTime",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Preset Chips Row (10m, 30m, 1h, Custom)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SquircleMedium)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PausePreset.values().forEach { preset ->
                        val isSelected = selectedPreset == preset
                        val itemBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color(0x50FFFFFF) else Color.White
                            } else Color.Transparent,
                            animationSpec = spring(),
                            label = "pauseChipBg"
                        )
                        val itemText by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color.White else colors.warning
                            } else colors.textSecondary,
                            animationSpec = spring(),
                            label = "pauseChipText"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(SquircleMedium)
                                .background(itemBg)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 0.8.dp,
                                            color = if (isDark) Color(0x60FFFFFF) else Color(0x25000000),
                                            shape = SquircleMedium
                                        )
                                    } else Modifier
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        selectedPreset = preset
                                        val newDuration = if (preset == PausePreset.CUSTOM) {
                                            customMinutes * 60_000L
                                        } else {
                                            preset.minutes * 60_000L
                                        }
                                        onPauseForDuration(newDuration)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                color = itemText
                            )
                        }
                    }
                }

                // Custom Duration Slider (When "Custom" chip is selected)
                if (selectedPreset == PausePreset.CUSTOM) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(shape = SquircleMedium, tintColor = colors.warning, isDark = isDark)
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Slider",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (customMinutes >= 60) {
                                        val hrs = customMinutes / 60
                                        val mins = customMinutes % 60
                                        if (mins == 0) "${hrs}h" else "${hrs}h ${mins}m"
                                    } else {
                                        "${customMinutes}m"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colors.warning
                                    )
                                )
                            }

                            Slider(
                                value = customMinutes.toFloat(),
                                onValueChange = {
                                    customMinutes = (it.toInt() / 5) * 5
                                    onPauseForDuration(customMinutes * 60_000L)
                                },
                                valueRange = 5f..480f,
                                steps = 94,
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.warning,
                                    activeTrackColor = colors.warning,
                                    inactiveTrackColor = if (isDark) Color(0x30FFFFFF) else Color(0x20000000)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("5 min", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.textSecondary)
                                Text("8 hrs", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.textSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Row 4: Theme Mode Segment Row
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
