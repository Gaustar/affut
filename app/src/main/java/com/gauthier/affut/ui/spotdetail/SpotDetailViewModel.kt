package com.gauthier.affut.ui.spotdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.repository.SpotRepository
import com.gauthier.affut.data.repository.WeatherRepository
import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpotDetailViewModel(
    application: Application,
    private val spotId: String,
) : AndroidViewModel(application) {
    private val repository = SpotRepository.create(application)
    private val weatherRepository = WeatherRepository.create(application)

    private val _spot = MutableStateFlow<Spot?>(null)
    val spot: StateFlow<Spot?> = _spot.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _weather = MutableStateFlow<WeatherSnapshot?>(null)
    val weather: StateFlow<WeatherSnapshot?> = _weather.asStateFlow()

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()

    private var weatherLoadedForId: String? = null

    init {
        viewModelScope.launch {
            repository.observeSpots().collect { spots ->
                val current = spots.find { it.id == spotId }
                _spot.value = current
                if (current != null && weatherLoadedForId != current.id) {
                    weatherLoadedForId = current.id
                    loadWeather(current.latitude, current.longitude)
                }
            }
        }
    }

    fun refreshWeather() {
        val current = _spot.value ?: return
        loadWeather(current.latitude, current.longitude, forceRefresh = true)
    }

    private fun loadWeather(latitude: Double, longitude: Double, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            weatherRepository.getWeather(latitude, longitude, forceRefresh)
                .onSuccess {
                    _weather.value = it
                    _weatherError.value = null
                }
                .onFailure { _weatherError.value = "Météo indisponible pour ce spot (hors ligne, aucun cache)." }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(spotId)
            _deleted.value = true
        }
    }
}
