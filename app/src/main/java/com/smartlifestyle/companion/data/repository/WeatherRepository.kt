package com.smartlifestyle.companion.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.smartlifestyle.companion.BuildConfig
import com.smartlifestyle.companion.data.remote.RetrofitClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class WeatherContext(
    val temperatureCelsius: Float?,
    val isRaining: Boolean
)

class WeatherRepository(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentWeather(hasLocationPermission: Boolean): WeatherContext {
        if (!hasLocationPermission) return WeatherContext(null, false)

        return try {
            val location = getFreshLocation() ?: getLastLocation() ?: return WeatherContext(null, false)
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
            WeatherContext(null, false)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cancellationSource = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellationSource.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationSource.token)
                .addOnSuccessListener { location ->
                    if (location != null) continuation.resume(location.latitude to location.longitude)
                    else continuation.resume(null)
                }
                .addOnFailureListener { continuation.resume(null) }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) continuation.resume(location.latitude to location.longitude)
                    else continuation.resume(null)
                }
                .addOnFailureListener { continuation.resume(null) }
        }
}