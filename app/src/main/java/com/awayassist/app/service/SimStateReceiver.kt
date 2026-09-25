package com.awayassist.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.awayassist.app.util.SosLocateController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SimStateReceiver"
        private const val ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED"
        private const val ACTION_SIM_CARD_STATE_CHANGED = "android.telephony.action.SIM_CARD_STATE_CHANGED"
        private const val EXTRA_SIM_STATE = "ss"
        private const val SIM_STATE_ABSENT = "ABSENT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_SIM_STATE_CHANGED && action != ACTION_SIM_CARD_STATE_CHANGED) {
            return
        }

        val simStateExtra = intent.getStringExtra(EXTRA_SIM_STATE)
        Log.d(TAG, "SIM state changed broadcast received. State: $simStateExtra")

        // Trigger if SIM was pulled / absent
        if (simStateExtra == SIM_STATE_ABSENT || action == ACTION_SIM_CARD_STATE_CHANGED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val controller = SosLocateController(context.applicationContext)
                    controller.handleSimStateChanged()
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling SIM state change", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
