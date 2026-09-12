package com.gauthier.affut.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.remote.firebase.dto.LivePositionDto
import com.gauthier.affut.data.routing.NavigationTarget
import com.gauthier.affut.data.routing.NavigationTargets
import com.gauthier.affut.data.routing.OfflineRouter
import com.gauthier.affut.data.repository.LiveShareRepository
import com.gauthier.affut.data.repository.SpotRepository
import com.gauthier.affut.data.repository.UpdateRepository
import com.gauthier.affut.data.repository.WeatherRepository
import com.gauthier.affut.domain.model.AppUpdate
import com.gauthier.affut.domain.model.RoutePoint
import com.gauthier.affut.domain.model.RouteResult
import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.domain.model.WeatherSnapshot
import com.gauthier.affut.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SpotRepository.create(application)
    private val weatherRepository = WeatherRepository.create(application)
    private val liveShareRepository = LiveShareRepository.create(application)
    private val router = OfflineRouter(application)
    private val updateRepository = UpdateRepository.create()

    val spots: StateFlow<List<Spot>> = repository.observeSpots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val otherLiveShares: StateFlow<List<LivePositionDto>> = liveShareRepository.observeOtherActiveShares()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val navigationTarget: StateFlow<NavigationTarget?> = NavigationTargets.target

    private val _route = MutableStateFlow<RouteResult?>(null)
    val route: StateFlow<RouteResult?> = _route.asStateFlow()

    private val _isRouting = MutableStateFlow(false)
    val isRouting: StateFlow<Boolean> = _isRouting.asStateFlow()

    private val _weather = MutableStateFlow<WeatherSnapshot?>(null)
    val weather: StateFlow<WeatherSnapshot?> = _weather.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdate?>(null)
    val availableUpdate: StateFlow<AppUpdate?> = _availableUpdate.asStateFlow()

    init {
        // Une fois par lancement : suffisant pour "voir la notification au prochain démarrage
        // après une mise à jour publiée sur GitHub", sans justifier une vérification périodique.
        viewModelScope.launch {
            _availableUpdate.value = updateRepository.checkForUpdate(BuildConfig.VERSION_NAME)
        }
    }

    fun dismissUpdateNotice() {
        _availableUpdate.value = null
    }

    /** Recalcule l'itinéraire depuis la position courante vers la destination choisie. */
    fun computeRoute(fromLatitude: Double, fromLongitude: Double) {
        val target = navigationTarget.value ?: return
        viewModelScope.launch {
            _isRouting.value = true
            _route.value = router.route(
                from = RoutePoint(fromLatitude, fromLongitude),
                to = RoutePoint(target.latitude, target.longitude),
            )
            _isRouting.value = false
        }
    }

    fun stopNavigation() {
        NavigationTargets.stop()
        _route.value = null
    }

    fun refreshWeather(latitude: Double, longitude: Double, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            weatherRepository.getWeather(latitude, longitude, forceRefresh).onSuccess { _weather.value = it }
        }
    }
}
