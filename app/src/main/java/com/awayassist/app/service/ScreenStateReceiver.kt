package com.awayassist.app.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.awayassist.app.data.AwayAssistPreferences
import com.awayassist.app.data.RingerState
import com.awayassist.app.util.RingerModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateReceiver"
        private val processingMutex = Mutex()
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "onReceive triggered with action: $action")

        val preferences = AwayAssistPreferences.getInstance(context)
        val ringerController = RingerModeController(context)
        val notificationHelper = NotificationHelper(context)

        receiverScope.launch {
            // Guarantee strictly sequential, race-free processing for rapid lock/unlock events
            processingMutex.withLock {
                val state = preferences.getAppState()

                if (!state.isEnabled) {
                    Log.d(TAG, "Automation is disabled in preferences; ignoring $action")
                    return@withLock
                }

                if (state.isPaused) {
                    Log.d(TAG, "Automation is currently paused until ${state.pauseUntilTimestamp}; ignoring $action")
                    return@withLock
                }

                if (state.overrideMode != null) {
                    Log.d(TAG, "Manual override active (${state.overrideMode}); ignoring $action")
                    return@withLock
                }

                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

                val isInteractive = powerManager?.isInteractive ?: true
                val isKeyguardLocked = keyguardManager?.isKeyguardLocked ?: false

                // Determine target ringer mode based on verified hardware state:
                // - If screen is turned off or not interactive: device is locked -> set RING
                // - If user is present or screen is interactive and unlocked: device is unlocked -> set VIBRATE
                val targetMode: RingerState? = when {
                    action == Intent.ACTION_SCREEN_OFF || !isInteractive -> {
                        RingerState.RING
                    }
                    action == Intent.ACTION_USER_PRESENT || (action == Intent.ACTION_SCREEN_ON && !isKeyguardLocked) -> {
                        RingerState.VIBRATE
                    }
                    else -> null
                }

                if (targetMode != null) {
                    Log.d(
                        TAG,
                        "Setting target mode $targetMode (action=$action, interactive=$isInteractive, keyguardLocked=$isKeyguardLocked)"
                    )
                    val success = when (targetMode) {
                        RingerState.RING -> ringerController.setRingMode()
                        RingerState.VIBRATE -> ringerController.setVibrateMode()
                        else -> false
                    }
                    if (success) {
                        preferences.updateRingerState(
                            mode = targetMode,
                            overrideMode = null
                        )
                        notificationHelper.updateNotification(
                            preferences.getAppState()
                        )
                    }
                }
            }
        }
    }
}
