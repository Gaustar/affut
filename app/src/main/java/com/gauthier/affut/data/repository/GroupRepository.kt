package com.gauthier.affut.data.repository

import com.gauthier.affut.data.remote.firebase.FirestoreGroupDataSource
import com.gauthier.affut.data.remote.firebase.FirestoreUserDataSource
import com.gauthier.affut.domain.model.Group
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class GroupRepository(
    private val remoteDataSource: FirestoreGroupDataSource = FirestoreGroupDataSource(),
    private val userDataSource: FirestoreUserDataSource = FirestoreUserDataSource(),
) {
    suspend fun createGroup(name: String): Result<Group> {
        val myUid = Firebase.auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Non connecté"))
        val dto = remoteDataSource.createGroup(name.trim(), myUid)
        userDataSource.addGroupId(myUid, dto.id)
        return Result.success(Group(dto.id, dto.name, dto.code, dto.createdBy, memberCount = 1))
    }

    suspend fun joinGroupByCode(code: String): Result<Group> {
        val myUid = Firebase.auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Non connecté"))
        val dto = remoteDataSource.findByCode(code.trim().uppercase())
            ?: return Result.failure(NoSuchElementException("Aucun groupe avec ce code"))
        remoteDataSource.joinGroup(dto.id, myUid)
        userDataSource.addGroupId(myUid, dto.id)
        val memberCount = remoteDataSource.listMemberUids(dto.id).size
        return Result.success(Group(dto.id, dto.name, dto.code, dto.createdBy, memberCount))
    }

    suspend fun leaveGroup(groupId: String) {
        val myUid = Firebase.auth.currentUser?.uid ?: return
        remoteDataSource.leaveGroup(groupId, myUid)
        userDataSource.removeGroupId(myUid, groupId)
    }

    /** Dénormalisé sur mon propre profil (users/{uid}.groupIds) plutôt que retrouvé par une
     * requête collectionGroup sur "members" à travers tous les groupes : Firestore refuse une
     * requête de LISTE dont la règle dépend d'un champ que la requête ne contraint pas elle-même
     * (même souci que le partage de position, voir FirestoreLiveDataSource) — testé en conditions
     * réelles le 2026-09-13 : "Missing or insufficient permissions" sur la requête entière. */
    suspend fun listMyGroups(): List<Group> {
        val myUid = Firebase.auth.currentUser?.uid ?: return emptyList()
        val groupIds = userDataSource.getGroupIds(myUid)
        return groupIds.mapNotNull { id ->
            val dto = remoteDataSource.getGroup(id) ?: return@mapNotNull null
            val memberCount = remoteDataSource.listMemberUids(id).size
            Group(dto.id, dto.name, dto.code, dto.createdBy, memberCount)
        }
    }

    /** UID de tous les membres actuels d'un groupe — utilisé pour "aplatir" un partage par
     * groupe en une liste d'UID au moment où un spot/une position est enregistré(e). */
    suspend fun listMemberUids(groupId: String): List<String> = remoteDataSource.listMemberUids(groupId)

    suspend fun getGroupName(groupId: String): String =
        remoteDataSource.getGroup(groupId)?.name ?: groupId.take(6)

    /** Aplatit une portée de partage (amis + groupes) en une simple liste d'UID, dénormalisée
     * au moment de l'enregistrement. Un membre qui quitte un groupe après coup ne perd pas
     * rétroactivement l'accès aux spots déjà partagés — limite connue, acceptée pour rester
     * simple (pas de fonction serveur pour recalculer ça en tâche de fond).
     *
     * Hors-ligne (ou tout autre échec Firestore), un groupe ne peut pas être résolu : plutôt
     * que de faire planter l'enregistrement (justement l'inverse de "hors-ligne d'abord"), on
     * dégrade en gardant les amis individuels déjà connus localement, et on ignore ce groupe
     * pour cette fois — le prochain enregistrement du spot le résoudra si le réseau revient. */
    suspend fun resolveSharedUids(friendIds: List<String>, groupIds: List<String>): List<String> {
        val fromGroups = groupIds.flatMap { groupId ->
            runCatching { listMemberUids(groupId) }.getOrDefault(emptyList())
        }
        return (friendIds + fromGroups).distinct()
    }

    companion object {
        fun create(): GroupRepository = GroupRepository()
    }
}
