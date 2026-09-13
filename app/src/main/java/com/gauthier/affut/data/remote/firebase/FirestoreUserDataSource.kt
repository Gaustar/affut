package com.gauthier.affut.data.remote.firebase

import com.gauthier.affut.data.remote.firebase.dto.UserProfileDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

private const val USERS_COLLECTION = "users"
private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sans 0/O/1/I, ambigus à l'oral
private const val CODE_LENGTH = 6

class FirestoreUserDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {
    private val collection = firestore.collection(USERS_COLLECTION)

    suspend fun getProfile(uid: String): UserProfileDto? =
        collection.document(uid).get().await().toObject(UserProfileDto::class.java)

    suspend fun findByFriendCode(code: String): UserProfileDto? {
        val snapshot = collection.whereEqualTo("friendCode", code).limit(1).get().await()
        return snapshot.documents.firstOrNull()?.toObject(UserProfileDto::class.java)
    }

    /** Crée le profil s'il n'existe pas encore, avec un code unique généré à la volée. */
    suspend fun ensureProfile(uid: String, displayName: String): UserProfileDto {
        getProfile(uid)?.let { return it }

        var code: String
        do {
            code = randomCode()
        } while (findByFriendCode(code) != null)

        val profile = UserProfileDto(
            uid = uid,
            displayName = displayName,
            friendCode = code,
            createdAt = System.currentTimeMillis(),
        )
        collection.document(uid).set(profile).await()
        return profile
    }

    suspend fun getGroupIds(uid: String): List<String> = getProfile(uid)?.groupIds ?: emptyList()

    suspend fun addGroupId(uid: String, groupId: String) {
        collection.document(uid).update("groupIds", FieldValue.arrayUnion(groupId)).await()
    }

    suspend fun removeGroupId(uid: String, groupId: String) {
        collection.document(uid).update("groupIds", FieldValue.arrayRemove(groupId)).await()
    }

    private fun randomCode(): String =
        (1..CODE_LENGTH).map { CODE_CHARS[Random.nextInt(CODE_CHARS.length)] }.joinToString("")
}
