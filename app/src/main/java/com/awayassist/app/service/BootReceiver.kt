package com.awayassist.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awayassist.app.data.AwayAssistPreferences
import com.awayassist.app.util.SosLocateController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            return
        }

        Log.d(TAG, "Boot completed broadcast received: $action")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = AwayAssistPreferences.getInstance(context.applicationContext)
                val appState = preferences.getAppState()

                // If core ringer service should be active, restart it
                if (appState.isEnabled) {
                    RingerService.startService(context.applicationContext)
                }

                // Handle SOS Locate boot alert
                val controller = SosLocateController(context.applicationContext)
                controller.handleBoot()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling boot broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
