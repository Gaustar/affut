package com.gauthier.affut.data.mapper

import com.gauthier.affut.data.local.entity.WeatherCacheEntity
import com.gauthier.affut.data.remote.weather.dto.OpenMeteoResponse
import com.gauthier.affut.domain.model.DailyForecast
import com.gauthier.affut.domain.model.HourlyForecast
import com.gauthier.affut.domain.model.WeatherSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

private fun findCurrentHourIndex(times: List<String>): Int {
    if (times.isEmpty()) return 0
    val now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
    times.forEachIndexed { index, t ->
        val time = runCatching { LocalDateTime.parse(t) }.getOrNull() ?: return@forEachIndexed
        if (!time.isBefore(now)) return index
    }
    return 0
}

fun OpenMeteoResponse.toDomain(latitude: Double, longitude: Double): WeatherSnapshot {
    val current = currentWeather
    val hourIndex = findCurrentHourIndex(hourly?.time.orEmpty())
    val precipitationProbability = hourly?.precipitationProbability?.getOrNull(hourIndex) ?: 0

    val pressureNow = hourly?.pressureMsl?.getOrNull(hourIndex)
    val pressureEarlier = hourly?.pressureMsl?.getOrNull((hourIndex - 3).coerceAtLeast(0))
    val pressureTrend = if (pressureNow != null && pressureEarlier != null) pressureNow - pressureEarlier else 0.0

    val dailyList = daily?.time?.indices?.map { i ->
        DailyForecast(
            date = daily.time[i],
            tempMin = daily.temperatureMin.getOrNull(i) ?: 0.0,
            tempMax = daily.temperatureMax.getOrNull(i) ?: 0.0,
            precipitationSum = daily.precipitationSum.getOrNull(i) ?: 0.0,
            windSpeedMaxKmh = daily.windSpeedMax.getOrNull(i) ?: 0.0,
            windDirectionDominantDeg = daily.windDirectionDominant.getOrNull(i) ?: 0.0,
            sunrise = daily.sunrise.getOrElse(i) { "" },
            sunset = daily.sunset.getOrElse(i) { "" },
        )
    }.orEmpty()

    val hourlyList = hourly?.time?.indices?.drop(hourIndex)?.map { i ->
        HourlyForecast(
            time = hourly.time[i],
            temperature = hourly.temperature.getOrNull(i) ?: 0.0,
            apparentTemperature = hourly.apparentTemperature.getOrNull(i) ?: 0.0,
            precipitationMm = hourly.precipitation.getOrNull(i) ?: 0.0,
            precipitationProbability = hourly.precipitationProbability.getOrNull(i) ?: 0,
            weatherCode = hourly.weatherCode.getOrNull(i) ?: 0,
            windSpeedKmh = hourly.windSpeed.getOrNull(i) ?: 0.0,
            // Rafale absente (l'API en omet parfois) : on retombe sur le vent moyen, pas sur 0.
            windGustKmh = hourly.windGusts.getOrNull(i) ?: hourly.windSpeed.getOrNull(i) ?: 0.0,
            windDirectionDeg = hourly.windDirection.getOrNull(i) ?: 0.0,
            cloudCoverPercent = hourly.cloudCover.getOrNull(i) ?: 0,
            humidityPercent = hourly.humidity.getOrNull(i) ?: 0,
            visibilityMeters = hourly.visibility.getOrNull(i) ?: 0.0,
            isDay = (hourly.isDay.getOrNull(i) ?: 1) == 1,
        )
    }.orEmpty()

    return WeatherSnapshot(
        latitude = latitude,
        longitude = longitude,
        temperature = current?.temperature ?: 0.0,
        windSpeedKmh = current?.windspeed ?: 0.0,
        windDirectionDeg = current?.winddirection ?: 0.0,
        weatherCode = current?.weathercode ?: 0,
        precipitationProbabilityNextHour = precipitationProbability,
        pressureTrendHpa3h = pressureTrend,
        fetchedAt = System.currentTimeMillis(),
        daily = dailyList,
        hourly = hourlyList,
    )
}

fun WeatherSnapshot.toEntity(key: String): WeatherCacheEntity = WeatherCacheEntity(
    locationKey = key,
    latitude = latitude,
    longitude = longitude,
    temperature = temperature,
    windSpeedKmh = windSpeedKmh,
    windDirectionDeg = windDirectionDeg,
    weatherCode = weatherCode,
    precipitationProbabilityNextHour = precipitationProbabilityNextHour,
    pressureTrendHpa3h = pressureTrendHpa3h,
    fetchedAt = fetchedAt,
    dailyJson = Json.encodeToString(daily),
    hourlyJson = Json.encodeToString(hourly),
)

fun WeatherCacheEntity.toDomain(): WeatherSnapshot = WeatherSnapshot(
    latitude = latitude,
    longitude = longitude,
    temperature = temperature,
    windSpeedKmh = windSpeedKmh,
    windDirectionDeg = windDirectionDeg,
    weatherCode = weatherCode,
    precipitationProbabilityNextHour = precipitationProbabilityNextHour,
    pressureTrendHpa3h = pressureTrendHpa3h,
    fetchedAt = fetchedAt,
    daily = runCatching { Json.decodeFromString<List<DailyForecast>>(dailyJson) }.getOrDefault(emptyList()),
    hourly = runCatching { Json.decodeFromString<List<HourlyForecast>>(hourlyJson) }.getOrDefault(emptyList()),
)
