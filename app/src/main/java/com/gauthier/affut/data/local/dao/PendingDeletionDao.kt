package com.gauthier.affut.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gauthier.affut.data.local.entity.PendingDeletionEntity

@Dao
interface PendingDeletionDao {
    @Query("SELECT * FROM pending_deletions")
    suspend fun getAll(): List<PendingDeletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingDeletionEntity)

    @Query("DELETE FROM pending_deletions WHERE spotId = :spotId")
    suspend fun delete(spotId: String)
}
