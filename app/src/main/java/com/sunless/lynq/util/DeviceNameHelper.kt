package com.sunless.lynq.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Resolves the device name with preference for the user's configured Quick Share / Bluetooth / system name.
 * Also provides persistence so the user can customize their display name directly inside Lynq.
 */
object DeviceNameHelper {

    private const val PREFS_NAME = "lynq_user_prefs"
    private const val KEY_CUSTOM_DEVICE_NAME = "custom_device_name"

    fun getDeviceName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_CUSTOM_DEVICE_NAME, null)?.trim()
        if (!savedName.isNullOrBlank()) {
            return savedName
        }

        // Try reading Quick Share / Android System / Bluetooth device name
        val detected = resolveSystemDeviceName(context)
        return if (!detected.isNullOrBlank()) {
            detected
        } else {
            "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        }
    }

    fun setCustomDeviceName(context: Context, newName: String) {
        val clean = newName.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (clean.isNotBlank()) {
            prefs.edit().putString(KEY_CUSTOM_DEVICE_NAME, clean).apply()
        } else {
            prefs.edit().remove(KEY_CUSTOM_DEVICE_NAME).apply()
        }
    }

    fun isCustomNameSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_CUSTOM_DEVICE_NAME, null).isNullOrBlank()
    }

    fun getSystemDetectedName(context: Context): String {
        return resolveSystemDeviceName(context)
            ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    private fun resolveSystemDeviceName(context: Context): String? {
        val resolver = context.contentResolver

        // 1. Settings.Global.DEVICE_NAME (used by Quick Share, Bluetooth, and About Phone on Android 7.1+)
        try {
            val globalName = Settings.Global.getString(resolver, Settings.Global.DEVICE_NAME)?.trim()
            if (isValidDeviceName(globalName)) return globalName
        } catch (_: Exception) {}

        try {
            val globalName = Settings.Global.getString(resolver, "device_name")?.trim()
            if (isValidDeviceName(globalName)) return globalName
        } catch (_: Exception) {}

        // 2. BluetoothAdapter name (often set by Quick Share / Bluetooth settings)
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val btAdapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (btAdapter != null) {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                } else true

                if (hasPermission) {
                    val btName = btAdapter.name?.trim()
                    if (isValidDeviceName(btName)) return btName
                }
            }
        } catch (_: Exception) {}

        // 3. Settings.Secure or System bluetooth_name
        try {
            val secName = Settings.Secure.getString(resolver, "bluetooth_name")?.trim()
            if (isValidDeviceName(secName)) return secName
        } catch (_: Exception) {}

        try {
            val sysName = Settings.System.getString(resolver, "bluetooth_name")?.trim()
            if (isValidDeviceName(sysName)) return sysName
        } catch (_: Exception) {}

        return null
    }

    private fun isValidDeviceName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        // Filter out temporary Nearby Connections advertisement base64 hashes (e.g. starting with "Il", "Ij", "Ik")
        if (name.length > 20 && (name.startsWith("Il") || name.startsWith("Ij") || name.startsWith("Ik")) && !name.contains(" ")) {
            return false
        }
        return true
    }
}
