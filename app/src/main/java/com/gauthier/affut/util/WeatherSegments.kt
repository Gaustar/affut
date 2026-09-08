package com.gauthier.affut.util

import com.gauthier.affut.domain.model.HourlyForecast
import java.time.LocalDate
import java.time.LocalDateTime

enum class DayPart(val label: String) {
    MATIN("Matin"),
    APRES_MIDI("Après-midi"),
    NUIT("Nuit"),
}

/** Météo agrégée sur une tranche de la journée — ce qu'il faut pour décider d'un affût ou d'un bivouac. */
data class SegmentForecast(
    val date: LocalDate,
    val part: DayPart,
    val tempMin: Double,
    val tempMax: Double,
    val apparentTempMin: Double,
    val precipitationMm: Double,
    val precipitationProbabilityMax: Int,
    val windSpeedAvgKmh: Double,
    val windGustMaxKmh: Double,
    /** Direction relevée à l'heure la plus ventée de la tranche. */
    val windDirectionDeg: Double,
    val cloudCoverAvgPercent: Int,
    val humidityAvgPercent: Int,
    val weatherCode: Int,
)

/**
 * Regroupe les heures en tranches Matin (6h-12h), Après-midi (12h-18h) et Nuit (18h-6h).
 * Les heures de 0h à 6h sont rattachées à la nuit de la veille : "la nuit du 7" désigne
 * bien la nuit qu'on passe dehors du 7 au soir au 8 au matin.
 */
fun buildSegments(hourly: List<HourlyForecast>): List<SegmentForecast> {
    val grouped = LinkedHashMap<Pair<LocalDate, DayPart>, MutableList<HourlyForecast>>()

    hourly.forEach { entry ->
        val dateTime = runCatching { LocalDateTime.parse(entry.time) }.getOrNull() ?: return@forEach
        val hour = dateTime.hour
        val key = when {
            hour in 6..11 -> dateTime.toLocalDate() to DayPart.MATIN
            hour in 12..17 -> dateTime.toLocalDate() to DayPart.APRES_MIDI
            hour >= 18 -> dateTime.toLocalDate() to DayPart.NUIT
            else -> dateTime.toLocalDate().minusDays(1) to DayPart.NUIT
        }
        grouped.getOrPut(key) { mutableListOf() }.add(entry)
    }

    return grouped.map { (key, entries) ->
        val windiest = entries.maxBy { it.windSpeedKmh }
        SegmentForecast(
            date = key.first,
            part = key.second,
            tempMin = entries.minOf { it.temperature },
            tempMax = entries.maxOf { it.temperature },
            apparentTempMin = entries.minOf { it.apparentTemperature },
            precipitationMm = entries.sumOf { it.precipitationMm },
            precipitationProbabilityMax = entries.maxOf { it.precipitationProbability },
            windSpeedAvgKmh = entries.map { it.windSpeedKmh }.average(),
            windGustMaxKmh = entries.maxOf { it.windGustKmh },
            windDirectionDeg = windiest.windDirectionDeg,
            cloudCoverAvgPercent = entries.map { it.cloudCoverPercent }.average().toInt(),
            humidityAvgPercent = entries.map { it.humidityPercent }.average().toInt(),
            // Code météo le plus "marquant" de la tranche (les codes croissent avec la sévérité).
            weatherCode = entries.maxOf { it.weatherCode },
        )
    }.sortedWith(compareBy({ it.date }, { it.part.ordinal }))
}
