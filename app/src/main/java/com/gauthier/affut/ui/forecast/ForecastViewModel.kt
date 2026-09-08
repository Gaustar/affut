package com.gauthier.affut.ui.forecast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.repository.WeatherRepository
import com.gauthier.affut.domain.model.WeatherSnapshot
import com.gauthier.affut.util.lastKnownLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Centre approximatif de la France — repli si aucune position connue.
private const val DEFAULT_LATITUDE = 46.6
private const val DEFAULT_LONGITUDE = 2.4

/**
 * [targetLatitude]/[targetLongitude] : météo d'un lieu précis (un spot, un point choisi).
 * Si null, on utilise la position actuelle.
 */
class ForecastViewModel(
    application: Application,
    private val targetLatitude: Double? = null,
    private val targetLongitude: Double? = null,
) : AndroidViewModel(application) {
    private val weatherRepository = WeatherRepository.create(application)

    private val _weather = MutableStateFlow<WeatherSnapshot?>(null)
    val weather: StateFlow<WeatherSnapshot?> = _weather.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load(forceRefresh = false)
    }

    fun refresh() {
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val location = if (targetLatitude == null) lastKnownLocation(getApplication()) else null
            val lat = targetLatitude ?: location?.latitude ?: _weather.value?.latitude ?: DEFAULT_LATITUDE
            val lon = targetLongitude ?: location?.longitude ?: _weather.value?.longitude ?: DEFAULT_LONGITUDE
            weatherRepository.getWeather(lat, lon, forceRefresh).onSuccess { _weather.value = it }
            _isLoading.value = false
        }
    }
}
