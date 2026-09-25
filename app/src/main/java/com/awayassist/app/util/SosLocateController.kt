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
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.awayassist.app.data.AwayAssistPreferences
import com.awayassist.app.data.SosSessionState
import com.awayassist.app.data.computeSha256
import kotlinx.coroutines.Dispatchers
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

        val lastSpaceIndex = trimmed.lastIndexOf(' ')
        if (lastSpaceIndex <= 0) return null

        val prefixCandidate = trimmed.substring(0, lastSpaceIndex).trim()
        val commandStr = trimmed.substring(lastSpaceIndex + 1).trim().uppercase(Locale.ROOT)

        val command = try {
            SosCommand.valueOf(commandStr)
        } catch (e: IllegalArgumentException) {
            return null
        }

        return ParsedCommand(prefixCandidate = prefixCandidate, command = command)
    }

    fun verifyPrefix(candidate: String, storedSha256: String): Boolean {
        if (candidate.isBlank() || storedSha256.isBlank()) return false
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
            Log.d(TAG, "Prefix SHA-256 verification failed for incoming SMS command.")
            return@withContext false
        }

        Log.d(TAG, "Authenticated SMS command: ${parsed.command} from $senderNumber")

        // Prompt user to rotate prefix after any successful authentication
        preferences.setPrefixRotationNeeded(true)

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

    private suspend fun handleFind(senderNumber: String) {
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val batteryLevel = getBatteryPercentage()
        val location = acquireLocation(LOCATION_TIMEOUT_MS)

        val message = if (location != null) {
            "Away Assist: Location fix at $timeStr.\n" +
            "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
            "Accuracy: ~${location.accuracy.toInt()}m | Battery: $batteryLevel%"
        } else {
            val lastKnown = getLastKnownLocation()
            if (lastKnown != null) {
                "Away Assist: Last known location at $timeStr.\n" +
                "https://maps.google.com/?q=${lastKnown.latitude},${lastKnown.longitude}\n" +
                "Accuracy: ~${lastKnown.accuracy.toInt()}m | Battery: $batteryLevel%"
            } else {
                "Away Assist: Location fix unavailable at $timeStr.\nBattery: $batteryLevel%"
            }
        }

        sendSms(senderNumber, message)
        preferences.recordSosTrigger("FIND command from $senderNumber")
    }

    private suspend fun handleTrack(senderNumber: String, autoTimeoutHours: Int) {
        preferences.startSosSession(SosSessionState.TRACK, senderNumber)
        val message = "Away Assist: Location turned on. Use Find My Device or Maps to view. Reply STOP with your prefix to turn off, or it will auto-stop after ${autoTimeoutHours}h."
        sendSms(senderNumber, message)
        preferences.recordSosTrigger("TRACK started from $senderNumber")
    }

    private suspend fun handleTrace(senderNumber: String, intervalMins: Int, autoTimeoutHours: Int) {
        preferences.startSosSession(SosSessionState.TRACE, senderNumber, intervalMins)
        val ackMessage = "Away Assist: Tracing active. Sending updates every ${intervalMins}m. Reply STOP with your prefix to cancel, or it will auto-stop after ${autoTimeoutHours}h."
        sendSms(senderNumber, ackMessage)
        preferences.recordSosTrigger("TRACE started from $senderNumber")

        // Send initial fix immediately
        sendTraceFix(senderNumber)
    }

    private suspend fun handleStop(senderNumber: String) {
        preferences.stopSosSession()
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
        val location = acquireLocation(LOCATION_TIMEOUT_MS) ?: getLastKnownLocation()

        val locationText = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude} (~${location.accuracy.toInt()}m)"
        } else {
            "Location unavailable"
        }

        val message = "[Away Assist SOS] Alert: SIM card state changed at $timeStr!\n" +
            "Location: $locationText\n" +
            "Battery: $batteryLevel%"

        sendSms(targetNumber, message)
        preferences.recordSosTrigger("SIM state change detected")
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
        val lastKnown = getLastKnownLocation()

        val locationText = if (lastKnown != null) {
            "https://maps.google.com/?q=${lastKnown.latitude},${lastKnown.longitude} (~${lastKnown.accuracy.toInt()}m)"
        } else {
            "Location unavailable"
        }

        val message = "[Away Assist SOS] Alert: Device powering off at $timeStr.\n" +
            "Last location: $locationText\n" +
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

        val message = "[Away Assist SOS] Alert: Device powered on at $timeStr.\n" +
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
        val location = acquireLocation(LOCATION_TIMEOUT_MS) ?: getLastKnownLocation()

        val message = if (location != null) {
            "Away Assist TRACE: Location at $timeStr\n" +
            "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
            "Accuracy: ~${location.accuracy.toInt()}m | Battery: $batteryLevel%"
        } else {
            "Away Assist TRACE: Location update unavailable at $timeStr.\nBattery: $batteryLevel%"
        }

        sendSms(targetNumber, message)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    private suspend fun acquireLocation(timeoutMs: Long): Location? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission() || locationManager == null) return@withContext null

        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (hasGps) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0L,
                            0f,
                            listener,
                            Looper.getMainLooper()
                        )
                    }
                    if (hasNetwork) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            0L,
                            0f,
                            listener,
                            Looper.getMainLooper()
                        )
                    }

                    continuation.invokeOnCancellation {
                        locationManager.removeUpdates(listener)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request location updates", e)
                    locationManager.removeUpdates(listener)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission() || locationManager == null) return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                        bestLocation = loc
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking provider $provider", e)
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
