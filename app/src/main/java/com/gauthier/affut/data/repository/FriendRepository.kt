package com.gauthier.affut.data.repository

import com.gauthier.affut.data.remote.firebase.FirestoreFriendshipDataSource
import com.gauthier.affut.domain.model.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class FriendRepository(
    private val remoteDataSource: FirestoreFriendshipDataSource = FirestoreFriendshipDataSource(),
    private val userRepository: UserRepository = UserRepository.create(),
) {
    /** Ajoute l'utilisateur propriétaire de ce code comme ami (relation mutuelle immédiate,
     * pas d'étape d'acceptation séparée : échanger le code vaut consentement des deux côtés). */
    suspend fun addFriendByCode(code: String): Result<UserProfile> {
        val myUid = Firebase.auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Non connecté"))
        val profile = userRepository.findByFriendCode(code)
            ?: return Result.failure(NoSuchElementException("Aucun compte avec ce code"))
        if (profile.uid == myUid) {
            return Result.failure(IllegalArgumentException("C'est ton propre code"))
        }
        remoteDataSource.addFriend(myUid, profile.uid)
        return Result.success(profile)
    }

    suspend fun removeFriend(friendUid: String) {
        val myUid = Firebase.auth.currentUser?.uid ?: return
        remoteDataSource.removeFriend(myUid, friendUid)
    }

    suspend fun listFriends(): List<UserProfile> {
        val myUid = Firebase.auth.currentUser?.uid ?: return emptyList()
        return remoteDataSource.listFriendUids(myUid).mapNotNull { userRepository.getProfile(it) }
    }

    companion object {
        fun create(): FriendRepository = FriendRepository()
    }
}
