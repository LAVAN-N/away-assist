package com.awayassist.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.awayassist.app.data.AppState
import com.awayassist.app.data.AwayAssistPreferences
import com.awayassist.app.service.NotificationHelper
import com.awayassist.app.service.RingerService
import com.awayassist.app.ui.MainScreen
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.util.RingerModeController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AwayAssistPreferences
    private lateinit var ringerController: RingerModeController

    private val postNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Notification permission is needed to show active ringer status",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = AwayAssistPreferences.getInstance(this)
        ringerController = RingerModeController(this)

        requestPostNotificationsPermissionIfNeeded()

        setContent {
            AwayAssistTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                var hasPolicyAccess by remember {
                    mutableStateOf(ringerController.isNotificationPolicyAccessGranted())
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val granted = ringerController.isNotificationPolicyAccessGranted()
                            hasPolicyAccess = granted
                            if (granted) {
                                // If enabled, make sure foreground service is running
                                val state = preferences.appStateFlow
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val appState by preferences.appStateFlow.collectAsState(initial = AppState())
                val scope = rememberCoroutineScope()

                MainScreen(
                    appState = appState,
                    hasNotificationPolicyAccess = hasPolicyAccess,
                    onToggleEnabled = { enabled ->
                        scope.launch {
                            preferences.setEnabled(enabled)
                            if (enabled) {
                                if (hasPolicyAccess) {
                                    RingerService.startService(this@MainActivity)
                                } else {
                                    openNotificationPolicyAccessSettings()
                                }
                            } else {
                                RingerService.stopService(this@MainActivity)
                            }
                        }
                    },
                    onForceRing = {
                        sendServiceAction(NotificationHelper.ACTION_FORCE_RING)
                    },
                    onForceSilent = {
                        sendServiceAction(NotificationHelper.ACTION_FORCE_SILENT)
                    },
                    onPause1h = {
                        sendServiceAction(NotificationHelper.ACTION_PAUSE_1H)
                    },
                    onResumeAutomation = {
                        sendServiceAction(NotificationHelper.ACTION_RESUME)
                    },
                    onRequestPolicyAccess = {
                        openNotificationPolicyAccessSettings()
                    }
                )
            }
        }

        // Auto-start foreground service on launch if enabled and permission granted
        startServiceIfEligible()
    }

    private fun startServiceIfEligible() {
        if (ringerController.isNotificationPolicyAccessGranted()) {
            RingerService.startService(this)
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, RingerService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun openNotificationPolicyAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Unable to open settings. Please open Android Settings -> Do Not Disturb access.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                postNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}
