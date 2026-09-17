package com.awayassist.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.awayassist.app.MainActivity
import com.awayassist.app.R
import com.awayassist.app.data.AppState
import com.awayassist.app.data.RingerState

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "away_assist_foreground_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_FORCE_RING = "com.awayassist.app.ACTION_FORCE_RING"
        const val ACTION_FORCE_SILENT = "com.awayassist.app.ACTION_FORCE_SILENT"
        const val ACTION_PAUSE = "com.awayassist.app.ACTION_PAUSE"
        const val ACTION_PAUSE_1H = "com.awayassist.app.ACTION_PAUSE_1H"
        const val ACTION_RESUME = "com.awayassist.app.ACTION_RESUME"
        const val ACTION_START = "com.awayassist.app.ACTION_START"
        const val ACTION_STOP = "com.awayassist.app.ACTION_STOP"
        const val EXTRA_PAUSE_DURATION_MS = "com.awayassist.app.EXTRA_PAUSE_DURATION_MS"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Away Assist Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows current ringer mode and active automation status"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(appState: AppState): Notification {
        createNotificationChannel()

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when {
            !appState.isEnabled && appState.overrideMode == null -> "Automation disabled"
            appState.isPaused -> {
                val remainingMinutes = ((appState.pauseUntilTimestamp - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
                "⏸ Paused for next ${remainingMinutes}m"
            }
            appState.overrideMode != null -> {
                when (appState.overrideMode) {
                    RingerState.RING -> "🔔 Force Ring active"
                    RingerState.VIBRATE, RingerState.SILENT -> "🔕 Force Silent active"
                    else -> "Manual override active"
                }
            }
            appState.currentMode == RingerState.RING -> "🔔 Ring mode — screen locked"
            appState.currentMode == RingerState.VIBRATE || appState.currentMode == RingerState.SILENT -> "🔕 Silent — screen unlocked"
            else -> "Monitoring lock state"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Away Assist")
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Add inline actions (max 2)
        if (appState.isPaused || appState.overrideMode != null) {
            val resumeIntent = PendingIntent.getService(
                context,
                101,
                Intent(context, RingerService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Resume", resumeIntent)
        } else {
            if (appState.currentMode == RingerState.RING) {
                val silentIntent = PendingIntent.getService(
                    context,
                    102,
                    Intent(context, RingerService::class.java).apply { action = ACTION_FORCE_SILENT },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, "Force Silent", silentIntent)
            } else {
                val ringIntent = PendingIntent.getService(
                    context,
                    103,
                    Intent(context, RingerService::class.java).apply { action = ACTION_FORCE_RING },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, "Force Ring", ringIntent)
            }

            val pauseIntent = PendingIntent.getService(
                context,
                104,
                Intent(context, RingerService::class.java).apply { action = ACTION_PAUSE_1H },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Pause 1h", pauseIntent)
        }

        return builder.build()
    }

    fun updateNotification(appState: AppState) {
        val notification = buildNotification(appState)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
