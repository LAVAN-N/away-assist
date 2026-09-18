package com.awayassist.app.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.awayassist.app.data.RingerState

class RingerModeController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        private const val TAG = "RingerModeController"
    }

    /**
     * Checks if the app has Notification Policy Access (Do Not Disturb access).
     * Required on Android 6.0+ to modify ringer mode to/from silent or vibrate.
     */
    fun isNotificationPolicyAccessGranted(): Boolean {
        val granted = notificationManager?.isNotificationPolicyAccessGranted == true
        Log.d(TAG, "Notification policy access granted: $granted")
        return granted
    }

    /**
     * Retrieves the current device ringer mode as a [RingerState].
     */
    fun getCurrentRingerState(): RingerState {
        val mode = audioManager?.ringerMode ?: return RingerState.UNKNOWN
        return when (mode) {
            AudioManager.RINGER_MODE_NORMAL -> RingerState.RING
            AudioManager.RINGER_MODE_VIBRATE -> RingerState.VIBRATE
            AudioManager.RINGER_MODE_SILENT -> RingerState.SILENT
            else -> RingerState.UNKNOWN
        }
    }

    /**
     * Sets the ringer mode to normal (Ring).
     * Must be guarded by Notification Policy Access check.
     */
    fun setRingMode(): Boolean {
        return setRingerMode(AudioManager.RINGER_MODE_NORMAL, "RING")
    }

    /**
     * Sets the ringer mode to vibrate.
     * Must be guarded by Notification Policy Access check.
     */
    fun setVibrateMode(): Boolean {
        return setRingerMode(AudioManager.RINGER_MODE_VIBRATE, "VIBRATE")
    }

    /**
     * Sets the ringer mode to silent.
     * Must be guarded by Notification Policy Access check.
     */
    fun setSilentMode(): Boolean {
        return setRingerMode(AudioManager.RINGER_MODE_SILENT, "SILENT")
    }

    private fun setRingerMode(targetMode: Int, modeLabel: String): Boolean {
        if (!isNotificationPolicyAccessGranted()) {
            Log.w(
                TAG,
                "Cannot change ringer mode to $modeLabel: Notification policy access NOT granted."
            )
            return false
        }

        val am = audioManager
        if (am == null) {
            Log.e(TAG, "AudioManager is null; failed to set ringer mode to $modeLabel")
            return false
        }

        try {
            val current = am.ringerMode
            if (current == targetMode) {
                Log.d(TAG, "Ringer mode is already $modeLabel ($targetMode); no change needed.")
                return true
            }

            Log.d(TAG, "Changing ringer mode from $current to $modeLabel ($targetMode)")
            am.ringerMode = targetMode
            Log.i(TAG, "Successfully changed ringer mode to $modeLabel ($targetMode)")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while changing ringer mode to $modeLabel", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error changing ringer mode to $modeLabel", e)
            return false
        }
    }
}
