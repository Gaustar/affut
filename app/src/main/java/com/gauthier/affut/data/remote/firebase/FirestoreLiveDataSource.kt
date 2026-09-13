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

    /** Documents actifs des AUTRES utilisateurs qui m'ont inclus dans leur partage (jamais le mien).
     * Filtre sur sharedWithUids (array-contains) plutôt que isActive : une règle Firestore ne peut
     * pas restreindre une requête de LISTE sur un champ que la requête elle-même ne contraint pas
     * ("Missing or insufficient permissions" sur toute la requête, pas un filtrage document par
     * document) — il faut que la requête et la règle portent sur le même champ. isActive/expiresAt
     * restent filtrés côté client, comme avant. */
    fun observeOtherActiveShares(myUid: String): Flow<List<LivePositionDto>> = callbackFlow {
        val registration = collection
            .whereArrayContains("sharedWithUids", myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val positions = snapshot?.documents
                    ?.mapNotNull { it.toObject(LivePositionDto::class.java) }
                    ?.filter { it.uid != myUid && myUid in it.sharedWithUids }
                    .orEmpty()
                trySend(positions)
            }
        awaitClose { registration.remove() }
    }
}
