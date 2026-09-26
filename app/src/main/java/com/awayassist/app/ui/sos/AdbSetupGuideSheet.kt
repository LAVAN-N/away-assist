package com.awayassist.app.ui.sos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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

private const val GRANT_COMMAND = "pm grant com.awayassist.app android.permission.WRITE_SECURE_SETTINGS"
private const val ADB_GRANT_COMMAND = "adb shell pm grant com.awayassist.app android.permission.WRITE_SECURE_SETTINGS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbSetupGuideSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember { SosLocateController(context) }
    var isGranted by remember { mutableStateOf(controller.hasWriteSecureSettingsPermission()) }
    var selectedMethodTab by remember { mutableIntStateOf(0) } // 0 = Shizuku (Phone only), 1 = Computer (ADB)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val shizukuPermissionListener = remember {
        rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val ok = controller.grantWriteSecureSettingsViaShizuku()
                isGranted = ok
                if (ok) {
                    Toast.makeText(context, "Permission granted via Shizuku!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        try {
            rikka.shizuku.Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Exception) {}
        onDispose {
            try {
                rikka.shizuku.Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
            } catch (e: Exception) {}
        }
    }

    val isShizukuRunning = remember { controller.isShizukuAvailable() }
    val isShizukuAuthorized = remember(isShizukuRunning) { controller.isShizukuPermissionGranted() }

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
                    text = "Remote Location & Data Setup",
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
                            text = "Do I really need this setup?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AwayAssistTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "No setup is needed if you keep your phone's Location and Mobile Data ON! On modern Android, leaving location ON consumes 0% battery when idle.\n\nThis 1-time setup empowers Away Assist to auto-enable Location and Mobile Data via emergency SMS if they were ever turned OFF when the device is lost.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AwayAssistTheme.colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Permission Status Card
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
                                text = if (isGranted) "Permission Active" else "Permission Not Granted",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = AwayAssistTheme.colors.textPrimary
                            )
                            Text(
                                text = if (isGranted) "Remote switching is ready" else "Follow Shizuku or PC guide below",
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

            Spacer(modifier = Modifier.height(16.dp))

            // Method Selector (Shizuku vs PC ADB)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(SquircleMedium)
                    .background(AwayAssistTheme.colors.cardSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Shizuku (Phone)
                val tab0Selected = selectedMethodTab == 0
                val tab0Bg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (tab0Selected) AwayAssistTheme.colors.accent else androidx.compose.ui.graphics.Color.Transparent,
                    animationSpec = androidx.compose.animation.core.spring(),
                    label = "tab0Bg"
                )
                val tab0Fg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (tab0Selected) androidx.compose.ui.graphics.Color.White else AwayAssistTheme.colors.textSecondary,
                    animationSpec = androidx.compose.animation.core.spring(),
                    label = "tab0Fg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(SquircleMedium)
                        .background(tab0Bg)
                        .clickable { selectedMethodTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = tab0Fg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shizuku (Phone)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = tab0Fg,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Tab 1: Computer (ADB)
                val tab1Selected = selectedMethodTab == 1
                val tab1Bg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (tab1Selected) AwayAssistTheme.colors.accent else androidx.compose.ui.graphics.Color.Transparent,
                    animationSpec = androidx.compose.animation.core.spring(),
                    label = "tab1Bg"
                )
                val tab1Fg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (tab1Selected) androidx.compose.ui.graphics.Color.White else AwayAssistTheme.colors.textSecondary,
                    animationSpec = androidx.compose.animation.core.spring(),
                    label = "tab1Fg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(SquircleMedium)
                        .background(tab1Bg)
                        .clickable { selectedMethodTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Laptop,
                            contentDescription = null,
                            tint = tab1Fg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Computer (ADB)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = tab1Fg,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedMethodTab == 0) {
                // SHIZUKU STEP-BY-STEP GUIDE (100% ON PHONE)
                Text(
                    text = "Method 1: 1-Tap Setup with Shizuku (Free & No PC)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AwayAssistTheme.colors.accent
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Step 1: Install Shizuku
                GuideStepItem(
                    stepNumber = "1",
                    title = "Install Free Shizuku App",
                    description = "Download the official, free Shizuku app from the Google Play Store on this phone.",
                    actionLabel = "Open Shizuku in Play Store",
                    onAction = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(webIntent)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Step 2: Start Shizuku via Wireless Debugging
                GuideStepItem(
                    stepNumber = "2",
                    title = "Start Shizuku via Wireless Debugging",
                    description = "1. Enable 'Developer Options' & toggle 'Wireless Debugging' ON.\n2. Open Shizuku, tap 'Pairing' (enter 6-digit code in the notification).\n3. Return to Shizuku and tap 'Start'.",
                    actionLabel = "Open Developer Options",
                    onAction = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (e: Exception) {
                            Toast.makeText(context, "Open Settings > Developer Options manually", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Step 3: Direct 1-Tap Grant inside Away Assist
                GuideStepItem(
                    stepNumber = "3",
                    title = "Grant Permission in 1-Tap",
                    description = "Once Shizuku is started, tap the button below. Away Assist will directly communicate with Shizuku to grant the permission automatically—no shell or terminal app needed!",
                    actionLabel = if (!isGranted) "Grant with Shizuku" else "Permission Granted ✓",
                    onAction = {
                        if (!isGranted) {
                            if (!controller.isShizukuAvailable()) {
                                Toast.makeText(context, "Shizuku is not running. Please open Shizuku and tap 'Start'.", Toast.LENGTH_LONG).show()
                            } else if (!controller.isShizukuPermissionGranted()) {
                                try {
                                    rikka.shizuku.Shizuku.requestPermission(1001)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to request Shizuku permission", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val success = controller.grantWriteSecureSettingsViaShizuku()
                                isGranted = success
                                if (success) {
                                    Toast.makeText(context, "Successfully granted via Shizuku!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Grant failed. Please check Shizuku status.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            } else {
                // PC / ADB STEP-BY-STEP GUIDE
                Text(
                    text = "Method 2: Setup via Computer (USB / WebADB)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AwayAssistTheme.colors.accent
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Step 1: Enable Developer Options
                GuideStepItem(
                    stepNumber = "1",
                    title = "Enable Developer Options",
                    description = "Go to Settings > About Phone, and tap 'Build Number' 7 times until you see 'You are now a developer!'.",
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
                    description = "In Developer Options, turn on 'USB Debugging'.\n(Xiaomi / POCO / Redmi users: also turn on 'USB debugging (Security settings)').",
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

                // Step 3: Run command
                GuideStepItem(
                    stepNumber = "3",
                    title = "Connect & Run Command",
                    description = "Plug phone into PC/Mac via USB:\n\n• Easy: In Chrome/Edge on PC, open app.webadb.com, click 'Connect', open 'Interactive Shell', and paste command.\n\n• Terminal: Run in Command Prompt / Terminal.",
                    actionLabel = null,
                    onAction = null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Command Box for PC
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
                                text = "COMMAND FOR PC / WEBADB:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AwayAssistTheme.colors.textSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .clip(SquircleMedium)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = ClipData.newPlainText("ADB Command", ADB_GRANT_COMMAND)
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
                            text = ADB_GRANT_COMMAND,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = AwayAssistTheme.colors.textPrimary
                        )
                    }
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
