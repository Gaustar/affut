package com.gauthier.affut.ui.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gauthier.affut.data.repository.FriendRepository
import com.gauthier.affut.data.repository.GroupRepository
import com.gauthier.affut.data.repository.UserRepository
import com.gauthier.affut.domain.model.Group
import com.gauthier.affut.domain.model.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendsGroupsViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository.create()
    private val friendRepository = FriendRepository.create()
    private val groupRepository = GroupRepository.create()

    private val _myProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _myProfile.asStateFlow()

    private val _isSignedIn = MutableStateFlow(Firebase.auth.currentUser != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    /** true si on est connecté mais que la création/lecture du profil a échoué (hors-ligne,
     * Firestore injoignable...) — distinct de "non connecté", pour ne pas laisser l'écran
     * bloqué en silence sur "…" sans jamais expliquer pourquoi ni proposer de réessayer. */
    private val _profileLoadFailed = MutableStateFlow(false)
    val profileLoadFailed: StateFlow<Boolean> = _profileLoadFailed.asStateFlow()

    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends: StateFlow<List<UserProfile>> = _friends.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadProfile()
        refresh()
    }

    private fun loadProfile() {
        val user = Firebase.auth.currentUser
        _isSignedIn.value = user != null
        if (user == null) return
        viewModelScope.launch {
            _profileLoadFailed.value = false
            val result = runCatching { userRepository.ensureProfile(user) }
            _myProfile.value = result.getOrNull()
            _profileLoadFailed.value = result.isFailure
        }
    }

    /** Relance la création/lecture du profil après un échec (hors-ligne, etc.) — sans ça,
     * l'écran restait bloqué sur "…" sans jamais donner de moyen de s'en sortir. */
    fun retryProfile() {
        loadProfile()
    }

    fun refresh() {
        viewModelScope.launch {
            _friends.value = runCatching { friendRepository.listFriends() }.getOrDefault(emptyList())
            _groups.value = runCatching { groupRepository.listMyGroups() }.getOrDefault(emptyList())
        }
    }

    fun addFriendByCode(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            friendRepository.addFriendByCode(code)
                .onSuccess {
                    _message.value = "${it.displayName} ajouté(e) comme ami(e)."
                    refresh()
                }
                .onFailure { _message.value = it.message ?: "Impossible d'ajouter cet ami." }
        }
    }

    fun removeFriend(uid: String) {
        viewModelScope.launch {
            friendRepository.removeFriend(uid)
            refresh()
        }
    }

    fun createGroup(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            groupRepository.createGroup(name)
                .onSuccess {
                    _message.value = "Groupe \"${it.name}\" créé — code ${it.code}."
                    refresh()
                }
                .onFailure { _message.value = it.message ?: "Impossible de créer le groupe." }
        }
    }

    fun joinGroupByCode(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            groupRepository.joinGroupByCode(code)
                .onSuccess {
                    _message.value = "Groupe \"${it.name}\" rejoint."
                    refresh()
                }
                .onFailure { _message.value = it.message ?: "Impossible de rejoindre ce groupe." }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.leaveGroup(groupId)
            refresh()
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
