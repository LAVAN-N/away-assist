package com.awayassist.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
        val resumeIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                101,
                Intent(context, RingerService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                101,
                Intent(context, RingerService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val forceSilentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                102,
                Intent(context, RingerService::class.java).apply { action = ACTION_FORCE_SILENT },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                102,
                Intent(context, RingerService::class.java).apply { action = ACTION_FORCE_SILENT },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val forceRingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                103,
                Intent(context, RingerService::class.java).apply { action = ACTION_FORCE_RING },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                103,
                Intent(context, RingerService::class.java).apply { action = ACTION_FORCE_RING },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val pause10mIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                104,
                Intent(context, RingerService::class.java).apply {
                    action = ACTION_PAUSE_10M
                    putExtra(EXTRA_PAUSE_DURATION_MS, DEFAULT_PAUSE_DURATION_MS)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                104,
                Intent(context, RingerService::class.java).apply {
                    action = ACTION_PAUSE_10M
                    putExtra(EXTRA_PAUSE_DURATION_MS, DEFAULT_PAUSE_DURATION_MS)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

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

        // Concise, refined max-3-words labels
        val (titleText, subtitleText, badgeText, statusIconRes, dotAnimRes, badgeTextColor) = when {
            !appState.isEnabled && appState.overrideMode == null -> {
                Sextet("Disabled", "Automation Off", "OFF", R.drawable.ic_widget_silent, R.drawable.anim_dot_gray, 0xFF8E8E93.toInt())
            }
            isPaused -> {
                Sextet("Paused ($countdownText)", "Resumes in $countdownText", "PAUSE", R.drawable.ic_widget_pause, R.drawable.anim_dot_amber, 0xFFFFB300.toInt())
            }
            isForceRing -> {
                Sextet("Force Ring", "Always Audible", "RING", R.drawable.ic_widget_ring, R.drawable.anim_dot_green, 0xFF30D158.toInt())
            }
            isForceSilent -> {
                Sextet("Force Silent", "Always Silent", "SILENT", R.drawable.ic_widget_silent, R.drawable.anim_dot_indigo, 0xFF7D7AFF.toInt())
            }
            isRingMode -> {
                Sextet("Ring Mode", "Audible on Lock", "AUTO", R.drawable.ic_widget_ring, R.drawable.anim_dot_green, 0xFF30D158.toInt())
            }
            else -> {
                Sextet("Silent Mode", "Vibrate in Use", "AUTO", R.drawable.ic_widget_silent, R.drawable.anim_dot_indigo, 0xFF7D7AFF.toInt())
            }
        }

        // 1. Build Collapsed Single-Row RemoteViews (Side-by-side)
        val collapsedViews = RemoteViews(context.packageName, R.layout.notification_glass_collapsed).apply {
            setTextViewText(R.id.notification_status_text, titleText)
            setTextViewText(R.id.notification_subtitle_text, subtitleText)
            setTextViewText(R.id.notification_mode_badge, badgeText)
            setTextColor(R.id.notification_mode_badge, badgeTextColor)
            setImageViewResource(R.id.notification_mode_dot, dotAnimRes)
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
            setTextViewText(R.id.widget_auto_text, "Auto")
            setInt(
                R.id.widget_btn_auto,
                "setBackgroundResource",
                if (isAutoMode) R.drawable.bg_glass_widget_button_auto else R.drawable.bg_glass_widget_button
            )
            setOnClickPendingIntent(R.id.widget_btn_auto, resumeIntent)
        }

        // 2. Build Expanded RemoteViews (Buttons Relocated Below Hint Text across Full Width)
        val expandedViews = RemoteViews(context.packageName, R.layout.notification_glass_expanded).apply {
            setTextViewText(R.id.expanded_status_text, titleText)
            setTextViewText(R.id.expanded_subtitle_text, subtitleText)
            setTextViewText(R.id.expanded_mode_badge, badgeText)
            setTextColor(R.id.expanded_mode_badge, badgeTextColor)
            setImageViewResource(R.id.expanded_mode_dot, dotAnimRes)
            setImageViewResource(R.id.expanded_status_icon, statusIconRes)

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
            setTextViewText(R.id.widget_auto_text, "Auto")
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
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews) // Big view relocates buttons cleanly below hint text!
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

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private data class Sextet<A, B, C, D, E, F>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F)
}
