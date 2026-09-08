package com.gauthier.affut.data.remote.weather

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import retrofit2.Retrofit

object OpenMeteoClient {
    private val json = Json { ignoreUnknownKeys = true }

    val api: OpenMeteoApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(json.asConverterFactory(MediaType.parse("application/json")!!))
        .build()
        .create(OpenMeteoApi::class.java)
}
