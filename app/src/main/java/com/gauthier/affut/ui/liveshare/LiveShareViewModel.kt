package com.gauthier.affut.ui.liveshare

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.remote.firebase.dto.LivePositionDto
import com.gauthier.affut.data.repository.FriendRepository
import com.gauthier.affut.data.repository.GroupRepository
import com.gauthier.affut.data.repository.LiveShareRepository
import com.gauthier.affut.domain.model.Group
import com.gauthier.affut.domain.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShareDuration(val label: String, val millis: Long)

val SHARE_DURATIONS = listOf(
    ShareDuration("15 min", 15 * 60_000L),
    ShareDuration("30 min", 30 * 60_000L),
    ShareDuration("1 h", 60 * 60_000L),
    ShareDuration("2 h", 2 * 60 * 60_000L),
    ShareDuration("4 h", 4 * 60 * 60_000L),
    ShareDuration("8 h", 8 * 60 * 60_000L),
    ShareDuration("24 h", 24 * 60 * 60_000L),
)

private const val REFRESH_INTERVAL_MS = 5_000L

class LiveShareViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LiveShareRepository.create(application)
    private val friendRepository = FriendRepository.create()
    private val groupRepository = GroupRepository.create()

    private val _isActive = MutableStateFlow(repository.isActive)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _expiresAt = MutableStateFlow(repository.expiresAt)
    val expiresAt: StateFlow<Long> = _expiresAt.asStateFlow()

    private val _lastSentAt = MutableStateFlow(repository.lastSentAt)
    val lastSentAt: StateFlow<Long> = _lastSentAt.asStateFlow()

    private val _hasUnsentPosition = MutableStateFlow(repository.hasUnsentPosition)
    val hasUnsentPosition: StateFlow<Boolean> = _hasUnsentPosition.asStateFlow()

    private val _availableFriends = MutableStateFlow<List<UserProfile>>(emptyList())
    val availableFriends: StateFlow<List<UserProfile>> = _availableFriends.asStateFlow()

    private val _availableGroups = MutableStateFlow<List<Group>>(emptyList())
    val availableGroups: StateFlow<List<Group>> = _availableGroups.asStateFlow()

    private val _selectedFriendIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFriendIds: StateFlow<Set<String>> = _selectedFriendIds.asStateFlow()

    private val _selectedGroupIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedGroupIds: StateFlow<Set<String>> = _selectedGroupIds.asStateFlow()

    /** Positions actives des amis/groupes qui m'ont partagé — potentiellement plusieurs à la fois. */
    val friendPositions: StateFlow<List<LivePositionDto>> = repository.observeOtherActiveShares()
        .map { shares ->
            val now = System.currentTimeMillis()
            shares.filter { it.isActive && now < it.expiresAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // L'état réel vit dans le service (autre processus logique) : on relit périodiquement
        // les préférences locales tant que l'écran est ouvert, plutôt qu'un flux temps réel complexe.
        viewModelScope.launch {
            // La boucle s'arrête automatiquement à l'annulation de viewModelScope (delay() la relaie).
            while (true) {
                refreshState()
                delay(REFRESH_INTERVAL_MS)
            }
        }
        viewModelScope.launch {
            _availableFriends.value = runCatching { friendRepository.listFriends() }.getOrDefault(emptyList())
            _availableGroups.value = runCatching { groupRepository.listMyGroups() }.getOrDefault(emptyList())
        }
    }

    fun onFriendToggle(uid: String) {
        val current = _selectedFriendIds.value
        _selectedFriendIds.value = if (uid in current) current - uid else current + uid
    }

    fun onGroupToggle(groupId: String) {
        val current = _selectedGroupIds.value
        _selectedGroupIds.value = if (groupId in current) current - groupId else current + groupId
    }

    fun start(duration: ShareDuration) {
        viewModelScope.launch {
            val sharedWithUids = groupRepository.resolveSharedUids(
                _selectedFriendIds.value.toList(),
                _selectedGroupIds.value.toList(),
            )
            repository.start(duration.millis, sharedWithUids)
            refreshState()
        }
    }

    fun stop() {
        viewModelScope.launch {
            repository.stop()
            refreshState()
        }
    }

    fun refreshState() {
        _isActive.value = repository.isActive
        _expiresAt.value = repository.expiresAt
        _lastSentAt.value = repository.lastSentAt
        _hasUnsentPosition.value = repository.hasUnsentPosition
    }
}
