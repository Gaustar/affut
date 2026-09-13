package com.gauthier.affut.data.remote.firebase

import com.gauthier.affut.data.remote.firebase.dto.FriendshipDto
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

private const val FRIENDSHIPS_COLLECTION = "friendships"

/** Id déterministe : les deux UID triés, pour qu'une amitié n'existe jamais qu'une fois. */
fun friendshipDocId(uidA: String, uidB: String): String =
    listOf(uidA, uidB).sorted().joinToString("_")

class FirestoreFriendshipDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {
    private val collection = firestore.collection(FRIENDSHIPS_COLLECTION)

    suspend fun addFriend(myUid: String, otherUid: String) {
        val (uidA, uidB) = listOf(myUid, otherUid).sorted()
        collection.document(friendshipDocId(myUid, otherUid)).set(
            FriendshipDto(uidA = uidA, uidB = uidB, createdAt = System.currentTimeMillis()),
        ).await()
    }

    suspend fun removeFriend(myUid: String, otherUid: String) {
        collection.document(friendshipDocId(myUid, otherUid)).delete().await()
    }

    /** UID de tous les amis de myUid (l'autre moitié de chaque paire). */
    suspend fun listFriendUids(myUid: String): List<String> {
        val snapshot = collection
            .where(Filter.or(Filter.equalTo("uidA", myUid), Filter.equalTo("uidB", myUid)))
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(FriendshipDto::class.java) ?: return@mapNotNull null
            if (dto.uidA == myUid) dto.uidB else dto.uidA
        }
    }
}
