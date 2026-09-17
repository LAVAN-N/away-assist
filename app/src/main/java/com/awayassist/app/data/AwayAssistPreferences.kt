package com.awayassist.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

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

data class AppState(
    val isEnabled: Boolean = true,
    val currentMode: RingerState = RingerState.UNKNOWN,
    val lastChangedTimestamp: Long = 0L,
    val pauseUntilTimestamp: Long = 0L,
    val overrideMode: RingerState? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val isPaused: Boolean
        get() = pauseUntilTimestamp > System.currentTimeMillis()
}

class AwayAssistPreferences(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_IS_ENABLED = booleanPreferencesKey("is_enabled")
        val KEY_CURRENT_MODE = stringPreferencesKey("current_mode")
        val KEY_LAST_CHANGED_TIMESTAMP = longPreferencesKey("last_changed_timestamp")
        val KEY_PAUSE_UNTIL_TIMESTAMP = longPreferencesKey("pause_until_timestamp")
        val KEY_OVERRIDE_MODE = stringPreferencesKey("override_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

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

            AppState(
                isEnabled = isEnabled,
                currentMode = currentMode,
                lastChangedTimestamp = lastChanged,
                pauseUntilTimestamp = pauseUntil,
                overrideMode = overrideMode,
                themeMode = themeMode
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
            // Enabling Force Ring disables the automatic lock/unlock toggle
            preferences[KEY_IS_ENABLED] = false
            preferences[KEY_CURRENT_MODE] = RingerState.RING.name
            preferences[KEY_OVERRIDE_MODE] = RingerState.RING.name
            preferences[KEY_LAST_CHANGED_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun forceSilent() {
        dataStore.edit { preferences ->
            // Enabling Force Silent disables the automatic lock/unlock toggle
            preferences[KEY_IS_ENABLED] = false
            preferences[KEY_CURRENT_MODE] = RingerState.VIBRATE.name
            preferences[KEY_OVERRIDE_MODE] = RingerState.VIBRATE.name
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
            preferences[KEY_PAUSE_UNTIL_TIMESTAMP] = pauseUntil
        }
    }

    suspend fun clearPause() {
        dataStore.edit { preferences ->
            preferences[KEY_IS_ENABLED] = true
            preferences[KEY_PAUSE_UNTIL_TIMESTAMP] = 0L
            preferences.remove(KEY_OVERRIDE_MODE)
        }
    }
}
