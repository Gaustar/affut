package com.gauthier.affut.data.repository

import android.content.Context
import com.gauthier.affut.data.local.AppDatabase
import com.gauthier.affut.data.local.dao.WeatherCacheDao
import com.gauthier.affut.data.mapper.toDomain
import com.gauthier.affut.data.mapper.toEntity
import com.gauthier.affut.data.remote.weather.OpenMeteoApi
import com.gauthier.affut.data.remote.weather.OpenMeteoClient
import com.gauthier.affut.domain.model.WeatherSnapshot
import java.util.Locale

class WeatherRepository(
    private val weatherCacheDao: WeatherCacheDao,
    private val api: OpenMeteoApi = OpenMeteoClient.api,
    /** Durée de fraîcheur du cache, alignée sur le réglage choisi dans les Paramètres. */
    private val cacheFreshDurationMs: () -> Long = { 10 * 60 * 1000L },
) {
    suspend fun getWeather(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false,
    ): Result<WeatherSnapshot> {
        val key = locationKey(latitude, longitude)

        if (!forceRefresh) {
            val cached = weatherCacheDao.get(key)
            if (cached != null && System.currentTimeMillis() - cached.fetchedAt < cacheFreshDurationMs()) {
                return Result.success(cached.toDomain())
            }
        }

        return try {
            val response = api.getForecast(latitude, longitude)
            val snapshot = response.toDomain(latitude, longitude)
            weatherCacheDao.upsert(snapshot.toEntity(key))
            Result.success(snapshot)
        } catch (e: Exception) {
            // Hors-ligne ou erreur réseau : on retombe sur le cache même périmé plutôt que rien
            // afficher, mais on marque la donnée pour que l'écran puisse le dire.
            val cached = weatherCacheDao.get(key)
            if (cached != null) Result.success(cached.toDomain().copy(isStale = true)) else Result.failure(e)
        }
    }

    companion object {
        fun locationKey(lat: Double, lon: Double): String =
            String.format(Locale.US, "%.2f,%.2f", lat, lon)

        fun create(context: Context): WeatherRepository {
            val preferences = AppPreferences(context)
            return WeatherRepository(
                weatherCacheDao = AppDatabase.getInstance(context).weatherCacheDao(),
                cacheFreshDurationMs = { preferences.weatherRefreshMinutes * 60_000L },
            )
        }
    }
}
