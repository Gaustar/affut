package com.gauthier.affut.data.remote.firebase

import com.gauthier.affut.data.remote.firebase.dto.SpotDto
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

private const val SPOTS_COLLECTION = "spots"

class FirestoreSpotDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {
    private val collection = firestore.collection(SPOTS_COLLECTION)

    suspend fun push(dto: SpotDto) {
        collection.document(dto.id).set(dto).await()
    }

    suspend fun delete(spotId: String) {
        collection.document(spotId).delete().await()
    }

    /** Mes spots + ceux partagés avec moi (amis ou groupes, aplatis dans sharedWithUids). */
    suspend fun fetchVisibleSpots(uid: String): List<SpotDto> {
        val snapshot = collection
            .where(Filter.or(Filter.equalTo("ownerId", uid), Filter.arrayContains("sharedWithUids", uid)))
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(SpotDto::class.java) }
    }
}
