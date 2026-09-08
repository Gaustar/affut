package com.gauthier.affut.data.remote.weather

import com.gauthier.affut.data.remote.weather.dto.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("hourly") hourly: String =
            "temperature_2m,apparent_temperature,precipitation,precipitation_probability,weathercode," +
                "windspeed_10m,windgusts_10m,winddirection_10m,cloudcover,relativehumidity_2m," +
                "pressure_msl,visibility,is_day",
        @Query("daily") daily: String =
            "temperature_2m_max,temperature_2m_min,precipitation_sum,windspeed_10m_max,winddirection_10m_dominant,sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 8,
    ): OpenMeteoResponse
}
