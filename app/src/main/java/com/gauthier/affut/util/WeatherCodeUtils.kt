package com.gauthier.affut.util

/** Codes météo WMO utilisés par Open-Meteo. */
fun weatherEmoji(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2 -> "🌤️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 56, 57 -> "🌦️"
    61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
    71, 73, 75, 77, 85, 86 -> "🌨️"
    95, 96, 99 -> "⛈️"
    else -> "🌡️"
}

fun weatherLabel(code: Int): String = when (code) {
    0 -> "Ciel dégagé"
    1 -> "Peu nuageux"
    2 -> "Partiellement nuageux"
    3 -> "Couvert"
    45, 48 -> "Brouillard"
    51, 53, 55, 56, 57 -> "Bruine"
    61, 63, 65, 66, 67 -> "Pluie"
    71, 73, 75, 77 -> "Neige"
    80, 81, 82 -> "Averses"
    85, 86 -> "Averses de neige"
    95, 96, 99 -> "Orage"
    else -> "Inconnu"
}
