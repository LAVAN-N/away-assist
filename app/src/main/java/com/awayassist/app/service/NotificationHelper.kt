package com.awayassist.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.awayassist.app.MainActivity
import com.awayassist.app.R
import com.awayassist.app.data.AppState
import com.awayassist.app.data.RingerState
import com.awayassist.app.widget.AwayAssistAppWidgetProvider
import java.util.Locale

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

        // Pending Intents for Widget Buttons
        val resumeIntent = PendingIntent.getService(
            context,
            101,
            Intent(context, RingerService::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val forceSilentIntent = PendingIntent.getService(
            context,
            102,
            Intent(context, RingerService::class.java).apply { action = ACTION_FORCE_SILENT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val forceRingIntent = PendingIntent.getService(
            context,
            103,
            Intent(context, RingerService::class.java).apply { action = ACTION_FORCE_RING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pause1hIntent = PendingIntent.getService(
            context,
            104,
            Intent(context, RingerService::class.java).apply { action = ACTION_PAUSE_1H },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPaused = appState.isPaused
        val isRingMode = appState.currentMode == RingerState.RING && !isPaused && appState.overrideMode == null
        val isForceRing = appState.overrideMode == RingerState.RING
        val isForceSilent = appState.overrideMode == RingerState.VIBRATE || appState.overrideMode == RingerState.SILENT

        val countdownText: String = if (isPaused) {
            val diffSecs = ((appState.pauseUntilTimestamp - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            val mins = (diffSecs % 3600) / 60
            val secs = diffSecs % 60
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        } else ""

        val (statusText, statusIconRes, badgeText) = when {
            !appState.isEnabled && appState.overrideMode == null -> {
                Triple("Disabled", R.drawable.ic_widget_silent, "OFF")
            }
            isPaused -> {
                Triple("Paused $countdownText", R.drawable.ic_widget_pause, "PAUSE")
            }
            isForceRing -> {
                Triple("Ring (Forced)", R.drawable.ic_widget_ring, "RING")
            }
            isForceSilent -> {
                Triple("Silent (Forced)", R.drawable.ic_widget_silent, "SILENT")
            }
            isRingMode -> {
                Triple("Ring", R.drawable.ic_widget_ring, "LOCKED")
            }
            else -> {
                Triple("Silent", R.drawable.ic_widget_silent, "IN USE")
            }
        }

        // Build Ultra-Compact Single-Row RemoteViews
        val compactViews = RemoteViews(context.packageName, R.layout.notification_glass_collapsed).apply {
            setTextViewText(R.id.notification_status_text, statusText)
            setTextViewText(R.id.notification_mode_badge, badgeText)
            setImageViewResource(R.id.notification_status_icon, statusIconRes)

            // Button 1: Force Silent / Force Ring Toggle
            if (appState.currentMode == RingerState.RING && appState.overrideMode != RingerState.VIBRATE) {
                setTextViewText(R.id.widget_toggle_text, "Silent")
                setImageViewResource(R.id.widget_toggle_icon, R.drawable.ic_widget_silent)
                setOnClickPendingIntent(R.id.widget_btn_toggle, forceSilentIntent)
            } else {
                setTextViewText(R.id.widget_toggle_text, "Ring")
                setImageViewResource(R.id.widget_toggle_icon, R.drawable.ic_widget_ring)
                setOnClickPendingIntent(R.id.widget_btn_toggle, forceRingIntent)
            }

            // Button 2: Pause 1h
            setOnClickPendingIntent(R.id.widget_btn_pause, pause1hIntent)

            // Button 3: Auto / Resume
            setOnClickPendingIntent(R.id.widget_btn_auto, resumeIntent)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Away Assist")
            .setContentText(statusText)
            .setContentIntent(contentIntent)
            .setCustomContentView(compactViews)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return builder.build()
    }

    fun updateNotification(appState: AppState) {
        val notification = buildNotification(appState)
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Synchronously update home screen widgets whenever state changes
        try {
            AwayAssistAppWidgetProvider.updateAll(context, appState)
        } catch (_: Exception) {}
    }
}
