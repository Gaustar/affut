package com.gauthier.affut.data.remote.firebase

import com.gauthier.affut.data.remote.firebase.dto.GroupDto
import com.gauthier.affut.data.remote.firebase.dto.GroupMemberDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

private const val GROUPS_COLLECTION = "groups"
private const val MEMBERS_SUBCOLLECTION = "members"
private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private const val CODE_LENGTH = 6

class FirestoreGroupDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {
    private val collection = firestore.collection(GROUPS_COLLECTION)

    suspend fun createGroup(name: String, createdBy: String): GroupDto {
        var code: String
        do {
            code = randomCode()
        } while (findByCode(code) != null)

        val group = GroupDto(
            id = UUID.randomUUID().toString(),
            name = name,
            code = code,
            createdBy = createdBy,
            createdAt = System.currentTimeMillis(),
        )
        collection.document(group.id).set(group).await()
        joinGroup(group.id, createdBy)
        return group
    }

    suspend fun findByCode(code: String): GroupDto? {
        val snapshot = collection.whereEqualTo("code", code).limit(1).get().await()
        return snapshot.documents.firstOrNull()?.toObject(GroupDto::class.java)
    }

    suspend fun getGroup(groupId: String): GroupDto? =
        collection.document(groupId).get().await().toObject(GroupDto::class.java)

    suspend fun joinGroup(groupId: String, uid: String) {
        collection.document(groupId).collection(MEMBERS_SUBCOLLECTION).document(uid)
            .set(GroupMemberDto(uid = uid, joinedAt = System.currentTimeMillis())).await()
    }

    suspend fun leaveGroup(groupId: String, uid: String) {
        collection.document(groupId).collection(MEMBERS_SUBCOLLECTION).document(uid).delete().await()
    }

    suspend fun listMemberUids(groupId: String): List<String> {
        val snapshot = collection.document(groupId).collection(MEMBERS_SUBCOLLECTION).get().await()
        return snapshot.documents.map { it.id }
    }

    /** Tous les groupes dont je suis membre, en interrogeant la sous-collection "members" à travers tous les groupes. */
    suspend fun listMyGroups(uid: String): List<GroupDto> {
        val membershipDocs = firestore.collectionGroup(MEMBERS_SUBCOLLECTION)
            .whereEqualTo("uid", uid)
            .get()
            .await()
        val groupIds = membershipDocs.documents.mapNotNull { it.reference.parent.parent?.id }
        return groupIds.mapNotNull { getGroup(it) }
    }

    private fun randomCode(): String =
        (1..CODE_LENGTH).map { CODE_CHARS[Random.nextInt(CODE_CHARS.length)] }.joinToString("")
}
