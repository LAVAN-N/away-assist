package com.awayassist.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awayassist.app.util.SosLocateController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShutdownReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ShutdownReceiver"
        private const val ACTION_QUICKBOOT_POWEROFF = "android.intent.action.QUICKBOOT_POWEROFF"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_SHUTDOWN && action != ACTION_QUICKBOOT_POWEROFF) {
            return
        }

        Log.d(TAG, "Device shutdown broadcast received: $action")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val controller = SosLocateController(context.applicationContext)
                controller.handleShutdown()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling device shutdown", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
