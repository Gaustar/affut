package com.gauthier.affut.ui.compass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.repository.WeatherRepository
import com.gauthier.affut.domain.model.WeatherSnapshot
import com.gauthier.affut.sensor.CompassReading
import com.gauthier.affut.sensor.CompassSensorManager
import com.gauthier.affut.util.lastKnownLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DEFAULT_LATITUDE = 46.6
private const val DEFAULT_LONGITUDE = 2.4

class CompassViewModel(application: Application) : AndroidViewModel(application) {
    private val compassSensorManager = CompassSensorManager(application)
    private val weatherRepository = WeatherRepository.create(application)

    val hasSensor: Boolean = compassSensorManager.hasCompassSensor()

    val compass: StateFlow<CompassReading?> = compassSensorManager.observeCompass()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _weather = MutableStateFlow<WeatherSnapshot?>(null)
    val weather: StateFlow<WeatherSnapshot?> = _weather.asStateFlow()

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()

    init {
        viewModelScope.launch {
            val location = lastKnownLocation(application)
            val lat = location?.latitude ?: DEFAULT_LATITUDE
            val lon = location?.longitude ?: DEFAULT_LONGITUDE
            // Corrige l'écart nord magnétique / nord géographique pour que la boussole
            // soit alignée avec la carte.
            compassSensorManager.updateLocation(lat, lon, location?.altitude ?: 0.0)
            weatherRepository.getWeather(lat, lon)
                .onSuccess { _weather.value = it }
                .onFailure { _weatherError.value = "Vent indisponible : météo injoignable et aucune donnée en cache." }
        }
    }
}
