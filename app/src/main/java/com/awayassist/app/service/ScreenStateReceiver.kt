package com.awayassist.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awayassist.app.data.AwayAssistPreferences
import com.awayassist.app.data.RingerState
import com.awayassist.app.util.RingerModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "onReceive triggered with action: $action")

        val preferences = AwayAssistPreferences.getInstance(context)
        val ringerController = RingerModeController(context)
        val notificationHelper = NotificationHelper(context)

        receiverScope.launch {
            val state = preferences.getAppState()

            if (!state.isEnabled) {
                Log.d(TAG, "Automation is disabled in preferences; ignoring $action")
                return@launch
            }

            if (state.isPaused) {
                Log.d(TAG, "Automation is currently paused until ${state.pauseUntilTimestamp}; ignoring $action")
                return@launch
            }

            when (action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen locked -> setting ringer to RING (Normal)")
                    val success = ringerController.setRingMode()
                    if (success) {
                        preferences.updateRingerState(
                            mode = RingerState.RING,
                            overrideMode = null
                        )
                        notificationHelper.updateNotification(
                            preferences.getAppState()
                        )
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "Screen unlocked (User present) -> setting ringer to VIBRATE")
                    val success = ringerController.setVibrateMode()
                    if (success) {
                        preferences.updateRingerState(
                            mode = RingerState.VIBRATE,
                            overrideMode = null
                        )
                        notificationHelper.updateNotification(
                            preferences.getAppState()
                        )
                    }
                }
                else -> {
                    Log.d(TAG, "Unhandled action: $action")
                }
            }
        }
    }
}
