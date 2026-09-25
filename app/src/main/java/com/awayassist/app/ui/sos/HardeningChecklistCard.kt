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
import androidx.compose.runtime.remember
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

@Composable
fun HardeningChecklistCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyguardManager = remember {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }
    val isDeviceSecure = remember(keyguardManager) {
        keyguardManager?.isDeviceSecure ?: false
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

            // Item 2: Hide Notification Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = AwayAssistTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide Lock Screen SMS Previews",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                    Text(
                        text = "Hiding sensitive notifications prevents someone from seeing secret command responses on the lock screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AwayAssistTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AppleStyleButton(
                    text = "Configure",
                    onClick = {
                        try {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            } else {
                                Intent(Settings.ACTION_SETTINGS)
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    style = AppleButtonStyle.SECONDARY
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Item 3: Quick Settings Restriction Advisory
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
