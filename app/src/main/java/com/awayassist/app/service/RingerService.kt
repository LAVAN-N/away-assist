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
import com.awayassist.app.data.AppState
import com.awayassist.app.data.AwayAssistPreferences
import com.awayassist.app.data.RingerState
import com.awayassist.app.util.RingerModeController
import com.awayassist.app.util.SosLocateController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
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

        fun pauseService(context: Context, durationMs: Long) {
            val intent = Intent(context, RingerService::class.java).apply {
                action = NotificationHelper.ACTION_PAUSE
                putExtra(NotificationHelper.EXTRA_PAUSE_DURATION_MS, durationMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObservationJob: Job? = null
    private var sosSessionJob: Job? = null

    private lateinit var preferences: AwayAssistPreferences
    private lateinit var ringerController: RingerModeController
    private lateinit var sosLocateController: SosLocateController
    private lateinit var notificationHelper: NotificationHelper
    private var screenStateReceiver: ScreenStateReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RingerService onCreate")

        preferences = AwayAssistPreferences.getInstance(this)
        ringerController = RingerModeController(this)
        sosLocateController = SosLocateController(this)
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
                serviceScope.launch {
                    val state = preferences.getAppState()
                    notificationHelper.updateNotification(state)
                }
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
            NotificationHelper.ACTION_PAUSE -> {
                val durationMs = intent?.getLongExtra(NotificationHelper.EXTRA_PAUSE_DURATION_MS, 600_000L) ?: 600_000L
                handlePause(durationMs)
            }
            NotificationHelper.ACTION_PAUSE_10M -> {
                val durationMs = intent?.getLongExtra(NotificationHelper.EXTRA_PAUSE_DURATION_MS, 600_000L) ?: 600_000L
                handlePause(durationMs)
            }
            NotificationHelper.ACTION_PAUSE_1H -> {
                handlePause(600_000L)
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
                addAction(Intent.ACTION_SCREEN_ON)
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
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (sosLocateController.hasLocationPermission()) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
        } else {
            0
        }
        val initialNotification = notificationHelper.buildNotification(AppState())
        ServiceCompat.startForeground(
            this@RingerService,
            NotificationHelper.NOTIFICATION_ID,
            initialNotification,
            foregroundServiceType
        )
        Log.d(TAG, "Started foreground notification synchronously")
    }

    private fun observeAppState() {
        stateObservationJob?.cancel()
        stateObservationJob = serviceScope.launch {
            preferences.appStateFlow.collectLatest { state ->
                notificationHelper.updateNotification(state)
                manageSosSessionJob(state.sosLocateState)
            }
        }
    }

    private fun manageSosSessionJob(sosState: com.awayassist.app.data.SosLocateState) {
        if (!sosState.isSessionActive) {
            sosSessionJob?.cancel()
            sosSessionJob = null
            return
        }

        if (sosSessionJob == null || sosSessionJob?.isActive != true) {
            sosSessionJob = serviceScope.launch {
                val intervalMinutes = sosState.traceIntervalMins.coerceAtLeast(1)
                val intervalMs = intervalMinutes * 60_000L
                while (isActive) {
                    kotlinx.coroutines.delay(intervalMs)
                    sosLocateController.checkSessionTimeoutAndExecuteTraceTick()
                }
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
