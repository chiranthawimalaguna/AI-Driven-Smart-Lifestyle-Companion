package com.smartlifestyle.companion.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(
    val main: MainWeather,
    val weather: List<WeatherCondition>
)
data class MainWeather(val temp: Float)
data class WeatherCondition(val main: String, val description: String)

interface WeatherApiService {
    // BuildConfig.WEATHER_API_KEY is injected from local.properties - never hard-code the key.
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}
