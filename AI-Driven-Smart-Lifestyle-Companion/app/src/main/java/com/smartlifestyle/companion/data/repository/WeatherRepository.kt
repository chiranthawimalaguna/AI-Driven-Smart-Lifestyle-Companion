package com.smartlifestyle.companion.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.smartlifestyle.companion.BuildConfig
import com.smartlifestyle.companion.data.remote.RetrofitClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class WeatherContext(
    val temperatureCelsius: Float?,
    val isRaining: Boolean
)

/**
 * Fetches current weather for the user's location. Never throws out to the caller -
 * on any failure (no location permission, no network, API error) it returns a neutral
 * WeatherContext(null, false) so the recommendation engine degrades gracefully instead
 * of crashing the dashboard refresh.
 */
class WeatherRepository(private val context: Context) {

    @SuppressLint("MissingPermission") // caller is responsible for checking permission first
    suspend fun getCurrentWeather(hasLocationPermission: Boolean): WeatherContext {
        if (!hasLocationPermission) return WeatherContext(null, false)

        return try {
            val location = getLastLocation() ?: return WeatherContext(null, false)
            val response = RetrofitClient.weatherApi.getCurrentWeather(
                lat = location.first,
                lon = location.second,
                apiKey = BuildConfig.WEATHER_API_KEY
            )
            val isRaining = response.weather.any {
                it.main.equals("Rain", ignoreCase = true) || it.main.equals("Drizzle", ignoreCase = true)
            }
            WeatherContext(temperatureCelsius = response.main.temp, isRaining = isRaining)
        } catch (e: Exception) {
            // Network/API failure - the app should still work with everything else.
            WeatherContext(null, false)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(location.latitude to location.longitude)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { continuation.resume(null) }
        }
}
