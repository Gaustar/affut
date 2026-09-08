package com.gauthier.affut.data.remote.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    @SerialName("current_weather") val currentWeather: CurrentWeatherDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null,
)

@Serializable
data class CurrentWeatherDto(
    val temperature: Double = 0.0,
    val windspeed: Double = 0.0,
    val winddirection: Double = 0.0,
    val weathercode: Int = 0,
)

@Serializable
data class HourlyDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("weathercode") val weatherCode: List<Int?> = emptyList(),
    @SerialName("windspeed_10m") val windSpeed: List<Double?> = emptyList(),
    @SerialName("windgusts_10m") val windGusts: List<Double?> = emptyList(),
    @SerialName("winddirection_10m") val windDirection: List<Double?> = emptyList(),
    @SerialName("cloudcover") val cloudCover: List<Int?> = emptyList(),
    @SerialName("relativehumidity_2m") val humidity: List<Int?> = emptyList(),
    @SerialName("pressure_msl") val pressureMsl: List<Double?> = emptyList(),
    val visibility: List<Double?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList(),
)

@Serializable
data class DailyDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @SerialName("windspeed_10m_max") val windSpeedMax: List<Double?> = emptyList(),
    @SerialName("winddirection_10m_dominant") val windDirectionDominant: List<Double?> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
)
