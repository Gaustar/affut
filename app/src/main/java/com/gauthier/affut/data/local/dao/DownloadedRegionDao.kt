package com.gauthier.affut.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gauthier.affut.data.local.entity.DownloadedRegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedRegionDao {
    @Query("SELECT * FROM downloaded_regions ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadedRegionEntity>>

    @Insert
    suspend fun insert(region: DownloadedRegionEntity)

    @Query("DELETE FROM downloaded_regions WHERE id = :id")
    suspend fun delete(id: String)
}
