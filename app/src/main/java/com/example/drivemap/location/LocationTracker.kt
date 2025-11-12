package com.example.drivemap.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTracker(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun locations(): Flow<LocationSample> = callbackFlow {
        if (!hasPermission()) {
            close(IllegalStateException("Location permission missing"))
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toSample())
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_TIME_MS,
            MIN_DISTANCE_METERS,
            listener
        )

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun Location.toSample(): LocationSample = LocationSample(
        latitude = latitude,
        longitude = longitude,
        speedMps = if (hasSpeed()) speed.toDouble() else null,
        bearing = if (hasBearing()) bearing else null,
        timestampMillis = time
    )

    companion object {
        private const val MIN_TIME_MS = 1_000L
        private const val MIN_DISTANCE_METERS = 5f
    }
}
