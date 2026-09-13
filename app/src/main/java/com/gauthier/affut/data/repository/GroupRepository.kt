package com.gauthier.affut.data.repository

import com.gauthier.affut.data.remote.firebase.FirestoreGroupDataSource
import com.gauthier.affut.domain.model.Group
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class GroupRepository(
    private val remoteDataSource: FirestoreGroupDataSource = FirestoreGroupDataSource(),
) {
    suspend fun createGroup(name: String): Result<Group> {
        val myUid = Firebase.auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Non connecté"))
        val dto = remoteDataSource.createGroup(name.trim(), myUid)
        return Result.success(Group(dto.id, dto.name, dto.code, dto.createdBy, memberCount = 1))
    }

    suspend fun joinGroupByCode(code: String): Result<Group> {
        val myUid = Firebase.auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Non connecté"))
        val dto = remoteDataSource.findByCode(code.trim().uppercase())
            ?: return Result.failure(NoSuchElementException("Aucun groupe avec ce code"))
        remoteDataSource.joinGroup(dto.id, myUid)
        val memberCount = remoteDataSource.listMemberUids(dto.id).size
        return Result.success(Group(dto.id, dto.name, dto.code, dto.createdBy, memberCount))
    }

    suspend fun leaveGroup(groupId: String) {
        val myUid = Firebase.auth.currentUser?.uid ?: return
        remoteDataSource.leaveGroup(groupId, myUid)
    }

    suspend fun listMyGroups(): List<Group> {
        val myUid = Firebase.auth.currentUser?.uid ?: return emptyList()
        return remoteDataSource.listMyGroups(myUid).map { dto ->
            val memberCount = remoteDataSource.listMemberUids(dto.id).size
            Group(dto.id, dto.name, dto.code, dto.createdBy, memberCount)
        }
    }

    /** UID de tous les membres actuels d'un groupe — utilisé pour "aplatir" un partage par
     * groupe en une liste d'UID au moment où un spot/une position est enregistré(e). */
    suspend fun listMemberUids(groupId: String): List<String> = remoteDataSource.listMemberUids(groupId)

    /** Aplatit une portée de partage (amis + groupes) en une simple liste d'UID, dénormalisée
     * au moment de l'enregistrement. Un membre qui quitte un groupe après coup ne perd pas
     * rétroactivement l'accès aux spots déjà partagés — limite connue, acceptée pour rester
     * simple (pas de fonction serveur pour recalculer ça en tâche de fond). */
    suspend fun resolveSharedUids(friendIds: List<String>, groupIds: List<String>): List<String> {
        val fromGroups = groupIds.flatMap { listMemberUids(it) }
        return (friendIds + fromGroups).distinct()
    }

    companion object {
        fun create(): GroupRepository = GroupRepository()
    }
}
