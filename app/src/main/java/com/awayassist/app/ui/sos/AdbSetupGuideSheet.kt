package com.awayassist.app.ui.sos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awayassist.app.ui.components.AppleButtonStyle
import com.awayassist.app.ui.components.AppleStyleButton
import com.awayassist.app.ui.components.GroupedListCard
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.RingState
import com.awayassist.app.ui.theme.SquircleLarge
import com.awayassist.app.ui.theme.SquircleMedium
import com.awayassist.app.util.SosLocateController

private const val ADB_COMMAND = "adb shell pm grant com.awayassist.app android.permission.WRITE_SECURE_SETTINGS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbSetupGuideSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember { SosLocateController(context) }
    var isGranted by remember { mutableStateOf(controller.hasWriteSecureSettingsPermission()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AwayAssistTheme.colors.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remote Location Switching Guide",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AwayAssistTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AwayAssistTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Non-tech user callout note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SquircleLarge)
                    .background(AwayAssistTheme.colors.accent.copy(alpha = 0.10f))
                    .border(1.dp, AwayAssistTheme.colors.accent.copy(alpha = 0.30f), SquircleLarge)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AwayAssistTheme.colors.accent,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Do I really need this?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AwayAssistTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "No computer needed if you keep your phone's Location toggle ON! On modern Android, leaving location ON consumes 0% battery when idle.\n\nThis 1-time setup is only needed if you prefer keeping your phone's location switch OFF manually at all times.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AwayAssistTheme.colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SquircleLarge)
                    .background(if (isGranted) RingState.copy(alpha = 0.12f) else AwayAssistTheme.colors.cardSurface)
                    .border(
                        1.dp,
                        if (isGranted) RingState.copy(alpha = 0.4f) else AwayAssistTheme.colors.cardSurface,
                        SquircleLarge
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isGranted) RingState else AwayAssistTheme.colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isGranted) "Permission Active" else "Permission Not Yet Granted",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = AwayAssistTheme.colors.textPrimary
                            )
                            Text(
                                text = if (isGranted) "Remote switching is ready" else "Follow steps below to enable",
                                style = MaterialTheme.typography.bodySmall,
                                color = AwayAssistTheme.colors.textSecondary
                            )
                        }
                    }
                    AppleStyleButton(
                        text = "Check",
                        onClick = {
                            isGranted = controller.hasWriteSecureSettingsPermission()
                            if (isGranted) {
                                Toast.makeText(context, "Permission active!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Not yet detected", Toast.LENGTH_SHORT).show()
                            }
                        },
                        style = AppleButtonStyle.SECONDARY
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 1: Enable Developer Options
            GuideStepItem(
                stepNumber = "1",
                title = "Enable Developer Options",
                description = "Go to Settings > About Phone, and tap 'Build Number' 7 times until you see the message 'You are now a developer!'",
                actionLabel = "Open About Phone",
                onAction = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (e: Exception) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (e2: Exception) {
                            // Fallback
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 2: Turn on USB Debugging
            GuideStepItem(
                stepNumber = "2",
                title = "Turn on USB Debugging",
                description = "In Developer Options, turn on 'USB Debugging'.\n(Xiaomi / Redmi / MIUI users: also turn on 'USB debugging (Security settings)')",
                actionLabel = "Open Developer Options",
                onAction = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (e: Exception) {
                        Toast.makeText(context, "Please open Settings > Developer Options manually", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 3: Run the 1-Time Command
            GuideStepItem(
                stepNumber = "3",
                title = "Run 1-Time Command",
                description = "Connect phone to PC/Mac via USB:\n\n• Easy (No install): In Chrome or Edge on PC, open app.webadb.com, click 'Connect', allow on phone, open 'Interactive Shell', and paste command.\n\n• Advanced: Run via ADB Terminal in PowerShell / Mac Terminal.",
                actionLabel = null,
                onAction = null
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Command Box with 1-tap copy
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SquircleMedium)
                    .background(AwayAssistTheme.colors.cardSurface)
                    .border(1.dp, AwayAssistTheme.colors.textSecondary.copy(alpha = 0.2f), SquircleMedium)
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMMAND TO RUN:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AwayAssistTheme.colors.textSecondary
                        )
                        Row(
                            modifier = Modifier
                                .clip(SquircleMedium)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("ADB Command", ADB_COMMAND)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = AwayAssistTheme.colors.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AwayAssistTheme.colors.accent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ADB_COMMAND,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AppleStyleButton(
                text = "Done",
                onClick = onDismiss,
                style = AppleButtonStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GuideStepItem(
    stepNumber: String,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(AwayAssistTheme.colors.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = AwayAssistTheme.colors.background
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = AwayAssistTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AwayAssistTheme.colors.textSecondary,
                lineHeight = 18.sp
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AppleStyleButton(
                    text = actionLabel,
                    onClick = onAction,
                    style = AppleButtonStyle.SECONDARY
                )
            }
        }
    }
}
