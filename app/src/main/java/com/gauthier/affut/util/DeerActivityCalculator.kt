package com.gauthier.affut.util

import com.gauthier.affut.domain.model.WeatherSnapshot
import java.time.Duration
import java.time.LocalDateTime

enum class DeerActivityLevel(val label: String, val emoji: String) {
    FAVORABLE("Favorable", "🟢"),
    MOYEN("Moyen", "🟠"),
    DEFAVORABLE("Défavorable", "🔴"),
}

data class DeerActivityResult(
    val level: DeerActivityLevel,
    val reasons: List<String>,
) {
    /** Phrase courte du type "Vent faible, chute de pression, crépuscule à 20h47". */
    fun summary(): String =
        reasons.joinToString(", ").replaceFirstChar { it.uppercase() }.ifBlank { "Conditions neutres" }
}

private const val TWILIGHT_WINDOW_MINUTES = 60L
private const val LOW_WIND_THRESHOLD_KMH = 20.0
private const val HIGH_WIND_THRESHOLD_KMH = 40.0
private const val FALLING_PRESSURE_THRESHOLD_HPA = -1.0
private const val RISING_PRESSURE_THRESHOLD_HPA = 1.0

/**
 * Indice indicatif d'activité des cerfs, basé sur : crépuscule (aube/coucher ± 1h),
 * vitesse du vent, tendance de pression, phase lunaire, précipitations à venir.
 * Un score plus élevé = conditions plus favorables ; pas de prétention scientifique exacte.
 */
object DeerActivityCalculator {
    fun compute(weather: WeatherSnapshot, now: LocalDateTime = LocalDateTime.now()): DeerActivityResult {
        var score = 0
        val reasons = mutableListOf<String>()
        val today = weather.daily.firstOrNull()

        val sunrise = today?.sunrise?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val sunset = today?.sunset?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val nearSunrise = sunrise != null && Duration.between(sunrise, now).abs().toMinutes() <= TWILIGHT_WINDOW_MINUTES
        val nearSunset = sunset != null && Duration.between(sunset, now).abs().toMinutes() <= TWILIGHT_WINDOW_MINUTES
        when {
            nearSunset -> {
                score += 2
                reasons.add("crépuscule à ${sunset!!.toLocalTime()}")
            }
            nearSunrise -> {
                score += 2
                reasons.add("aube à ${sunrise!!.toLocalTime()}")
            }
        }

        when {
            weather.windSpeedKmh < LOW_WIND_THRESHOLD_KMH -> {
                score += 1
                reasons.add("vent faible")
            }
            weather.windSpeedKmh > HIGH_WIND_THRESHOLD_KMH -> {
                score -= 1
                reasons.add("vent fort")
            }
        }

        when {
            weather.pressureTrendHpa3h <= FALLING_PRESSURE_THRESHOLD_HPA -> {
                score += 1
                reasons.add("chute de pression")
            }
            weather.pressureTrendHpa3h >= RISING_PRESSURE_THRESHOLD_HPA -> {
                reasons.add("pression en hausse")
            }
        }

        if (moonPhaseFor(now.toLocalDate()) == MoonPhase.PLEINE_LUNE) {
            score += 1
            reasons.add("pleine lune")
        }

        when {
            weather.precipitationProbabilityNextHour in 20..60 -> {
                score += 1
                reasons.add("pluie à venir")
            }
            weather.precipitationProbabilityNextHour > 60 -> score -= 1
        }

        val level = when {
            score >= 3 -> DeerActivityLevel.FAVORABLE
            score <= 0 -> DeerActivityLevel.DEFAVORABLE
            else -> DeerActivityLevel.MOYEN
        }
        return DeerActivityResult(level, reasons)
    }
}
