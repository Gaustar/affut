package com.gauthier.affut.ui.spotlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.repository.SpotRepository
import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.domain.model.SpotType
import com.gauthier.affut.util.haversineMeters
import com.gauthier.affut.util.lastKnownLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class SpotSort(val label: String) {
    OBSERVATION("Date d'observation"),
    RECENT("Modifié récemment"),
    DISTANCE("Distance"),
    TITLE("Nom"),
}

/** Un spot accompagné de sa distance à la position actuelle, quand elle est connue. */
data class SpotListEntry(
    val spot: Spot,
    val distanceMeters: Double?,
)

class SpotListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SpotRepository.create(application)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _typeFilter = MutableStateFlow<SpotType?>(null)
    val typeFilter: StateFlow<SpotType?> = _typeFilter.asStateFlow()

    private val _sort = MutableStateFlow(SpotSort.OBSERVATION)
    val sort: StateFlow<SpotSort> = _sort.asStateFlow()

    private val currentLocation = lastKnownLocation(application)

    val spots: StateFlow<List<SpotListEntry>> =
        combine(repository.observeSpots(), _query, _typeFilter, _sort) { spots, query, type, sort ->
            spots
                .filter { type == null || it.type == type }
                .filter { matchesQuery(it, query) }
                .map { spot ->
                    SpotListEntry(
                        spot = spot,
                        distanceMeters = currentLocation?.let {
                            haversineMeters(it.latitude, it.longitude, spot.latitude, spot.longitude)
                        },
                    )
                }
                .sortedWith(comparatorFor(sort))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Nombre de spots par type, pour afficher les filtres réellement utiles. */
    val countsByType: StateFlow<Map<SpotType, Int>> = repository.observeSpots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        .let { flow ->
            combine(flow, _query) { spots, _ -> spots.groupingBy { it.type }.eachCount() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onTypeFilterChange(value: SpotType?) {
        _typeFilter.value = value
    }

    fun onSortChange(value: SpotSort) {
        _sort.value = value
    }

    private fun matchesQuery(spot: Spot, query: String): Boolean {
        if (query.isBlank()) return true
        val needle = query.trim().lowercase()
        return spot.title.lowercase().contains(needle) ||
            spot.notes.lowercase().contains(needle) ||
            spot.type.label.lowercase().contains(needle)
    }

    private fun comparatorFor(sort: SpotSort): Comparator<SpotListEntry> = when (sort) {
        // Les spots sans date d'observation passent après ceux qui en ont une.
        SpotSort.OBSERVATION -> compareByDescending { it.spot.observedAt ?: Long.MIN_VALUE }
        SpotSort.RECENT -> compareByDescending { it.spot.updatedAt }
        SpotSort.DISTANCE -> compareBy { it.distanceMeters ?: Double.MAX_VALUE }
        SpotSort.TITLE -> compareBy { it.spot.title.lowercase() }
    }
}
