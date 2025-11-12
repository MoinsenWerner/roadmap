package com.example.drivemap.location

import android.content.Context
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.drivemap.data.GeocodeResult
import com.example.drivemap.model.LatLon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class GeocodeResolver(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    suspend fun resolve(location: LatLon): GeocodeResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable() || !Geocoder.isPresent()) {
            return@withContext GeocodeResult.None
        }

        return@withContext runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (results.isNullOrEmpty()) {
                GeocodeResult.None
            } else {
                val address = results.first()
                GeocodeResult.Details(
                    street = address.thoroughfare ?: address.featureName,
                    locality = address.locality ?: address.subAdminArea
                )
            }
        }.getOrElse { GeocodeResult.None }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
