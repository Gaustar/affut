package com.gauthier.affut.domain.model

import kotlinx.serialization.Serializable

data class WeatherSnapshot(
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Double,
    val weatherCode: Int,
    val precipitationProbabilityNextHour: Int,
    /** Variation de pression (hPa) sur les 3 dernières heures. Négatif = pression en baisse. */
    val pressureTrendHpa3h: Double = 0.0,
    val fetchedAt: Long,
    /** true quand la donnée vient du cache après un échec réseau (et non d'une récupération fraîche). */
    val isStale: Boolean = false,
    val daily: List<DailyForecast>,
    /** Détail heure par heure à partir de l'heure courante (bivouac, sortie de nuit…). */
    val hourly: List<HourlyForecast> = emptyList(),
)

@Serializable
data class HourlyForecast(
    /** Format ISO local, ex: "2026-09-07T18:00". */
    val time: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val precipitationMm: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val windSpeedKmh: Double,
    val windGustKmh: Double,
    val windDirectionDeg: Double,
    val cloudCoverPercent: Int,
    val humidityPercent: Int,
    val visibilityMeters: Double,
    val isDay: Boolean,
)

@Serializable
data class DailyForecast(
    val date: String,
    val tempMin: Double,
    val tempMax: Double,
    val precipitationSum: Double,
    val windSpeedMaxKmh: Double,
    val windDirectionDominantDeg: Double,
    val sunrise: String,
    val sunset: String,
)
