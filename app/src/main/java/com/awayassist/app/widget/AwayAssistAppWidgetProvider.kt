package com.awayassist.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.awayassist.app.MainActivity
import com.awayassist.app.R
import com.awayassist.app.data.AppState
import com.awayassist.app.data.RingerState
import com.awayassist.app.service.NotificationHelper
import com.awayassist.app.service.RingerService
import java.util.Locale

class AwayAssistAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = buildRemoteViews(context, null)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        fun updateAll(context: Context, appState: AppState) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, AwayAssistAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val views = buildRemoteViews(context, appState)
                appWidgetManager.updateAppWidget(componentName, views)
            }
        }

        fun buildRemoteViews(context: Context, appState: AppState?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_glassmorphic)

            // Content Click (Open App)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                200,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Button 1: Auto / Resume
            val autoIntent = Intent(context, RingerService::class.java).apply {
                action = NotificationHelper.ACTION_RESUME
            }
            val autoPendingIntent = PendingIntent.getService(
                context,
                201,
                autoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.appwidget_btn_auto, autoPendingIntent)

            // Button 2: Force Ring
            val ringIntent = Intent(context, RingerService::class.java).apply {
                action = NotificationHelper.ACTION_FORCE_RING
            }
            val ringPendingIntent = PendingIntent.getService(
                context,
                202,
                ringIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.appwidget_btn_ring, ringPendingIntent)

            // Button 3: Pause 10m
            val pauseIntent = Intent(context, RingerService::class.java).apply {
                action = NotificationHelper.ACTION_PAUSE_10M
                putExtra(NotificationHelper.EXTRA_PAUSE_DURATION_MS, NotificationHelper.DEFAULT_PAUSE_DURATION_MS)
            }
            val pausePendingIntent = PendingIntent.getService(
                context,
                203,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.appwidget_btn_pause, pausePendingIntent)

            // State Population
            if (appState == null) {
                views.setTextViewText(R.id.appwidget_badge, "AUTO")
                views.setTextViewText(R.id.appwidget_headline, "Away Assist Ready")
                return views
            }

            val isPaused = appState.isPaused
            val isAutoMode = appState.isEnabled && !isPaused && appState.overrideMode == null
            val isForceRing = appState.overrideMode == RingerState.RING
            val isRingLocked = appState.currentMode == RingerState.RING && isAutoMode

            when {
                !appState.isEnabled && appState.overrideMode == null -> {
                    views.setTextViewText(R.id.appwidget_badge, "OFF")
                    views.setTextViewText(R.id.appwidget_headline, "Disabled")
                    views.setInt(R.id.appwidget_btn_auto, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                    views.setInt(R.id.appwidget_btn_ring, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                    views.setInt(R.id.appwidget_btn_pause, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                }
                isPaused -> {
                    val diffSecs = ((appState.pauseUntilTimestamp - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                    val mins = (diffSecs % 3600) / 60
                    val secs = diffSecs % 60
                    val countdown = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                    views.setTextViewText(R.id.appwidget_badge, "PAUSED")
                    views.setTextViewText(R.id.appwidget_headline, "Paused ($countdown)")
                    views.setTextViewText(R.id.appwidget_pause_text, countdown)

                    views.setInt(R.id.appwidget_btn_auto, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                    views.setInt(R.id.appwidget_btn_ring, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                    views.setInt(R.id.appwidget_btn_pause, "setBackgroundResource", R.drawable.bg_glass_widget_button_pause)
                }
                isForceRing -> {
                    views.setTextViewText(R.id.appwidget_badge, "RING")
                    views.setTextViewText(R.id.appwidget_headline, "Force Ring")
                    views.setTextViewText(R.id.appwidget_pause_text, "10m")

                    views.setInt(R.id.appwidget_btn_auto, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                    views.setInt(R.id.appwidget_btn_ring, "setBackgroundResource", R.drawable.bg_glass_widget_button_ring)
                    views.setInt(R.id.appwidget_btn_pause, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                }
                isRingLocked -> {
                    views.setTextViewText(R.id.appwidget_badge, "AUTO")
                    views.setTextViewText(R.id.appwidget_headline, "Screen Locked")
                    views.setTextViewText(R.id.appwidget_pause_text, "10m")

                    views.setInt(R.id.appwidget_btn_auto, "setBackgroundResource", R.drawable.bg_glass_widget_button_auto)
                    views.setInt(R.id.appwidget_btn_ring, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                    views.setInt(R.id.appwidget_btn_pause, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                }
                else -> {
                    views.setTextViewText(R.id.appwidget_badge, "AUTO")
                    views.setTextViewText(R.id.appwidget_headline, "Screen Unlocked")
                    views.setTextViewText(R.id.appwidget_pause_text, "10m")

                    views.setInt(R.id.appwidget_btn_auto, "setBackgroundResource", R.drawable.bg_glass_widget_button_auto)
                    views.setInt(R.id.appwidget_btn_ring, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                    views.setInt(R.id.appwidget_btn_pause, "setBackgroundResource", R.drawable.bg_glass_widget_button)
                }
            }

            return views
        }
    }
}
