package com.gauthier.affut.ui.spotedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.repository.SpotRepository
import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.domain.model.SpotType
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SpotEditUiState(
    val title: String = "",
    val type: SpotType = SpotType.AUTRE,
    val notes: String = "",
    val isShared: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val observedAt: Long? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    /** Le spot à modifier n'existe plus (supprimé entre-temps, ici ou par l'autre utilisateur). */
    val isMissing: Boolean = false,
)

class SpotEditViewModel(
    application: Application,
    private val editingSpotId: String?,
    initialLatitude: Double,
    initialLongitude: Double,
) : AndroidViewModel(application) {

    private val repository = SpotRepository.create(application)

    private val _uiState = MutableStateFlow(
        SpotEditUiState(
            latitude = initialLatitude,
            longitude = initialLongitude,
            isEditing = editingSpotId != null,
            isLoading = editingSpotId != null,
        ),
    )
    val uiState: StateFlow<SpotEditUiState> = _uiState.asStateFlow()

    private var existingSpot: Spot? = null

    init {
        if (editingSpotId != null) {
            viewModelScope.launch {
                val spot = repository.getById(editingSpotId)
                existingSpot = spot
                _uiState.value = if (spot != null) {
                    _uiState.value.copy(
                        title = spot.title,
                        type = spot.type,
                        notes = spot.notes,
                        isShared = spot.isShared,
                        latitude = spot.latitude,
                        longitude = spot.longitude,
                        observedAt = spot.observedAt,
                        isLoading = false,
                    )
                } else {
                    _uiState.value.copy(isLoading = false, isMissing = true)
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun onTypeChange(value: SpotType) {
        _uiState.value = _uiState.value.copy(type = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onSharedChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(isShared = value)
    }

    fun onObservedAtChange(value: Long?) {
        _uiState.value = _uiState.value.copy(observedAt = value)
    }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) return
        // Sans ce garde-fou, un spot supprimé entre-temps serait recréé en (0,0).
        if (state.isMissing) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val ownerId = Firebase.auth.currentUser?.uid ?: "inconnu"
            val spot = Spot(
                id = existingSpot?.id ?: editingSpotId ?: UUID.randomUUID().toString(),
                ownerId = existingSpot?.ownerId ?: ownerId,
                title = state.title.trim(),
                type = state.type,
                latitude = state.latitude,
                longitude = state.longitude,
                accuracy = existingSpot?.accuracy ?: 0f,
                notes = state.notes.trim(),
                isShared = state.isShared,
                createdAt = existingSpot?.createdAt ?: now,
                updatedAt = now,
                observedAt = state.observedAt,
            )
            repository.upsert(spot)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
