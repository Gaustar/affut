package com.gauthier.affut.util

import com.gauthier.affut.domain.model.WeatherSnapshot
import java.time.LocalDateTime

private const val STRONG_WIND_THRESHOLD_KMH = 40.0
private const val STRONG_GUST_THRESHOLD_KMH = 60.0
private const val HEAVY_RAIN_MM = 4.0
private const val POOR_VISIBILITY_M = 1000.0
private const val FREEZING_C = 0.0
private val STORM_CODES = setOf(95, 96, 99)
private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)

/** Fenêtre de prévision utile pour préparer une sortie ou une nuit dehors. */
private const val LOOKAHEAD_HOURS = 24

/**
 * Alertes pour les prochaines 24 h, pas seulement pour l'instant présent : ce qui compte
 * avant un bivouac, c'est l'orage de cette nuit ou le gel de demain matin.
 */
fun weatherAlerts(weather: WeatherSnapshot): List<String> {
    val alerts = mutableListOf<String>()
    val window = weather.hourly.take(LOOKAHEAD_HOURS)

    if (weather.windSpeedKmh >= STRONG_WIND_THRESHOLD_KMH) {
        alerts.add("Vent fort actuellement (${weather.windSpeedKmh.toInt()} km/h)")
    }
    if (weather.weatherCode in STORM_CODES) {
        alerts.add("Orage en cours")
    }

    if (window.isEmpty()) return alerts

    window.firstOrNull { it.weatherCode in STORM_CODES }?.let {
        alerts.add("Orage prévu ${whenLabel(it.time)}")
    }
    window.firstOrNull { it.weatherCode in SNOW_CODES }?.let {
        alerts.add("Neige prévue ${whenLabel(it.time)}")
    }
    window.filter { it.temperature <= FREEZING_C }.minByOrNull { it.temperature }?.let {
        alerts.add("Gel ${whenLabel(it.time)} (${it.temperature.toInt()}°C)")
    }
    window.maxByOrNull { it.windGustKmh }?.takeIf { it.windGustKmh >= STRONG_GUST_THRESHOLD_KMH }?.let {
        alerts.add("Rafales à ${it.windGustKmh.toInt()} km/h ${whenLabel(it.time)}")
    }
    window.sumOf { it.precipitationMm }.takeIf { it >= HEAVY_RAIN_MM }?.let {
        alerts.add("Pluie soutenue à venir (${"%.1f".format(it)} mm sur 24 h)")
    }
    window.firstOrNull { it.visibilityMeters in 1.0..POOR_VISIBILITY_M }?.let {
        alerts.add("Visibilité réduite ${whenLabel(it.time)} (${it.visibilityMeters.toInt()} m)")
    }

    return alerts
}

/** "ce soir à 21h", "demain à 06h" — plus parlant qu'une date ISO. */
private fun whenLabel(isoTime: String): String {
    val time = runCatching { LocalDateTime.parse(isoTime) }.getOrNull() ?: return ""
    val now = LocalDateTime.now()
    val hour = "%02dh".format(time.hour)
    return when {
        time.toLocalDate() == now.toLocalDate() && time.hour >= 18 -> "ce soir à $hour"
        time.toLocalDate() == now.toLocalDate() -> "aujourd'hui à $hour"
        time.toLocalDate() == now.toLocalDate().plusDays(1) -> "demain à $hour"
        else -> "le ${time.dayOfMonth}/${time.monthValue} à $hour"
    }
}
