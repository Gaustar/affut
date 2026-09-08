package com.gauthier.affut.data.repository

import android.content.Context

/** Réglages modifiables depuis l'écran Paramètres. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("affut_settings", Context.MODE_PRIVATE)

    /** Intervalle de rafraîchissement de la météo, en minutes. */
    var weatherRefreshMinutes: Int
        get() = prefs.getInt(KEY_WEATHER_MINUTES, DEFAULT_WEATHER_MINUTES)
        set(value) = prefs.edit().putInt(KEY_WEATHER_MINUTES, value).apply()

    /** Intervalle d'envoi de la position pendant un partage, en minutes. */
    var locationIntervalMinutes: Int
        get() = prefs.getInt(KEY_LOCATION_MINUTES, DEFAULT_LOCATION_MINUTES)
        set(value) = prefs.edit().putInt(KEY_LOCATION_MINUTES, value).apply()

    companion object {
        const val DEFAULT_WEATHER_MINUTES = 10
        const val DEFAULT_LOCATION_MINUTES = 1
        val WEATHER_CHOICES = listOf(5, 10, 15, 30)
        val LOCATION_CHOICES = listOf(1, 2, 5, 10)

        private const val KEY_WEATHER_MINUTES = "weather_refresh_minutes"
        private const val KEY_LOCATION_MINUTES = "location_interval_minutes"
    }
}
