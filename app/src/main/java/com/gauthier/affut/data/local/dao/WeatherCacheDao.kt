package com.gauthier.affut.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gauthier.affut.data.local.entity.WeatherCacheEntity

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE locationKey = :key")
    suspend fun get(key: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherCacheEntity)
}
