package com.gauthier.affut.ui.spotedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.repository.FriendRepository
import com.gauthier.affut.data.repository.GroupRepository
import com.gauthier.affut.data.repository.SpotRepository
import com.gauthier.affut.domain.model.Group
import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.domain.model.SpotType
import com.gauthier.affut.domain.model.UserProfile
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
    val availableFriends: List<UserProfile> = emptyList(),
    val availableGroups: List<Group> = emptyList(),
    val selectedFriendIds: Set<String> = emptySet(),
    val selectedGroupIds: Set<String> = emptySet(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val observedAt: Long? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    /** Le spot à modifier n'existe plus (supprimé entre-temps, ici ou par un autre utilisateur). */
    val isMissing: Boolean = false,
)

class SpotEditViewModel(
    application: Application,
    private val editingSpotId: String?,
    initialLatitude: Double,
    initialLongitude: Double,
) : AndroidViewModel(application) {

    private val repository = SpotRepository.create(application)
    private val friendRepository = FriendRepository.create()
    private val groupRepository = GroupRepository.create()

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
        viewModelScope.launch {
            // Chargés en parallèle logique : n'empêchent pas le formulaire de s'afficher
            // si l'un des deux échoue (ex: hors-ligne) — la portée sera juste vide.
            val friends = runCatching { friendRepository.listFriends() }.getOrDefault(emptyList())
            val groups = runCatching { groupRepository.listMyGroups() }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(availableFriends = friends, availableGroups = groups)
        }

        if (editingSpotId != null) {
            viewModelScope.launch {
                val spot = repository.getById(editingSpotId)
                existingSpot = spot
                _uiState.value = if (spot != null) {
                    _uiState.value.copy(
                        title = spot.title,
                        type = spot.type,
                        notes = spot.notes,
                        selectedFriendIds = spot.sharedWithFriendIds.toSet(),
                        selectedGroupIds = spot.sharedWithGroupIds.toSet(),
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

    fun onFriendToggle(uid: String) {
        val current = _uiState.value.selectedFriendIds
        _uiState.value = _uiState.value.copy(
            selectedFriendIds = if (uid in current) current - uid else current + uid,
        )
    }

    fun onGroupToggle(groupId: String) {
        val current = _uiState.value.selectedGroupIds
        _uiState.value = _uiState.value.copy(
            selectedGroupIds = if (groupId in current) current - groupId else current + groupId,
        )
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
            val friendIds = state.selectedFriendIds.toList()
            val groupIds = state.selectedGroupIds.toList()
            val sharedWithUids = groupRepository.resolveSharedUids(friendIds, groupIds)
            val spot = Spot(
                id = existingSpot?.id ?: editingSpotId ?: UUID.randomUUID().toString(),
                ownerId = existingSpot?.ownerId ?: ownerId,
                title = state.title.trim(),
                type = state.type,
                latitude = state.latitude,
                longitude = state.longitude,
                accuracy = existingSpot?.accuracy ?: 0f,
                notes = state.notes.trim(),
                sharedWithFriendIds = friendIds,
                sharedWithGroupIds = groupIds,
                sharedWithUids = sharedWithUids,
                createdAt = existingSpot?.createdAt ?: now,
                updatedAt = now,
                observedAt = state.observedAt,
            )
            repository.upsert(spot)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
