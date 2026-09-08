package com.gauthier.affut.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class MoonPhase(val label: String, val emoji: String) {
    NOUVELLE_LUNE("Nouvelle lune", "🌑"),
    PREMIER_CROISSANT("Premier croissant", "🌒"),
    PREMIER_QUARTIER("Premier quartier", "🌓"),
    GIBBEUSE_CROISSANTE("Gibbeuse croissante", "🌔"),
    PLEINE_LUNE("Pleine lune", "🌕"),
    GIBBEUSE_DECROISSANTE("Gibbeuse décroissante", "🌖"),
    DERNIER_QUARTIER("Dernier quartier", "🌗"),
    DERNIER_CROISSANT("Dernier croissant", "🌘"),
}

// Référence : nouvelle lune du 6 janvier 2000, couramment utilisée pour ce calcul approximatif.
private val REFERENCE_NEW_MOON: LocalDate = LocalDate.of(2000, 1, 6)
private const val SYNODIC_MONTH_DAYS = 29.53058867

/** Calcul approximatif (± 1 jour), suffisant pour une indication de tendance, pas un almanach précis. */
fun moonPhaseFor(date: LocalDate): MoonPhase {
    val daysSince = ChronoUnit.DAYS.between(REFERENCE_NEW_MOON, date).toDouble()
    val phase = (((daysSince % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS) / SYNODIC_MONTH_DAYS
    return when {
        phase < 0.0625 || phase >= 0.9375 -> MoonPhase.NOUVELLE_LUNE
        phase < 0.1875 -> MoonPhase.PREMIER_CROISSANT
        phase < 0.3125 -> MoonPhase.PREMIER_QUARTIER
        phase < 0.4375 -> MoonPhase.GIBBEUSE_CROISSANTE
        phase < 0.5625 -> MoonPhase.PLEINE_LUNE
        phase < 0.6875 -> MoonPhase.GIBBEUSE_DECROISSANTE
        phase < 0.8125 -> MoonPhase.DERNIER_QUARTIER
        else -> MoonPhase.DERNIER_CROISSANT
    }
}
