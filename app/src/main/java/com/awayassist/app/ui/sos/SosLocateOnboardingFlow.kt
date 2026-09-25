package com.awayassist.app.ui.sos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.awayassist.app.data.computeSha256
import com.awayassist.app.ui.components.AppleButtonStyle
import com.awayassist.app.ui.components.AppleStyleButton
import com.awayassist.app.ui.components.GroupedListCard
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.ui.theme.RingState
import com.awayassist.app.ui.theme.SquircleMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosLocateOnboardingFlow(
    onDismiss: () -> Unit,
    onComplete: (emergencyNumber: String, prefix: String) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }
    var emergencyNumber by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("AWAYASSIST") }

    var hasSmsPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasLocationPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasPhoneStatePerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasSmsPerm = perms[Manifest.permission.RECEIVE_SMS] == true || perms[Manifest.permission.SEND_SMS] == true
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPerm = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPhoneStatePerm = isGranted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SOS Locate Setup (${currentStep}/3)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) {
                            currentStep -= 1
                        } else {
                            onDismiss()
                        }
                    }) {
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            when (currentStep) {
                1 -> {
                    // Step 1: Emergency Alert Phone Number
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = AwayAssistTheme.colors.accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Emergency Alert Number",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Passive security alerts (SIM removal, shutdown, reboot) will be sent as an SMS to this trusted contact number.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AwayAssistTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = emergencyNumber,
                        onValueChange = { emergencyNumber = it },
                        label = { Text("Emergency Phone Number") },
                        placeholder = { Text("+1234567890") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AwayAssistTheme.colors.accent,
                            unfocusedBorderColor = AwayAssistTheme.colors.textSecondary.copy(alpha = 0.4f),
                            focusedTextColor = AwayAssistTheme.colors.textPrimary,
                            unfocusedTextColor = AwayAssistTheme.colors.textPrimary
                        ),
                        shape = SquircleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    AppleStyleButton(
                        text = "Next: Command Prefix",
                        onClick = { currentStep = 2 },
                        style = AppleButtonStyle.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    // Step 2: Secret Command Prefix with live SHA-256 checksum preview
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = AwayAssistTheme.colors.accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Command Passkey Prefix",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Incoming SMS commands from any phone are authenticated against a fast SHA-256 checksum. Choose a unique secret phrase.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AwayAssistTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("Secret Command Prefix") },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    val sha256Preview = remember(prefix) { computeSha256(prefix.trim()) }
                    GroupedListCard(header = "Live Checksum & Commands") {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "SHA-256 Digest:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AwayAssistTheme.colors.textSecondary
                            )
                            Text(
                                text = if (sha256Preview.isNotBlank()) sha256Preview.take(24) + "..." else "None",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = AwayAssistTheme.colors.accent
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Available Commands:\n" +
                                        "• ${prefix.trim()} FIND (single fix reply)\n" +
                                        "• ${prefix.trim()} TRACK (location on)\n" +
                                        "• ${prefix.trim()} TRACE (periodic updates)\n" +
                                        "• ${prefix.trim()} STOP (disable tracking)",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = AwayAssistTheme.colors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    AppleStyleButton(
                        text = "Next: Permissions",
                        onClick = { currentStep = 3 },
                        style = AppleButtonStyle.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                3 -> {
                    // Step 3: Sequential Permissions
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AwayAssistTheme.colors.accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Grant Necessary Permissions",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AwayAssistTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Away Assist is 100% on-device with zero internet permission. These permissions are strictly used during triggers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AwayAssistTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // SMS Permission Item
                    GroupedListCard {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasSmsPerm) Icons.Default.CheckCircle else Icons.Default.CellTower,
                                    contentDescription = null,
                                    tint = if (hasSmsPerm) RingState else AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SMS Receive & Send",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AwayAssistTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "To listen for secret commands and send emergency SMS responses.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AwayAssistTheme.colors.textSecondary
                                    )
                                }
                                if (!hasSmsPerm) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AppleStyleButton(
                                        text = "Grant",
                                        onClick = {
                                            smsPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.RECEIVE_SMS,
                                                    Manifest.permission.SEND_SMS
                                                )
                                            )
                                        },
                                        style = AppleButtonStyle.SECONDARY
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Location Permission Item
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasLocationPerm) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (hasLocationPerm) RingState else AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Location (Fine / Coarse)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AwayAssistTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Acquired momentarily only when an authorized trigger is received.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AwayAssistTheme.colors.textSecondary
                                    )
                                }
                                if (!hasLocationPerm) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AppleStyleButton(
                                        text = "Grant",
                                        onClick = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        style = AppleButtonStyle.SECONDARY
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Phone State Item
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasPhoneStatePerm) Icons.Default.CheckCircle else Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (hasPhoneStatePerm) RingState else AwayAssistTheme.colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Phone State",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AwayAssistTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Strictly used to detect SIM card removal.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AwayAssistTheme.colors.textSecondary
                                    )
                                }
                                if (!hasPhoneStatePerm) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AppleStyleButton(
                                        text = "Grant",
                                        onClick = {
                                            phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                                        },
                                        style = AppleButtonStyle.SECONDARY
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    AppleStyleButton(
                        text = "Complete & Enable SOS Locate",
                        onClick = {
                            onComplete(emergencyNumber, prefix)
                        },
                        style = AppleButtonStyle.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
