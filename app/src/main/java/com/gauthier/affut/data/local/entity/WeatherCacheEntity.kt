package com.gauthier.affut.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** locationKey = latitude/longitude arrondies à 2 décimales (~1km), pour partager le cache entre points proches. */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationKey: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Double,
    val weatherCode: Int,
    val precipitationProbabilityNextHour: Int,
    val pressureTrendHpa3h: Double = 0.0,
    val fetchedAt: Long,
    /** Liste de DailyForecast sérialisée en JSON — simple, suffisant pour cette taille de données. */
    val dailyJson: String,
    /** Liste de HourlyForecast sérialisée en JSON (détail horaire). */
    val hourlyJson: String = "[]",
)
