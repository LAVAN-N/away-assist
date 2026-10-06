package com.awayassist.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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

        // 1. Ignore broadcasts during initial device boot uptime (<45s) to prevent false alerts during modem startup
        if (SystemClock.elapsedRealtime() < 45_000L) {
            Log.d(TAG, "Ignoring SIM state change during initial device boot uptime (<45s)")
            return
        }

        val simStateExtra = intent.getStringExtra(EXTRA_SIM_STATE)
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simState = tm?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN

        Log.d(TAG, "SIM broadcast: action=$action, extra=$simStateExtra, simState=$simState")

        // 2. Only trigger if SIM is genuinely removed / ABSENT
        val isAbsent = simStateExtra.equals(SIM_STATE_ABSENT, ignoreCase = true) ||
                       simState == TelephonyManager.SIM_STATE_ABSENT

        if (isAbsent) {
            Log.w(TAG, "SIM card absence confirmed! Triggering SOS emergency SIM alert.")
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
