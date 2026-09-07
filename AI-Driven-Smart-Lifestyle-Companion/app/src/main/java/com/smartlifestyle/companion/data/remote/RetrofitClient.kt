package com.smartlifestyle.companion.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    val weatherApi: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    /** Only used if BuildConfig.AI_API_KEY is set - see AiCoachRepository. Base URL
     * points at OpenAI's endpoint by default; swap it if you use a different
     * OpenAI-compatible provider. */
    val aiChatApi: AiChatApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiChatApiService::class.java)
    }
}
