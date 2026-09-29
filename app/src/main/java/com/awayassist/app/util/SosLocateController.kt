package com.awayassist.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.awayassist.app.data.AwayAssistPreferences
import com.awayassist.app.data.SosSessionState
import com.awayassist.app.data.computeSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class SosLocateController(private val context: Context) {

    private val preferences = AwayAssistPreferences.getInstance(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    companion object {
        private const val TAG = "SosLocateController"
        private const val LOCATION_TIMEOUT_MS = 20_000L
    }

    enum class SosCommand {
        FIND,
        TRACK,
        TRACE,
        STOP
    }

    data class ParsedCommand(
        val prefixCandidate: String,
        val command: SosCommand
    )

    fun parseMessage(body: String): ParsedCommand? {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return null

        for (cmd in SosCommand.values()) {
            val cmdName = cmd.name
            if (trimmed.equals(cmdName, ignoreCase = true)) {
                return ParsedCommand(prefixCandidate = "", command = cmd)
            }
            if (trimmed.endsWith(cmdName, ignoreCase = true)) {
                val candidate = trimmed.substring(0, trimmed.length - cmdName.length).trim()
                return ParsedCommand(prefixCandidate = candidate, command = cmd)
            }
        }

        return null
    }

    fun verifyPrefix(candidate: String, storedSha256: String): Boolean {
        // If no prefix is configured yet, allow commands without prefix
        if (storedSha256.isBlank()) return true
        if (candidate.isBlank()) return false
        val candidateHash = computeSha256(candidate.trim())
        return MessageDigest.isEqual(
            candidateHash.toByteArray(Charsets.UTF_8),
            storedSha256.toByteArray(Charsets.UTF_8)
        )
    }

    suspend fun handleSmsCommand(senderNumber: String, messageBody: String): Boolean = withContext(Dispatchers.IO) {
        val appState = preferences.getAppState()
        val sosState = appState.sosLocateState

        if (!sosState.isSosEnabled || !sosState.triggerSmsCommands) {
            Log.d(TAG, "SOS Locate or SMS command trigger disabled.")
            return@withContext false
        }

        val parsed = parseMessage(messageBody) ?: return@withContext false
        if (!verifyPrefix(parsed.prefixCandidate, sosState.prefixSha256)) {
            Log.d(TAG, "Prefix SHA-256 verification failed for incoming SMS command: '$messageBody'")
            return@withContext false
        }

        Log.d(TAG, "Authenticated SMS command: ${parsed.command} from $senderNumber")

        // Prompt user to rotate prefix after any successful authentication
        if (sosState.prefixSha256.isNotBlank()) {
            preferences.setPrefixRotationNeeded(true)
        }

        when (parsed.command) {
            SosCommand.FIND -> {
                handleFind(senderNumber)
            }
            SosCommand.TRACK -> {
                handleTrack(senderNumber, sosState.autoTimeoutHours)
            }
            SosCommand.TRACE -> {
                handleTrace(senderNumber, sosState.traceIntervalMins, sosState.autoTimeoutHours)
            }
            SosCommand.STOP -> {
                handleStop(senderNumber)
            }
        }

        return@withContext true
    }

    fun hasWriteSecureSettingsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            rikka.shizuku.Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun isShizukuPermissionGranted(): Boolean {
        return try {
            if (!isShizukuAvailable()) false
            else rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    private fun runShizukuCommand(command: Array<String>): Pair<Int, String> {
        return try {
            val method = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, command, null, null) as java.lang.Process

            val outputFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                try {
                    val out = process.inputStream.bufferedReader().readText()
                    val err = process.errorStream.bufferedReader().readText()
                    (out + "\n" + err).trim()
                } catch (e: Exception) {
                    ""
                }
            }

            var exitCode = -1
            val exitFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                try {
                    process.waitFor()
                } catch (e: Exception) {
                    -1
                }
            }

            try {
                exitCode = exitFuture.get(2, java.util.concurrent.TimeUnit.SECONDS)
            } catch (e: Exception) {
                process.destroy()
            }

            val output = try {
                outputFuture.get(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                ""
            }

            Log.d(TAG, "Shizuku '${command.joinToString(" ")}' exit: $exitCode, out: $output")
            Pair(exitCode, output)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed running Shizuku command: ${command.joinToString(" ")}", e)
            Pair(-1, e.message ?: "Exception")
        }
    }

    fun grantWriteSecureSettingsViaShizuku(): Pair<Boolean, String> {
        if (!isShizukuAvailable()) return Pair(false, "Shizuku service is not running.")
        if (!isShizukuPermissionGranted()) return Pair(false, "Shizuku permission not authorized.")

        val pkg = context.packageName
        val perm = Manifest.permission.WRITE_SECURE_SETTINGS

        // Attempt 1: sh -c "pm grant ..."
        var res = runShizukuCommand(arrayOf("sh", "-c", "pm grant $pkg $perm"))

        // Attempt 2: cmd package grant ...
        if (!hasWriteSecureSettingsPermission()) {
            val res2 = runShizukuCommand(arrayOf("cmd", "package", "grant", pkg, perm))
            if (res2.second.isNotBlank()) res = res2
        }

        // Attempt 3: pm grant ...
        if (!hasWriteSecureSettingsPermission()) {
            val res3 = runShizukuCommand(arrayOf("pm", "grant", pkg, perm))
            if (res3.second.isNotBlank()) res = res3
        }

        val granted = hasWriteSecureSettingsPermission()
        return Pair(granted, res.second)
    }

    fun enableSystemLocation(): Boolean {
        var success = false
        if (hasWriteSecureSettingsPermission()) {
            try {
                @Suppress("DEPRECATION")
                Settings.Secure.putInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
                )
                @Suppress("DEPRECATION")
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                    "+gps,+network"
                )
                Log.d(TAG, "Successfully enabled system master location switch via WRITE_SECURE_SETTINGS.")
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable system location via ContentResolver", e)
            }
        }
        if (isShizukuPermissionGranted()) {
            runShizukuCommand(arrayOf("sh", "-c", "cmd location set-location-enabled true && settings put secure location_mode 3 && settings put secure location_providers_allowed +gps,+network"))
            success = true
        }
        return success
    }

    fun disableSystemLocation(): Boolean {
        var success = false
        if (hasWriteSecureSettingsPermission()) {
            try {
                @Suppress("DEPRECATION")
                Settings.Secure.putInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
                )
                @Suppress("DEPRECATION")
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                    "-gps,-network"
                )
                Log.d(TAG, "Successfully disabled system master location switch via WRITE_SECURE_SETTINGS.")
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable system location via ContentResolver", e)
            }
        }
        if (isShizukuPermissionGranted()) {
            runShizukuCommand(arrayOf("sh", "-c", "cmd location set-location-enabled false && settings put secure location_mode 0 && settings put secure location_providers_allowed -gps,-network"))
            success = true
        }
        return success
    }

    fun isMobileDataEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                tm?.isDataEnabled ?: (Settings.Global.getInt(context.contentResolver, "mobile_data", 0) == 1)
            } else {
                Settings.Global.getInt(context.contentResolver, "mobile_data", 0) == 1
            }
        } catch (e: Exception) {
            false
        }
    }

    fun enableMobileData(): Boolean {
        var success = false
        if (hasWriteSecureSettingsPermission()) {
            try {
                Settings.Global.putInt(context.contentResolver, "mobile_data", 1)
                Settings.Global.putInt(context.contentResolver, "mobile_data0", 1)
                Settings.Global.putInt(context.contentResolver, "mobile_data1", 1)
                Log.d(TAG, "Set mobile_data flags in Settings.Global via WRITE_SECURE_SETTINGS.")
                success = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enable mobile data via ContentResolver", e)
            }
        }
        // Always execute direct telephony modem commands via Shizuku if available
        if (isShizukuPermissionGranted()) {
            val cmd = runShizukuCommand(arrayOf("sh", "-c", "svc data enable && cmd telephony set-data-enabled true && cmd phone set-data-enabled true && settings put global mobile_data 1"))
            if (cmd.first == 0) {
                success = true
                Log.d(TAG, "Executed cellular mobile data enable commands via Shizuku")
            }
        }
        return success
    }

    fun disableMobileData(): Boolean {
        var success = false
        if (hasWriteSecureSettingsPermission()) {
            try {
                Settings.Global.putInt(context.contentResolver, "mobile_data", 0)
                Settings.Global.putInt(context.contentResolver, "mobile_data0", 0)
                Settings.Global.putInt(context.contentResolver, "mobile_data1", 0)
                Log.d(TAG, "Disabled mobile_data flags in Settings.Global via WRITE_SECURE_SETTINGS.")
                success = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to disable mobile data via ContentResolver", e)
            }
        }
        if (isShizukuPermissionGranted()) {
            runShizukuCommand(arrayOf("sh", "-c", "svc data disable && cmd telephony set-data-enabled false && cmd phone set-data-enabled false && settings put global mobile_data 0"))
            success = true
        }
        return success
    }

    fun isLocationEnabled(): Boolean {
        if (locationManager == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private suspend fun handleFind(senderNumber: String) {
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val batteryLevel = getBatteryPercentage()
        val wasLocationOff = !isLocationEnabled()
        val wasDataOff = !isMobileDataEnabled()
        var toggledLocOn = false

        try {
            if (hasWriteSecureSettingsPermission() || isShizukuPermissionGranted()) {
                if (wasLocationOff) {
                    toggledLocOn = enableSystemLocation()
                }
                if (wasDataOff) {
                    enableMobileData()
                }
                if (toggledLocOn || wasDataOff) {
                    delay(1200L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in auto-toggling location/data", e)
        }

        // Generous 15s timeout with multi-provider + cached fallbacks to guarantee a valid location URL
        val locationResult = getBestAvailableLocation(timeoutMs = 15_000L, quickAttempt = true)

        // Turn location back OFF immediately if we turned it on for this single fix
        if (toggledLocOn) {
            try {
                disableSystemLocation()
            } catch (e: Exception) {
                Log.e(TAG, "Error disabling location in handleFind", e)
            }
        }

        val message = "Away Assist FIND: Location at $timeStr\n" +
            "${locationResult.text}\n" +
            "Battery: $batteryLevel%"

        sendSms(senderNumber, message)
        preferences.recordSosTrigger("FIND command from $senderNumber")
    }

    private suspend fun handleTrack(senderNumber: String, autoTimeoutHours: Int) {
        val prevSession = preferences.getAppState().sosLocateState.sessionState
        if (hasWriteSecureSettingsPermission() || isShizukuPermissionGranted()) {
            if (!isLocationEnabled()) enableSystemLocation()
            if (!isMobileDataEnabled()) enableMobileData()
        }
        preferences.startSosSession(SosSessionState.TRACK, senderNumber)
        val isLocOn = isLocationEnabled()
        val switchDesc = when (prevSession) {
            SosSessionState.TRACE -> "Switched from TRACE to continuous TRACK mode. "
            SosSessionState.TRACK -> "Continuous TRACK mode refreshed. "
            else -> ""
        }
        val note = if (!isLocOn) " (Location toggle is OFF; grant WRITE_SECURE_SETTINGS via ADB for remote ON)" else ""
        val message = "Away Assist: ${switchDesc}Location & Mobile Data enabled$note. Use Google Find My Device or Maps to view. Reply STOP to turn off, or auto-stops after ${autoTimeoutHours}h."
        sendSms(senderNumber, message)
        preferences.recordSosTrigger("TRACK started from $senderNumber")
    }

    private suspend fun handleTrace(senderNumber: String, intervalMins: Int, autoTimeoutHours: Int) {
        val prevSession = preferences.getAppState().sosLocateState.sessionState
        if (hasWriteSecureSettingsPermission() || isShizukuPermissionGranted()) {
            if (!isMobileDataEnabled()) enableMobileData()
        }
        preferences.startSosSession(SosSessionState.TRACE, senderNumber, intervalMins)
        val switchDesc = when (prevSession) {
            SosSessionState.TRACK -> "Switched from TRACK to periodic TRACE mode (${intervalMins}m intervals). "
            SosSessionState.TRACE -> "TRACE mode interval updated to ${intervalMins}m. "
            else -> ""
        }
        val ackMessage = "Away Assist: ${switchDesc}Sending location updates every ${intervalMins}m (Location turns on before each fix and turns off immediately after). Reply STOP to cancel, or auto-stops after ${autoTimeoutHours}h."
        sendSms(senderNumber, ackMessage)
        preferences.recordSosTrigger("TRACE started from $senderNumber")

        // Send initial fix immediately (turns ON location, acquires, sends SMS, and turns OFF location)
        sendTraceFix(senderNumber)
    }

    private suspend fun handleStop(senderNumber: String) {
        preferences.stopSosSession()
        if (hasWriteSecureSettingsPermission() || isShizukuPermissionGranted()) {
            disableSystemLocation()
        }
        val message = "Away Assist: Tracking stopped, location turned off."
        sendSms(senderNumber, message)
        preferences.recordSosTrigger("STOP command received from $senderNumber")
    }

    suspend fun handleSimStateChanged() = withContext(Dispatchers.IO) {
        val appState = preferences.getAppState()
        val sosState = appState.sosLocateState

        if (!sosState.isSosEnabled || !sosState.triggerSimRemoved) {
            return@withContext
        }

        val targetNumber = sosState.emergencyAlertNumber
        if (targetNumber.isBlank()) return@withContext

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val batteryLevel = getBatteryPercentage()

        val locationResult = getBestAvailableLocation(timeoutMs = 8_000L, quickAttempt = true)

        val message = "[Away Assist SOS] Alert: SIM card removed at $timeStr!\n" +
            "Location: ${locationResult.text}\n" +
            "Battery: $batteryLevel%"

        sendSms(targetNumber, message)
        preferences.recordSosTrigger("SIM removal detected")
    }

    suspend fun handleShutdown() = withContext(Dispatchers.IO) {
        val appState = preferences.getAppState()
        val sosState = appState.sosLocateState

        if (!sosState.isSosEnabled || !sosState.triggerShutdown) {
            return@withContext
        }

        val targetNumber = sosState.emergencyAlertNumber
        if (targetNumber.isBlank()) return@withContext

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val batteryLevel = getBatteryPercentage()

        // Quick 1.5s best-effort location attempt before shutdown kills processes
        val locationResult = getBestAvailableLocation(timeoutMs = 1_500L, quickAttempt = true)

        val message = "[Away Assist SOS] Alert: Device powering off at $timeStr.\n" +
            "Last location: ${locationResult.text}\n" +
            "Battery: $batteryLevel%"

        sendSms(targetNumber, message)
        preferences.recordSosTrigger("Shutdown trigger fired")
    }

    suspend fun handleBoot() = withContext(Dispatchers.IO) {
        val appState = preferences.getAppState()
        val sosState = appState.sosLocateState

        if (!sosState.isSosEnabled || !sosState.triggerBoot) {
            return@withContext
        }

        val targetNumber = sosState.emergencyAlertNumber
        if (targetNumber.isBlank()) return@withContext

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val batteryLevel = getBatteryPercentage()

        val locationResult = getBestAvailableLocation(timeoutMs = 5_000L, quickAttempt = true)

        val message = "[Away Assist SOS] Alert: Device powered on at $timeStr.\n" +
            "Location: ${locationResult.text}\n" +
            "Battery: $batteryLevel%"

        sendSms(targetNumber, message)
        preferences.recordSosTrigger("Device boot detected")
    }

    suspend fun checkSessionTimeoutAndExecuteTraceTick() = withContext(Dispatchers.IO) {
        val appState = preferences.getAppState()
        val sosState = appState.sosLocateState

        if (sosState.sessionState == SosSessionState.NONE) return@withContext

        val now = System.currentTimeMillis()
        val elapsedMs = now - sosState.sessionStartTime
        val timeoutMs = sosState.autoTimeoutHours * 3600_000L

        if (elapsedMs >= timeoutMs) {
            Log.d(TAG, "SOS session timed out after ${sosState.autoTimeoutHours} hours.")
            preferences.stopSosSession()
            if (sosState.activeTargetNumber.isNotBlank()) {
                val timeoutMsg = "Away Assist: Active tracking session timed out after ${sosState.autoTimeoutHours}h. Tracking stopped."
                sendSms(sosState.activeTargetNumber, timeoutMsg)
            }
            preferences.recordSosTrigger("Auto-timeout reached (${sosState.autoTimeoutHours}h)")
            return@withContext
        }

        if (sosState.sessionState == SosSessionState.TRACE && sosState.activeTargetNumber.isNotBlank()) {
            sendTraceFix(sosState.activeTargetNumber)
        }
    }

    private suspend fun sendTraceFix(targetNumber: String) {
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val batteryLevel = getBatteryPercentage()
        var toggledLocOn = false

        try {
            if (hasWriteSecureSettingsPermission() || isShizukuPermissionGranted()) {
                if (!isLocationEnabled()) {
                    toggledLocOn = enableSystemLocation()
                }
                if (!isMobileDataEnabled()) {
                    enableMobileData()
                }
                if (toggledLocOn) {
                    delay(1200L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling location in sendTraceFix", e)
        }

        val locationResult = getBestAvailableLocation(timeoutMs = 15_000L, quickAttempt = true)

        // TRACE requirement: Always turn location OFF immediately after sending each periodic update!
        if (hasWriteSecureSettingsPermission() || isShizukuPermissionGranted()) {
            try {
                disableSystemLocation()
            } catch (e: Exception) {
                Log.e(TAG, "Error disabling location after trace fix", e)
            }
        }

        val message = "Away Assist TRACE: Location at $timeStr\n" +
            "${locationResult.text}\n" +
            "Battery: $batteryLevel%"

        sendSms(targetNumber, message)
    }

    data class FormattedLocation(
        val text: String,
        val location: Location? = null
    )

    private suspend fun getBestAvailableLocation(
        timeoutMs: Long = 15_000L,
        quickAttempt: Boolean = true
    ): FormattedLocation {
        var location: Location? = null
        if (quickAttempt && hasLocationPermission()) {
            location = try {
                acquireLocation(timeoutMs)
            } catch (e: Exception) {
                Log.w(TAG, "Error acquiring location in getBestAvailableLocation", e)
                null
            }
        }

        if (location == null) {
            location = getLastKnownLocation()
        }

        if (location != null) {
            preferences.saveLastKnownLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = if (location.hasAccuracy()) location.accuracy else 0f,
                timestamp = System.currentTimeMillis()
            )
            val accuracyStr = if (location.hasAccuracy()) " (~${location.accuracy.toInt()}m)" else ""
            return FormattedLocation(
                text = "https://maps.google.com/?q=${location.latitude},${location.longitude}$accuracyStr",
                location = location
            )
        }

        // Fallback to persisted location in DataStore
        val appState = preferences.getAppState()
        val sosState = appState.sosLocateState
        if (sosState.lastKnownLatitude != 0.0 && sosState.lastKnownLongitude != 0.0) {
            val timeAgoStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(sosState.lastKnownLocationTime))
            val accStr = if (sosState.lastKnownAccuracy > 0f) " (~${sosState.lastKnownAccuracy.toInt()}m)" else ""
            return FormattedLocation(
                text = "https://maps.google.com/?q=${sosState.lastKnownLatitude},${sosState.lastKnownLongitude}$accStr (cached from $timeAgoStr)",
                location = null
            )
        }

        return FormattedLocation(
            text = "Location unavailable (GPS/Network fix not acquired)",
            location = null
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun acquireLocation(timeoutMs: Long = 15_000L): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission() || locationManager == null) return@withContext null

        // 1. If we have a very fresh location (< 10s old), return immediately
        val initialBest = getLastKnownLocation()
        if (initialBest != null && (System.currentTimeMillis() - initialBest.time) < 10_000L) {
            return@withContext initialBest
        }

        // 2. Try modern Android R+ LocationManager.getCurrentLocation if available
        var modernLocation: Location? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                modernLocation = withTimeoutOrNull(timeoutMs) {
                    suspendCancellableCoroutine { continuation ->
                        val cancellationSignal = android.os.CancellationSignal()
                        continuation.invokeOnCancellation { cancellationSignal.cancel() }
                        try {
                            val provider = when {
                                locationManager.isProviderEnabled("fused") -> "fused"
                                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                                else -> LocationManager.PASSIVE_PROVIDER
                            }
                            locationManager.getCurrentLocation(
                                provider,
                                cancellationSignal,
                                context.mainExecutor
                            ) { loc ->
                                if (continuation.isActive) {
                                    continuation.resume(loc)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "getCurrentLocation failed, falling back to listener", e)
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in modern getCurrentLocation", e)
            }
        }

        if (modernLocation != null) {
            return@withContext modernLocation
        }

        // 3. Fallback to active multi-provider LocationListener with full timeout
        val freshLocation = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        try {
                            locationManager.removeUpdates(this)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error removing location updates", e)
                        }
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                var registered = false
                try {
                    val providers = listOf(
                        "fused",
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER
                    )

                    for (p in providers) {
                        if (locationManager.isProviderEnabled(p)) {
                            try {
                                locationManager.requestLocationUpdates(
                                    p,
                                    0L,
                                    0f,
                                    listener,
                                    Looper.getMainLooper()
                                )
                                registered = true
                            } catch (_: Exception) {}
                        }
                    }

                    if (!registered) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    } else {
                        continuation.invokeOnCancellation {
                            try {
                                locationManager.removeUpdates(listener)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error removing location listener on cancellation", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request location updates", e)
                    try {
                        locationManager.removeUpdates(listener)
                    } catch (ignored: Exception) {}
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }

        return@withContext freshLocation ?: getLastKnownLocation() ?: initialBest
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission() || locationManager == null) return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            "fused"
        )
        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null) {
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            } catch (e: Exception) {
                // Ignore provider error
            }
        }
        return bestLocation
    }

    private fun sendSms(destinationAddress: String, message: String) {
        if (destinationAddress.isBlank() || message.isBlank()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot send SMS: SEND_SMS permission not granted.")
            return
        }

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(destinationAddress, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(destinationAddress, null, message, null, null)
            }
            Log.d(TAG, "SMS dispatched to $destinationAddress")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $destinationAddress", e)
        }
    }

    private fun getBatteryPercentage(): Int {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not retrieve battery level", e)
            -1
        }
    }
}
