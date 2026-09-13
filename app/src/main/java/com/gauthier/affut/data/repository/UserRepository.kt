package com.gauthier.affut.data.repository

import com.gauthier.affut.data.remote.firebase.FirestoreUserDataSource
import com.gauthier.affut.domain.model.UserProfile
import com.google.firebase.auth.FirebaseUser

class UserRepository(
    private val remoteDataSource: FirestoreUserDataSource = FirestoreUserDataSource(),
) {
    // Petit cache mémoire : un nom affiché ne change pas dans la durée de vie du process,
    // pas la peine de relire Firestore à chaque fois qu'on affiche une liste d'amis.
    private val displayNameCache = mutableMapOf<String, String>()

    /** À appeler une fois après connexion : crée le profil (et le code ami) s'il n'existe pas encore. */
    suspend fun ensureProfile(user: FirebaseUser): UserProfile {
        val dto = remoteDataSource.ensureProfile(user.uid, user.displayName ?: "Sans nom")
        displayNameCache[dto.uid] = dto.displayName
        return UserProfile(dto.uid, dto.displayName, dto.friendCode)
    }

    suspend fun getProfile(uid: String): UserProfile? =
        remoteDataSource.getProfile(uid)?.let { UserProfile(it.uid, it.displayName, it.friendCode) }

    suspend fun findByFriendCode(code: String): UserProfile? =
        remoteDataSource.findByFriendCode(code.trim().uppercase())
            ?.let { UserProfile(it.uid, it.displayName, it.friendCode) }

    suspend fun getDisplayName(uid: String): String {
        displayNameCache[uid]?.let { return it }
        val name = remoteDataSource.getProfile(uid)?.displayName ?: uid.take(6)
        displayNameCache[uid] = name
        return name
    }

    companion object {
        fun create(): UserRepository = UserRepository()
    }
}
