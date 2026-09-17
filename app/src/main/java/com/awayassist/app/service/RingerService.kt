package com.awayassist.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.awayassist.app.data.AwayAssistPreferences
import com.awayassist.app.data.RingerState
import com.awayassist.app.util.RingerModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RingerService : Service() {

    companion object {
        private const val TAG = "RingerService"

        fun startService(context: Context) {
            val intent = Intent(context, RingerService::class.java).apply {
                action = NotificationHelper.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RingerService::class.java).apply {
                action = NotificationHelper.ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObservationJob: Job? = null

    private lateinit var preferences: AwayAssistPreferences
    private lateinit var ringerController: RingerModeController
    private lateinit var notificationHelper: NotificationHelper
    private var screenStateReceiver: ScreenStateReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RingerService onCreate")

        preferences = AwayAssistPreferences.getInstance(this)
        ringerController = RingerModeController(this)
        notificationHelper = NotificationHelper(this)

        registerScreenReceiver()
        startForegroundNotification()
        observeAppState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: NotificationHelper.ACTION_START
        Log.d(TAG, "onStartCommand received action: $action")

        when (action) {
            NotificationHelper.ACTION_START -> {
                syncInitialState()
            }
            NotificationHelper.ACTION_STOP -> {
                Log.d(TAG, "Stopping foreground service via ACTION_STOP")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            NotificationHelper.ACTION_FORCE_RING -> {
                handleForceRing()
            }
            NotificationHelper.ACTION_FORCE_SILENT -> {
                handleForceSilent()
            }
            NotificationHelper.ACTION_PAUSE_1H -> {
                handlePause(3600_000L)
            }
            NotificationHelper.ACTION_RESUME -> {
                handleResume()
            }
        }

        return START_STICKY
    }

    private fun registerScreenReceiver() {
        if (screenStateReceiver == null) {
            screenStateReceiver = ScreenStateReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)
            Log.d(TAG, "Registered ScreenStateReceiver dynamically")
        }
    }

    private fun unregisterScreenReceiver() {
        screenStateReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d(TAG, "Unregistered ScreenStateReceiver")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering ScreenStateReceiver", e)
            }
            screenStateReceiver = null
        }
    }

    private fun startForegroundNotification() {
        notificationHelper.createNotificationChannel()
        serviceScope.launch {
            val state = preferences.getAppState()
            val notification = notificationHelper.buildNotification(state)
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(
                this@RingerService,
                NotificationHelper.NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
            Log.d(TAG, "Started foreground notification")
        }
    }

    private fun observeAppState() {
        stateObservationJob?.cancel()
        stateObservationJob = serviceScope.launch {
            preferences.appStateFlow.collectLatest { state ->
                notificationHelper.updateNotification(state)
            }
        }
    }

    private fun syncInitialState() {
        serviceScope.launch {
            val state = preferences.getAppState()
            if (!state.isEnabled || state.isPaused || state.overrideMode != null) {
                return@launch
            }

            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isInteractive = powerManager?.isInteractive ?: true
            if (!isInteractive) {
                Log.d(TAG, "Initial sync: screen is off -> set RING")
                ringerController.setRingMode()
                preferences.updateRingerState(RingerState.RING)
            } else {
                Log.d(TAG, "Initial sync: screen is on -> set VIBRATE")
                ringerController.setVibrateMode()
                preferences.updateRingerState(RingerState.VIBRATE)
            }
        }
    }

    private fun handleForceRing() {
        serviceScope.launch {
            Log.d(TAG, "Handling Force Ring override: setting ringer to RING and disabling automation toggle")
            ringerController.setRingMode()
            preferences.forceRing()
        }
    }

    private fun handleForceSilent() {
        serviceScope.launch {
            Log.d(TAG, "Handling Force Silent override: setting ringer to VIBRATE and disabling automation toggle")
            ringerController.setVibrateMode()
            preferences.forceSilent()
        }
    }

    private fun handlePause(durationMs: Long) {
        serviceScope.launch {
            Log.d(TAG, "Handling Pause for $durationMs ms")
            preferences.pauseForDuration(durationMs)
        }
    }

    private fun handleResume() {
        serviceScope.launch {
            Log.d(TAG, "Handling Resume automation")
            preferences.clearPause()
            syncInitialState()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "RingerService onDestroy")
        unregisterScreenReceiver()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
