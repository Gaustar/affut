package com.gauthier.affut.data.remote.firebase

import com.gauthier.affut.data.remote.firebase.dto.LivePositionDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val LIVE_COLLECTION = "live"

class FirestoreLiveDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {
    private val collection = firestore.collection(LIVE_COLLECTION)

    suspend fun push(position: LivePositionDto) {
        collection.document(position.uid).set(position).await()
    }

    /** Documents actifs des AUTRES utilisateurs (jamais le mien). */
    fun observeOtherActiveShares(myUid: String): Flow<List<LivePositionDto>> = callbackFlow {
        val registration = collection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val positions = snapshot?.documents
                    ?.mapNotNull { it.toObject(LivePositionDto::class.java) }
                    ?.filter { it.uid != myUid }
                    .orEmpty()
                trySend(positions)
            }
        awaitClose { registration.remove() }
    }
}
