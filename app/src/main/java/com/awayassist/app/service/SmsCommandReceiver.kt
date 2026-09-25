package com.awayassist.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.awayassist.app.util.SosLocateController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsCommandReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsCommandReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group by sender in case multipart messages arrive
        val messagesBySender = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val controller = SosLocateController(context.applicationContext)
                for ((sender, parts) in messagesBySender) {
                    if (sender.isNullOrBlank()) continue
                    val fullBody = parts.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
                    if (fullBody.isNotBlank()) {
                        controller.handleSmsCommand(sender, fullBody)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS command", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
