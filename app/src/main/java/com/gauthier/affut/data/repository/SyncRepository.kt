package com.gauthier.affut.data.repository

import com.gauthier.affut.data.local.dao.PendingDeletionDao
import com.gauthier.affut.data.local.dao.SpotDao
import com.gauthier.affut.data.mapper.toDomain
import com.gauthier.affut.data.mapper.toDto
import com.gauthier.affut.data.mapper.toEntity
import com.gauthier.affut.data.remote.firebase.FirestoreSpotDataSource

class SyncRepository(
    private val spotDao: SpotDao,
    private val pendingDeletionDao: PendingDeletionDao,
    private val remoteDataSource: FirestoreSpotDataSource = FirestoreSpotDataSource(),
) {
    /** Envoie les suppressions, puis les créations/modifications vers Firestore. */
    suspend fun pushPendingChanges() {
        pendingDeletionDao.getAll().forEach { pending ->
            remoteDataSource.delete(pending.spotId)
            pendingDeletionDao.delete(pending.spotId)
        }
        spotDao.getPendingUpserts().forEach { entity ->
            remoteDataSource.push(entity.toDomain().toDto())
            spotDao.clearPendingSync(entity.id)
        }
    }

    /**
     * Récupère mes spots + ceux partagés par l'autre utilisateur, et les fusionne
     * localement. Conflit résolu par "dernière modification gagne" (updatedAt le plus récent).
     * Une modification locale pas encore envoyée n'est jamais écrasée par une version distante plus ancienne.
     */
    suspend fun pullRemoteChanges(uid: String) {
        val remoteSpots = remoteDataSource.fetchVisibleSpots(uid).map { it.toDomain() }
        val remoteIds = remoteSpots.map { it.id }.toSet()

        remoteSpots.forEach { remote ->
            val local = spotDao.getById(remote.id)
            val shouldAdoptRemote = when {
                local == null -> true
                local.pendingSync -> remote.updatedAt > local.updatedAt
                else -> remote.updatedAt > local.updatedAt
            }
            if (shouldAdoptRemote) {
                spotDao.upsert(remote.toEntity().copy(pendingSync = false))
            }
        }

        // Spots supprimés côté serveur (par l'autre utilisateur ou une autre synchro) : on les retire
        // localement, sauf modification locale pas encore envoyée (elle partira au prochain push).
        spotDao.getAllOnce()
            .filter { (it.ownerId == uid || uid in it.sharedWithUids.split(",")) && it.id !in remoteIds && !it.pendingSync }
            .forEach { spotDao.delete(it.id) }
    }
}
