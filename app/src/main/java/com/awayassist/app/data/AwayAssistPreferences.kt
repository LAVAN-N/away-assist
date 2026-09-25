package com.awayassist.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "away_assist_preferences")

enum class RingerState(val displayName: String) {
    RING("Ring"),
    VIBRATE("Vibrate"),
    SILENT("Silent"),
    UNKNOWN("Unknown")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class SosSessionState {
    NONE,
    TRACK,
    TRACE
}

data class SosLocateState(
    val isSosEnabled: Boolean = false,
    val emergencyAlertNumber: String = "",
    val commandPrefix: String = "",
    val prefixSha256: String = "",
    val prefixRotationNeeded: Boolean = false,
    val triggerSimRemoved: Boolean = true,
    val triggerShutdown: Boolean = true,
    val triggerSmsCommands: Boolean = true,
    val triggerBoot: Boolean = true,
    val autoTimeoutHours: Int = 4,
    val sessionState: SosSessionState = SosSessionState.NONE,
    val sessionStartTime: Long = 0L,
    val activeTargetNumber: String = "",
    val traceIntervalMins: Int = 5,
    val lastTriggeredTimestamp: Long = 0L,
    val lastTriggerDesc: String = "",
    val isOnboarded: Boolean = false
) {
    val isSessionActive: Boolean
        get() = sessionState != SosSessionState.NONE
}

data class AppState(
    val isEnabled: Boolean = true,
    val currentMode: RingerState = RingerState.UNKNOWN,
    val lastChangedTimestamp: Long = 0L,
    val pauseUntilTimestamp: Long = 0L,
    val customPauseDurationMs: Long = 10 * 60_000L,
    val overrideMode: RingerState? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sosLocateState: SosLocateState = SosLocateState()
) {
    val isPaused: Boolean
        get() = pauseUntilTimestamp > System.currentTimeMillis()
}

fun formatPauseDurationLabel(durationMs: Long): String {
    val mins = (durationMs / 60_000L).coerceAtLeast(1L)
    val hours = mins / 60
    val remMins = mins % 60
    return when {
        hours > 0 && remMins > 0 -> "${hours}h ${remMins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

fun computeSha256(input: String): String {
    if (input.isEmpty()) return ""
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

class AwayAssistPreferences(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_IS_ENABLED = booleanPreferencesKey("is_enabled")
        val KEY_CURRENT_MODE = stringPreferencesKey("current_mode")
        val KEY_LAST_CHANGED_TIMESTAMP = longPreferencesKey("last_changed_timestamp")
        val KEY_PAUSE_UNTIL_TIMESTAMP = longPreferencesKey("pause_until_timestamp")
        val KEY_CUSTOM_PAUSE_DURATION_MS = longPreferencesKey("custom_pause_duration_ms")
        val KEY_OVERRIDE_MODE = stringPreferencesKey("override_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

        // SOS Locate Preference Keys
        val KEY_SOS_ENABLED = booleanPreferencesKey("sos_enabled")
        val KEY_SOS_EMERGENCY_NUMBER = stringPreferencesKey("sos_emergency_number")
        val KEY_SOS_COMMAND_PREFIX = stringPreferencesKey("sos_command_prefix")
        val KEY_SOS_PREFIX_SHA256 = stringPreferencesKey("sos_prefix_sha256")
        val KEY_SOS_PREFIX_ROTATION_NEEDED = booleanPreferencesKey("sos_prefix_rotation_needed")
        val KEY_SOS_TRIGGER_SIM_REMOVED = booleanPreferencesKey("sos_trigger_sim_removed")
        val KEY_SOS_TRIGGER_SHUTDOWN = booleanPreferencesKey("sos_trigger_shutdown")
        val KEY_SOS_TRIGGER_SMS_COMMANDS = booleanPreferencesKey("sos_trigger_sms_commands")
        val KEY_SOS_TRIGGER_BOOT = booleanPreferencesKey("sos_trigger_boot")
        val KEY_SOS_AUTO_TIMEOUT_HOURS = intPreferencesKey("sos_auto_timeout_hours")
        val KEY_SOS_SESSION_STATE = stringPreferencesKey("sos_session_state")
        val KEY_SOS_SESSION_START_TIME = longPreferencesKey("sos_session_start_time")
        val KEY_SOS_ACTIVE_TARGET_NUMBER = stringPreferencesKey("sos_active_target_number")
        val KEY_SOS_TRACE_INTERVAL_MINS = intPreferencesKey("sos_trace_interval_mins")
        val KEY_SOS_LAST_TRIGGERED_TIMESTAMP = longPreferencesKey("sos_last_triggered_timestamp")
        val KEY_SOS_LAST_TRIGGER_DESC = stringPreferencesKey("sos_last_trigger_desc")
        val KEY_SOS_IS_ONBOARDED = booleanPreferencesKey("sos_is_onboarded")

        @Volatile
        private var INSTANCE: AwayAssistPreferences? = null

        fun getInstance(context: Context): AwayAssistPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AwayAssistPreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    val appStateFlow: Flow<AppState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isEnabled = preferences[KEY_IS_ENABLED] ?: true
            val currentModeStr = preferences[KEY_CURRENT_MODE] ?: RingerState.UNKNOWN.name
            val currentMode = try {
                RingerState.valueOf(currentModeStr)
            } catch (e: IllegalArgumentException) {
                RingerState.UNKNOWN
            }
            val lastChanged = preferences[KEY_LAST_CHANGED_TIMESTAMP] ?: 0L
            val pauseUntil = preferences[KEY_PAUSE_UNTIL_TIMESTAMP] ?: 0L
            val customPauseDuration = preferences[KEY_CUSTOM_PAUSE_DURATION_MS] ?: (10 * 60_000L)
            val overrideStr = preferences[KEY_OVERRIDE_MODE]
            val overrideMode = overrideStr?.let {
                try {
                    RingerState.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            val themeModeStr = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeStr)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }

            val sosSessionStateStr = preferences[KEY_SOS_SESSION_STATE] ?: SosSessionState.NONE.name
            val sosSessionState = try {
                SosSessionState.valueOf(sosSessionStateStr)
            } catch (e: IllegalArgumentException) {
                SosSessionState.NONE
            }

            val sosLocateState = SosLocateState(
                isSosEnabled = preferences[KEY_SOS_ENABLED] ?: false,
                emergencyAlertNumber = preferences[KEY_SOS_EMERGENCY_NUMBER] ?: "",
                commandPrefix = preferences[KEY_SOS_COMMAND_PREFIX] ?: "",
                prefixSha256 = preferences[KEY_SOS_PREFIX_SHA256] ?: "",
                prefixRotationNeeded = preferences[KEY_SOS_PREFIX_ROTATION_NEEDED] ?: false,
                triggerSimRemoved = preferences[KEY_SOS_TRIGGER_SIM_REMOVED] ?: true,
                triggerShutdown = preferences[KEY_SOS_TRIGGER_SHUTDOWN] ?: true,
                triggerSmsCommands = preferences[KEY_SOS_TRIGGER_SMS_COMMANDS] ?: true,
                triggerBoot = preferences[KEY_SOS_TRIGGER_BOOT] ?: true,
                autoTimeoutHours = preferences[KEY_SOS_AUTO_TIMEOUT_HOURS] ?: 4,
                sessionState = sosSessionState,
                sessionStartTime = preferences[KEY_SOS_SESSION_START_TIME] ?: 0L,
                activeTargetNumber = preferences[KEY_SOS_ACTIVE_TARGET_NUMBER] ?: "",
                traceIntervalMins = preferences[KEY_SOS_TRACE_INTERVAL_MINS] ?: 5,
                lastTriggeredTimestamp = preferences[KEY_SOS_LAST_TRIGGERED_TIMESTAMP] ?: 0L,
                lastTriggerDesc = preferences[KEY_SOS_LAST_TRIGGER_DESC] ?: "",
                isOnboarded = preferences[KEY_SOS_IS_ONBOARDED] ?: false
            )

            AppState(
                isEnabled = isEnabled,
                currentMode = currentMode,
                lastChangedTimestamp = lastChanged,
                pauseUntilTimestamp = pauseUntil,
                customPauseDurationMs = customPauseDuration,
                overrideMode = overrideMode,
                themeMode = themeMode,
                sosLocateState = sosLocateState
            )
        }

    suspend fun getAppState(): AppState = appStateFlow.first()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_ENABLED] = enabled
            if (enabled) {
                // Clearing override when user explicitly re-enables automation
                preferences.remove(KEY_OVERRIDE_MODE)
                preferences[KEY_PAUSE_UNTIL_TIMESTAMP] = 0L
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun forceRing() {
        dataStore.edit { preferences ->
            preferences[KEY_IS_ENABLED] = false
            preferences[KEY_CURRENT_MODE] = RingerState.RING.name
            preferences[KEY_OVERRIDE_MODE] = RingerState.RING.name
            preferences[KEY_PAUSE_UNTIL_TIMESTAMP] = 0L
            preferences[KEY_LAST_CHANGED_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun forceSilent() {
        dataStore.edit { preferences ->
            preferences[KEY_IS_ENABLED] = false
            preferences[KEY_CURRENT_MODE] = RingerState.VIBRATE.name
            preferences[KEY_OVERRIDE_MODE] = RingerState.VIBRATE.name
            preferences[KEY_PAUSE_UNTIL_TIMESTAMP] = 0L
            preferences[KEY_LAST_CHANGED_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun updateRingerState(
        mode: RingerState,
        overrideMode: RingerState? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENT_MODE] = mode.name
            preferences[KEY_LAST_CHANGED_TIMESTAMP] = timestamp
            if (overrideMode != null) {
                preferences[KEY_OVERRIDE_MODE] = overrideMode.name
            } else {
                preferences.remove(KEY_OVERRIDE_MODE)
            }
        }
    }

    suspend fun pauseForDuration(durationMs: Long) {
        val pauseUntil = System.currentTimeMillis() + durationMs
        dataStore.edit { preferences ->
            preferences.remove(KEY_OVERRIDE_MODE)
            preferences[KEY_PAUSE_UNTIL_TIMESTAMP] = pauseUntil
            preferences[KEY_CUSTOM_PAUSE_DURATION_MS] = durationMs
        }
    }

    suspend fun setCustomPauseDuration(durationMs: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_PAUSE_DURATION_MS] = durationMs
        }
    }

    suspend fun clearPause() {
        dataStore.edit { preferences ->
            preferences[KEY_IS_ENABLED] = true
            preferences[KEY_PAUSE_UNTIL_TIMESTAMP] = 0L
            preferences.remove(KEY_OVERRIDE_MODE)
        }
    }

    // --- SOS Locate Operations ---

    suspend fun setSosEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_ENABLED] = enabled
            if (!enabled) {
                preferences[KEY_SOS_SESSION_STATE] = SosSessionState.NONE.name
                preferences[KEY_SOS_ACTIVE_TARGET_NUMBER] = ""
            }
        }
    }

    suspend fun completeSosOnboarding(
        enabled: Boolean,
        emergencyNumber: String,
        prefix: String
    ) {
        val cleanPrefix = prefix.trim()
        dataStore.edit { preferences ->
            preferences[KEY_SOS_IS_ONBOARDED] = true
            preferences[KEY_SOS_ENABLED] = enabled
            preferences[KEY_SOS_EMERGENCY_NUMBER] = emergencyNumber.trim()
            preferences[KEY_SOS_COMMAND_PREFIX] = cleanPrefix
            preferences[KEY_SOS_PREFIX_SHA256] = computeSha256(cleanPrefix)
            preferences[KEY_SOS_PREFIX_ROTATION_NEEDED] = false
        }
    }

    suspend fun setSosPrefix(prefix: String) {
        val cleanPrefix = prefix.trim()
        dataStore.edit { preferences ->
            preferences[KEY_SOS_COMMAND_PREFIX] = cleanPrefix
            preferences[KEY_SOS_PREFIX_SHA256] = computeSha256(cleanPrefix)
            preferences[KEY_SOS_PREFIX_ROTATION_NEEDED] = false
        }
    }

    suspend fun setEmergencyAlertNumber(number: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_EMERGENCY_NUMBER] = number.trim()
        }
    }

    suspend fun setPrefixRotationNeeded(needed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_PREFIX_ROTATION_NEEDED] = needed
        }
    }

    suspend fun setSosTriggers(
        simRemoved: Boolean,
        shutdown: Boolean,
        smsCommands: Boolean,
        boot: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_TRIGGER_SIM_REMOVED] = simRemoved
            preferences[KEY_SOS_TRIGGER_SHUTDOWN] = shutdown
            preferences[KEY_SOS_TRIGGER_SMS_COMMANDS] = smsCommands
            preferences[KEY_SOS_TRIGGER_BOOT] = boot
        }
    }

    suspend fun setAutoTimeoutHours(hours: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_AUTO_TIMEOUT_HOURS] = hours.coerceIn(1, 24)
        }
    }

    suspend fun setTraceIntervalMins(mins: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_TRACE_INTERVAL_MINS] = mins.coerceIn(1, 60)
        }
    }

    suspend fun startSosSession(
        state: SosSessionState,
        targetNumber: String,
        intervalMins: Int = 5
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_SESSION_STATE] = state.name
            preferences[KEY_SOS_SESSION_START_TIME] = System.currentTimeMillis()
            preferences[KEY_SOS_ACTIVE_TARGET_NUMBER] = targetNumber
            preferences[KEY_SOS_TRACE_INTERVAL_MINS] = intervalMins
        }
    }

    suspend fun stopSosSession() {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_SESSION_STATE] = SosSessionState.NONE.name
            preferences[KEY_SOS_SESSION_START_TIME] = 0L
            preferences[KEY_SOS_ACTIVE_TARGET_NUMBER] = ""
        }
    }

    suspend fun recordSosTrigger(desc: String, timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_LAST_TRIGGERED_TIMESTAMP] = timestamp
            preferences[KEY_SOS_LAST_TRIGGER_DESC] = desc
        }
    }
}
