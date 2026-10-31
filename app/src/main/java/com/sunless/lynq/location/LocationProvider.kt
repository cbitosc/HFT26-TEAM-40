package com.sunless.lynq.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Safe, non-blocking GPS provider for emergency SOS broadcasts.
 * Never blocks SOS if permissions are missing or GPS is slow.
 */
class LocationProvider(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val hasLocationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getEmergencyCoordinates(): String? {
        if (!hasLocationPermission) return null

        return try {
            withTimeoutOrNull(2500) {
                val cts = CancellationTokenSource()
                val location = fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token,
                ).awaitTask()

                if (location != null) {
                    val latStr = "%.4f° %s".format(
                        Math.abs(location.latitude),
                        if (location.latitude >= 0) "N" else "S",
                    )
                    val lonStr = "%.4f° %s".format(
                        Math.abs(location.longitude),
                        if (location.longitude >= 0) "E" else "W",
                    )
                    val accStr = "±${location.accuracy.toInt()}m"
                    "$latStr, $lonStr ($accStr)"
                } else {
                    val lastLoc: Location? = fusedClient.lastLocation.awaitTask()
                    lastLoc?.let {
                        "%.4f°, %.4f°".format(it.latitude, it.longitude)
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { exception -> cont.resumeWithException(exception) }
        addOnCanceledListener { cont.cancel() }
    }
}
