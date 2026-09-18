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
        const val ACTION_PAUSE_10M = "com.awayassist.app.ACTION_PAUSE_10M"
        const val ACTION_PAUSE_1H = "com.awayassist.app.ACTION_PAUSE_1H"
        const val ACTION_RESUME = "com.awayassist.app.ACTION_RESUME"
        const val ACTION_START = "com.awayassist.app.ACTION_START"
        const val ACTION_STOP = "com.awayassist.app.ACTION_STOP"
        const val EXTRA_PAUSE_DURATION_MS = "com.awayassist.app.EXTRA_PAUSE_DURATION_MS"

        const val DEFAULT_PAUSE_DURATION_MS = 10 * 60_000L // 10 minutes default
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

        val pause10mIntent = PendingIntent.getService(
            context,
            104,
            Intent(context, RingerService::class.java).apply {
                action = ACTION_PAUSE_10M
                putExtra(EXTRA_PAUSE_DURATION_MS, DEFAULT_PAUSE_DURATION_MS)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPaused = appState.isPaused
        val isAutoMode = appState.isEnabled && !isPaused && appState.overrideMode == null
        val isRingMode = appState.currentMode == RingerState.RING && isAutoMode
        val isForceRing = appState.overrideMode == RingerState.RING
        val isForceSilent = appState.overrideMode == RingerState.VIBRATE || appState.overrideMode == RingerState.SILENT

        val countdownText: String = if (isPaused) {
            val diffSecs = ((appState.pauseUntilTimestamp - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            val mins = (diffSecs % 3600) / 60
            val secs = diffSecs % 60
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        } else ""

        val (titleText, subtitleText, statusIconRes) = when {
            !appState.isEnabled && appState.overrideMode == null -> {
                Triple("Disabled", "Automation Off", R.drawable.ic_widget_silent)
            }
            isPaused -> {
                Triple("Paused ($countdownText)", "Resumes in $countdownText", R.drawable.ic_widget_pause)
            }
            isForceRing -> {
                Triple("Force Ring [ACTIVE]", "Manual Override • Locked", R.drawable.ic_widget_ring)
            }
            isForceSilent -> {
                Triple("Force Silent [ACTIVE]", "Manual Override • Silent", R.drawable.ic_widget_silent)
            }
            isRingMode -> {
                Triple("Ring Mode [AUTO]", "Screen Locked ➔ Audible", R.drawable.ic_widget_ring)
            }
            else -> {
                Triple("Silent Mode [AUTO]", "Screen Unlocked ➔ Silent", R.drawable.ic_widget_silent)
            }
        }

        // Build Ultra-Compact Single-Row RemoteViews
        val compactViews = RemoteViews(context.packageName, R.layout.notification_glass_collapsed).apply {
            setTextViewText(R.id.notification_status_text, titleText)
            setTextViewText(R.id.notification_subtitle_text, subtitleText)
            setImageViewResource(R.id.notification_status_icon, statusIconRes)

            // Button 1: Force Silent / Force Ring Toggle
            if (appState.currentMode == RingerState.RING && appState.overrideMode != RingerState.VIBRATE) {
                setTextViewText(R.id.widget_toggle_text, "Silent")
                setImageViewResource(R.id.widget_toggle_icon, R.drawable.ic_widget_silent)
                setInt(
                    R.id.widget_btn_toggle,
                    "setBackgroundResource",
                    if (isForceRing) R.drawable.bg_glass_widget_button_ring else R.drawable.bg_glass_widget_button
                )
                setOnClickPendingIntent(R.id.widget_btn_toggle, forceSilentIntent)
            } else {
                setTextViewText(R.id.widget_toggle_text, "Ring")
                setImageViewResource(R.id.widget_toggle_icon, R.drawable.ic_widget_ring)
                setInt(
                    R.id.widget_btn_toggle,
                    "setBackgroundResource",
                    if (isForceSilent) R.drawable.bg_glass_widget_button_accent else R.drawable.bg_glass_widget_button
                )
                setOnClickPendingIntent(R.id.widget_btn_toggle, forceRingIntent)
            }

            // Button 2: Pause 10m
            setTextViewText(R.id.widget_pause_text, if (isPaused) countdownText else "10m")
            setInt(
                R.id.widget_btn_pause,
                "setBackgroundResource",
                if (isPaused) R.drawable.bg_glass_widget_button_pause else R.drawable.bg_glass_widget_button
            )
            setOnClickPendingIntent(R.id.widget_btn_pause, pause10mIntent)

            // Button 3: Auto
            setTextViewText(R.id.widget_auto_text, if (isAutoMode) "● Auto" else "Auto")
            setInt(
                R.id.widget_btn_auto,
                "setBackgroundResource",
                if (isAutoMode) R.drawable.bg_glass_widget_button_auto else R.drawable.bg_glass_widget_button
            )
            setOnClickPendingIntent(R.id.widget_btn_auto, resumeIntent)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleText)
            .setContentText(subtitleText)
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
