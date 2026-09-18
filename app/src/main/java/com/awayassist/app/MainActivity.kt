package com.awayassist.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.awayassist.app.data.ThemeMode
import com.awayassist.app.service.NotificationHelper
import com.awayassist.app.service.RingerService
import com.awayassist.app.ui.MainScreen
import com.awayassist.app.ui.components.OperationMode
import com.awayassist.app.ui.theme.AwayAssistTheme
import com.awayassist.app.util.RingerModeController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AwayAssistPreferences
    private lateinit var ringerController: RingerModeController

    private val postNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startServiceIfEligible()
            } else {
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
            val appState by preferences.appStateFlow.collectAsState(initial = AppState())
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (appState.themeMode) {
                ThemeMode.SYSTEM -> systemInDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AwayAssistTheme(darkTheme = isDarkTheme) {
                val lifecycleOwner = LocalLifecycleOwner.current
                var hasPolicyAccess by remember {
                    mutableStateOf(ringerController.isNotificationPolicyAccessGranted())
                }
                var isBatteryOptIgnored by remember {
                    mutableStateOf(isBatteryOptimizationIgnored())
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasPolicyAccess = ringerController.isNotificationPolicyAccessGranted()
                            isBatteryOptIgnored = isBatteryOptimizationIgnored()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val scope = rememberCoroutineScope()

                MainScreen(
                    appState = appState,
                    hasNotificationPolicyAccess = hasPolicyAccess,
                    isBatteryOptimizationIgnored = isBatteryOptIgnored,
                    onSelectMode = { mode ->
                        when (mode) {
                            OperationMode.AUTO -> {
                                scope.launch {
                                    preferences.setEnabled(true)
                                    if (hasPolicyAccess) {
                                        RingerService.startService(this@MainActivity)
                                    } else {
                                        openNotificationPolicyAccessSettings()
                                    }
                                }
                                sendServiceAction(NotificationHelper.ACTION_RESUME)
                            }
                            OperationMode.FORCE_RING -> {
                                sendServiceAction(NotificationHelper.ACTION_FORCE_RING)
                            }
                            OperationMode.PAUSE -> {
                                scope.launch {
                                    preferences.setCustomPauseDuration(appState.customPauseDurationMs)
                                }
                                RingerService.pauseService(this@MainActivity, appState.customPauseDurationMs)
                            }
                        }
                    },
                    onPauseForDuration = { durationMs ->
                        scope.launch {
                            preferences.setCustomPauseDuration(durationMs)
                        }
                        RingerService.pauseService(this@MainActivity, durationMs)
                    },
                    onRequestPolicyAccess = {
                        openNotificationPolicyAccessSettings()
                    },
                    onRequestBatteryOptimization = {
                        openBatteryOptimizationSettings()
                    },
                    onRequestAutostart = {
                        openAutostartSettings()
                    },
                    onSelectTheme = { mode ->
                        scope.launch {
                            preferences.setThemeMode(mode)
                        }
                    }
                )
            }
        }

        // Auto-start foreground service on launch if enabled and permission granted
        startServiceIfEligible()
    }

    override fun onResume() {
        super.onResume()
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

    private fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
        }
        return true
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

    private fun openBatteryOptimizationSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } else {
                openAppDetailsSettings()
            }
        } catch (e: Exception) {
            openAppDetailsSettings()
        }
    }

    private fun openAutostartSettings() {
        val autostartIntents = listOf(
            // Xiaomi / MIUI / HyperOS
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")),
            // Oppo / ColorOS / Realme
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            // Vivo / FuntouchOS / iQOO
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            // Huawei / Honor EMUI
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            // Samsung
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            // OnePlus
            Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"))
        )

        var launched = false
        for (intent in autostartIntents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    launched = true
                    break
                }
            } catch (_: Exception) {
            }
        }

        if (!launched) {
            openAppDetailsSettings()
        }
    }

    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Please open Android Settings -> Apps -> Away Assist", Toast.LENGTH_LONG).show()
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
