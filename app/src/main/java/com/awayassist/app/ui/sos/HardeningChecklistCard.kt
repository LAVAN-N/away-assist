package com.awayassist.app.ui.sos

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awayassist.app.ui.components.AppleButtonStyle
import com.awayassist.app.ui.components.AppleStyleButton
import com.awayassist.app.ui.components.GroupedListCard
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.RingState

import android.app.Notification
import android.app.NotificationManager
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.awayassist.app.service.NotificationHelper

private fun checkIsLockScreenNotificationHidden(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false

    // 1. Check if notifications are disabled completely
    if (!nm.areNotificationsEnabled()) {
        return true
    }

    // 2. Check global lock screen notification setting
    try {
        val globalShow = Settings.Secure.getInt(
            context.contentResolver,
            "lock_screen_show_notifications",
            1
        )
        if (globalShow == 0) return true
    } catch (_: Exception) {}

    // 3. Check channel-level lock screen visibility
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = nm.getNotificationChannel(NotificationHelper.CHANNEL_ID)
        if (channel != null) {
            if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return true
            }
            if (channel.lockscreenVisibility == Notification.VISIBILITY_SECRET) {
                return true
            }
        }
    }

    return false
}

@Composable
fun HardeningChecklistCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyguardManager = remember {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }
    val sosLocateController = remember { com.awayassist.app.util.SosLocateController(context) }

    var isDeviceSecure by remember { mutableStateOf(keyguardManager?.isDeviceSecure ?: false) }
    var isLockNotificationHidden by remember { mutableStateOf(checkIsLockScreenNotificationHidden(context)) }
    var hasAdbPermission by remember { mutableStateOf(sosLocateController.hasWriteSecureSettingsPermission()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDeviceSecure = keyguardManager?.isDeviceSecure ?: false
                isLockNotificationHidden = checkIsLockScreenNotificationHidden(context)
                hasAdbPermission = sosLocateController.hasWriteSecureSettingsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showAdbGuideSheet by remember { mutableStateOf(false) }

    if (showAdbGuideSheet) {
        AdbSetupGuideSheet(onDismiss = { showAdbGuideSheet = false })
    }

    GroupedListCard(
        header = "Security Hardening Checklist",
        footer = "These advisory checks ensure a finder or thief cannot easily disable SOS Locate from the lock screen.",
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Item 1: Device Lock Screen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDeviceSecure) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isDeviceSecure) RingState else AwayAssistTheme.colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDeviceSecure) "Lock Screen Protected" else "Lock Screen Not Set",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                    Text(
                        text = if (isDeviceSecure) {
                            "Device has PIN/Pattern/Biometrics enabled."
                        } else {
                            "Set a lock screen PIN/Pattern so settings cannot be tampered with."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AwayAssistTheme.colors.textSecondary
                    )
                }
                if (!isDeviceSecure) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AppleStyleButton(
                        text = "Set Lock",
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        style = AppleButtonStyle.SECONDARY
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Item 2: Hide Lock Screen Notification Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isLockNotificationHidden) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isLockNotificationHidden) RingState else AwayAssistTheme.colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLockNotificationHidden) "Lock Screen Notification: Hidden" else "Lock Screen Notification: Visible",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                    Text(
                        text = if (isLockNotificationHidden) {
                            "Away Assist notifications are hidden on the lock screen so secrets cannot be viewed."
                        } else {
                            "Hide Away Assist notifications on the lock screen to prevent exposing status or secret responses."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AwayAssistTheme.colors.textSecondary
                    )
                }
                if (!isLockNotificationHidden) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AppleStyleButton(
                        text = "Hide on Lock",
                        onClick = {
                            try {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID)
                                    }
                                } else {
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val fallbackIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(fallbackIntent)
                                } catch (_: Exception) {}
                            }
                        },
                        style = AppleButtonStyle.SECONDARY
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Item 3: Remote Location Switching (ADB Permission)
            val sosLocateController = remember { com.awayassist.app.util.SosLocateController(context) }
            val hasAdbPermission = remember { sosLocateController.hasWriteSecureSettingsPermission() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasAdbPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasAdbPermission) RingState else AwayAssistTheme.colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasAdbPermission) "Remote Location Switching: Enabled" else "Remote Location Switching: Optional",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                    Text(
                        text = if (hasAdbPermission) {
                            "App can automatically turn ON location when emergency SMS arrives and turn it OFF after fix."
                        } else {
                            "Only needed if you keep phone location OFF. Tap 'Guide' for step-by-step setup."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AwayAssistTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AppleStyleButton(
                    text = if (hasAdbPermission) "Status" else "Guide",
                    onClick = { showAdbGuideSheet = true },
                    style = AppleButtonStyle.SECONDARY
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Item 4: Quick Settings Restriction Advisory
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = AwayAssistTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Restrict Quick Settings on Lock Screen",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                    Text(
                        text = "On OEM skins (Samsung, Xiaomi, etc.), restrict pulling down Quick Settings or toggling Airplane mode while locked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AwayAssistTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
