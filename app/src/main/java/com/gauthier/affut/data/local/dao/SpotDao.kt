package com.gauthier.affut.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gauthier.affut.data.local.entity.SpotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpotDao {
    @Query("SELECT * FROM spots ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SpotEntity>>

    @Query("SELECT * FROM spots WHERE id = :id")
    suspend fun getById(id: String): SpotEntity?

    @Upsert
    suspend fun upsert(spot: SpotEntity)

    @Query("DELETE FROM spots WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM spots WHERE pendingSync = 1")
    suspend fun getPendingUpserts(): List<SpotEntity>

    @Query("UPDATE spots SET pendingSync = 0 WHERE id = :id")
    suspend fun clearPendingSync(id: String)

    @Query("SELECT * FROM spots")
    suspend fun getAllOnce(): List<SpotEntity>
}
