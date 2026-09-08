package com.gauthier.affut.data.repository

import android.content.Context
import com.gauthier.affut.data.local.AppDatabase
import com.gauthier.affut.data.local.dao.PendingDeletionDao
import com.gauthier.affut.data.local.dao.SpotDao
import com.gauthier.affut.data.local.entity.PendingDeletionEntity
import com.gauthier.affut.data.mapper.toDomain
import com.gauthier.affut.data.mapper.toEntity
import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SpotRepository(
    private val context: Context,
    private val spotDao: SpotDao,
    private val pendingDeletionDao: PendingDeletionDao,
) {
    fun observeSpots(): Flow<List<Spot>> =
        spotDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getById(id: String): Spot? = spotDao.getById(id)?.toDomain()

    suspend fun upsert(spot: Spot) {
        spotDao.upsert(spot.toEntity())
        SyncScheduler.scheduleNow(context)
    }

    suspend fun delete(id: String) {
        pendingDeletionDao.insert(PendingDeletionEntity(id, System.currentTimeMillis()))
        spotDao.delete(id)
        SyncScheduler.scheduleNow(context)
    }

    companion object {
        fun create(context: Context): SpotRepository {
            val db = AppDatabase.getInstance(context)
            return SpotRepository(context, db.spotDao(), db.pendingDeletionDao())
        }
    }
}
