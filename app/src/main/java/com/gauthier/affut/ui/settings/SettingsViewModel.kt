package com.gauthier.affut.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.local.entity.DownloadedRegionEntity
import com.gauthier.affut.data.repository.AppPreferences
import com.gauthier.affut.data.repository.AuthRepository
import com.gauthier.affut.data.repository.OfflineMapRepository
import com.gauthier.affut.data.routing.OfflineRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutingDataStatus(
    val installed: Boolean,
    val fileNames: List<String>,
    val totalMegabytes: Long,
    val directoryPath: String,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val offlineMapRepository = OfflineMapRepository.create(application)
    private val preferences = AppPreferences(application)
    private val router = OfflineRouter(application)
    private val authRepository = AuthRepository()

    private val _isSignedIn = MutableStateFlow(authRepository.currentUser != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    /** À rappeler après une tentative de connexion, pour refléter le résultat à l'écran. */
    fun refreshAuthState() {
        _isSignedIn.value = authRepository.currentUser != null
    }

    val regions: StateFlow<List<DownloadedRegionEntity>> = offlineMapRepository.observeRegions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _weatherMinutes = MutableStateFlow(preferences.weatherRefreshMinutes)
    val weatherMinutes: StateFlow<Int> = _weatherMinutes.asStateFlow()

    private val _locationMinutes = MutableStateFlow(preferences.locationIntervalMinutes)
    val locationMinutes: StateFlow<Int> = _locationMinutes.asStateFlow()

    private val _routingStatus = MutableStateFlow(readRoutingStatus())
    val routingStatus: StateFlow<RoutingDataStatus> = _routingStatus.asStateFlow()

    fun setWeatherMinutes(value: Int) {
        preferences.weatherRefreshMinutes = value
        _weatherMinutes.value = value
    }

    fun setLocationMinutes(value: Int) {
        preferences.locationIntervalMinutes = value
        _locationMinutes.value = value
    }

    fun deleteRegion(id: String) {
        viewModelScope.launch { offlineMapRepository.deleteRegion(id) }
    }

    fun refreshRoutingStatus() {
        _routingStatus.value = readRoutingStatus()
    }

    fun signOut() {
        authRepository.signOut()
        refreshAuthState()
    }

    private fun readRoutingStatus(): RoutingDataStatus {
        val dir = router.segmentsDirectory()
        val files = dir.listFiles { f -> f.name.endsWith(".rd5") }.orEmpty()
        return RoutingDataStatus(
            installed = files.isNotEmpty(),
            fileNames = files.map { it.name }.sorted(),
            totalMegabytes = files.sumOf { it.length() } / (1024 * 1024),
            directoryPath = dir.absolutePath,
        )
    }
}
