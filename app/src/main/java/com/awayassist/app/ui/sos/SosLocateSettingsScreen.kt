package com.awayassist.app.ui.sos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.data.SosLocateState
import com.awayassist.app.data.SosSessionState
import com.awayassist.app.data.computeSha256
import com.awayassist.app.ui.components.AppleButtonStyle
import com.awayassist.app.ui.components.AppleStyleButton
import com.awayassist.app.ui.components.AppleStyleSwitch
import com.awayassist.app.ui.components.GroupedListCard
import com.awayassist.app.ui.components.GroupedListRow
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.RingState
import com.awayassist.app.ui.theme.SquircleLarge
import com.awayassist.app.ui.theme.SquircleMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosLocateSettingsScreen(
    sosState: SosLocateState,
    onBack: () -> Unit,
    onToggleSosEnabled: (Boolean) -> Unit,
    onUpdateEmergencyNumber: (String) -> Unit,
    onUpdatePrefix: (String) -> Unit,
    onDismissRotationReminder: () -> Unit,
    onUpdateTriggers: (sim: Boolean, shutdown: Boolean, sms: Boolean, boot: Boolean) -> Unit,
    onUpdateTimeoutHours: (Int) -> Unit,
    onUpdateTraceInterval: (Int) -> Unit,
    onStopActiveSession: () -> Unit
) {
    var showEditNumberDialog by remember { mutableStateOf(false) }
    var showEditPrefixDialog by remember { mutableStateOf(false) }
    var tempEmergencyNumber by remember(sosState.emergencyAlertNumber) {
        mutableStateOf(sosState.emergencyAlertNumber)
    }
    var tempPrefix by remember(sosState.commandPrefix) {
        mutableStateOf(sosState.commandPrefix)
    }

    BackHandler(enabled = true) {
        if (showEditNumberDialog) {
            showEditNumberDialog = false
        } else if (showEditPrefixDialog) {
            showEditPrefixDialog = false
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SOS Locate Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AwayAssistTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AwayAssistTheme.colors.background
                )
            )
        },
        containerColor = AwayAssistTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Master SOS Toggle
            GroupedListCard(
                header = "SOS Locate Protocol"
            ) {
                GroupedListRow(
                    title = "Enable SOS Locate",
                    subtitle = "Automated remote location on emergency triggers",
                    trailingContent = {
                        AppleStyleSwitch(
                            checked = sosState.isSosEnabled,
                            onCheckedChange = onToggleSosEnabled
                        )
                    }
                )
            }

            // Foldable content when SOS Locate is Enabled
            AnimatedVisibility(
                visible = sosState.isSosEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Prefix Rotation Appeal / Reminder Banner
                    if (sosState.prefixRotationNeeded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SquircleLarge)
                                .background(AwayAssistTheme.colors.accent.copy(alpha = 0.12f))
                                .border(1.dp, AwayAssistTheme.colors.accent.copy(alpha = 0.35f), SquircleLarge)
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AwayAssistTheme.colors.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Security Notice: Passkey Used",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AwayAssistTheme.colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "An emergency SMS command was recently authenticated. For continued safety after recovery, rotate your secret passkey prefix.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AwayAssistTheme.colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppleStyleButton(
                                        text = "Rotate Prefix",
                                        onClick = {
                                            tempPrefix = ""
                                            showEditPrefixDialog = true
                                        },
                                        style = AppleButtonStyle.PRIMARY,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AppleStyleButton(
                                        text = "Dismiss",
                                        onClick = onDismissRotationReminder,
                                        style = AppleButtonStyle.SECONDARY,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Active Tracking Session Status Card
                    if (sosState.isSessionActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SquircleLarge)
                                .background(RingState.copy(alpha = 0.15f))
                                .border(1.dp, RingState.copy(alpha = 0.4f), SquircleLarge)
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = RingState,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Active ${sosState.sessionState.name} Session",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AwayAssistTheme.colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Replying to ${sosState.activeTargetNumber}. Auto-timeout in ${sosState.autoTimeoutHours}h.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AwayAssistTheme.colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                AppleStyleButton(
                                    text = "Stop Active Session",
                                    onClick = onStopActiveSession,
                                    style = AppleButtonStyle.SECONDARY,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Separate Heading next to SOS Locate Protocol: Remote Location Switching
                    RemoteLocationSwitchingCard()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Configuration Group
                    GroupedListCard(
                        header = "Configuration"
                    ) {
                        GroupedListRow(
                            title = "Emergency Contact Number",
                            subtitle = if (sosState.emergencyAlertNumber.isNotBlank()) sosState.emergencyAlertNumber else "Not set",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = AwayAssistTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                tempEmergencyNumber = sosState.emergencyAlertNumber
                                showEditNumberDialog = true
                            },
                            showDivider = true
                        )

                        GroupedListRow(
                            title = "Secret Command Prefix",
                            subtitle = if (sosState.commandPrefix.isNotBlank()) {
                                "${sosState.commandPrefix} (${sosState.prefixSha256.take(12)}...)"
                            } else "Not configured",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = AwayAssistTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                tempPrefix = sosState.commandPrefix
                                showEditPrefixDialog = true
                            },
                            showDivider = true
                        )

                        GroupedListRow(
                            title = "Auto-Timeout Safety Net",
                            subtitle = "${sosState.autoTimeoutHours} hours maximum duration",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                val nextHours = when (sosState.autoTimeoutHours) {
                                    1 -> 2
                                    2 -> 4
                                    4 -> 6
                                    6 -> 12
                                    12 -> 24
                                    else -> 1
                                }
                                onUpdateTimeoutHours(nextHours)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trigger Toggles
                    GroupedListCard(
                        header = "Emergency Triggers"
                    ) {
                        GroupedListRow(
                            title = "SIM Card Removed",
                            subtitle = "Alert emergency number when SIM is pulled",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SimCard,
                                    contentDescription = null,
                                    tint = AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingContent = {
                                AppleStyleSwitch(
                                    checked = sosState.triggerSimRemoved,
                                    onCheckedChange = { checked ->
                                        onUpdateTriggers(checked, sosState.triggerShutdown, sosState.triggerSmsCommands, sosState.triggerBoot)
                                    }
                                )
                            },
                            showDivider = true
                        )

                        GroupedListRow(
                            title = "Phone Switched Off",
                            subtitle = "Best-effort location alert during shutdown",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingContent = {
                                AppleStyleSwitch(
                                    checked = sosState.triggerShutdown,
                                    onCheckedChange = { checked ->
                                        onUpdateTriggers(sosState.triggerSimRemoved, checked, sosState.triggerSmsCommands, sosState.triggerBoot)
                                    }
                                )
                            },
                            showDivider = true
                        )

                        GroupedListRow(
                            title = "Remote SMS Commands",
                            subtitle = "Respond to FIND, TRACK, TRACE, STOP from any phone",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CellTower,
                                    contentDescription = null,
                                    tint = AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingContent = {
                                AppleStyleSwitch(
                                    checked = sosState.triggerSmsCommands,
                                    onCheckedChange = { checked ->
                                        onUpdateTriggers(sosState.triggerSimRemoved, sosState.triggerShutdown, checked, sosState.triggerBoot)
                                    }
                                )
                            },
                            showDivider = true
                        )

                        GroupedListRow(
                            title = "Device Restart Alert",
                            subtitle = "Notify emergency contact upon boot",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingContent = {
                                AppleStyleSwitch(
                                    checked = sosState.triggerBoot,
                                    onCheckedChange = { checked ->
                                        onUpdateTriggers(sosState.triggerSimRemoved, sosState.triggerShutdown, sosState.triggerSmsCommands, checked)
                                    }
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (!sosState.isSosEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Hardening Checklist Card (Always visible)
            HardeningChecklistCard()

            // Last Triggered Summary (Always visible when present)
            if (sosState.lastTriggeredTimestamp > 0L) {
                Spacer(modifier = Modifier.height(16.dp))
                val timeStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(sosState.lastTriggeredTimestamp))
                Text(
                    text = "Last Triggered: ${sosState.lastTriggerDesc} at $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = AwayAssistTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog: Edit Emergency Number
    if (showEditNumberDialog) {
        AlertDialog(
            onDismissRequest = { showEditNumberDialog = false },
            title = {
                Text(
                    text = "Emergency Contact Number",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AwayAssistTheme.colors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = tempEmergencyNumber,
                    onValueChange = { tempEmergencyNumber = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1234567890") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AwayAssistTheme.colors.accent,
                        unfocusedBorderColor = AwayAssistTheme.colors.textSecondary.copy(alpha = 0.4f),
                        focusedTextColor = AwayAssistTheme.colors.textPrimary,
                        unfocusedTextColor = AwayAssistTheme.colors.textPrimary
                    ),
                    shape = SquircleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateEmergencyNumber(tempEmergencyNumber.trim())
                        showEditNumberDialog = false
                    }
                ) {
                    Text("Save", color = AwayAssistTheme.colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNumberDialog = false }) {
                    Text("Cancel", color = AwayAssistTheme.colors.textSecondary)
                }
            },
            containerColor = AwayAssistTheme.colors.cardSurface
        )
    }

    // Dialog: Edit Command Prefix
    if (showEditPrefixDialog) {
        val previewHash = remember(tempPrefix) { computeSha256(tempPrefix.trim()) }
        AlertDialog(
            onDismissRequest = { showEditPrefixDialog = false },
            title = {
                Text(
                    text = "Rotate Command Prefix",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AwayAssistTheme.colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a new secret passkey prefix. Live SHA-256 checksum will be stored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AwayAssistTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempPrefix,
                        onValueChange = { tempPrefix = it },
                        label = { Text("New Prefix") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AwayAssistTheme.colors.accent,
                            unfocusedBorderColor = AwayAssistTheme.colors.textSecondary.copy(alpha = 0.4f),
                            focusedTextColor = AwayAssistTheme.colors.textPrimary,
                            unfocusedTextColor = AwayAssistTheme.colors.textPrimary
                        ),
                        shape = SquircleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SHA-256: ${if (previewHash.isNotBlank()) previewHash.take(24) + "..." else "None"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = AwayAssistTheme.colors.accent
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempPrefix.isNotBlank()) {
                            onUpdatePrefix(tempPrefix.trim())
                        }
                        showEditPrefixDialog = false
                    }
                ) {
                    Text("Save", color = AwayAssistTheme.colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPrefixDialog = false }) {
                    Text("Cancel", color = AwayAssistTheme.colors.textSecondary)
                }
            },
            containerColor = AwayAssistTheme.colors.cardSurface
        )
    }
}
