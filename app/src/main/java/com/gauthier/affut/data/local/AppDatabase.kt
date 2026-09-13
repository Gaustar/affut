package com.gauthier.affut.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gauthier.affut.data.local.dao.DownloadedRegionDao
import com.gauthier.affut.data.local.dao.PendingDeletionDao
import com.gauthier.affut.data.local.dao.SpotDao
import com.gauthier.affut.data.local.dao.WeatherCacheDao
import com.gauthier.affut.data.local.entity.DownloadedRegionEntity
import com.gauthier.affut.data.local.entity.PendingDeletionEntity
import com.gauthier.affut.data.local.entity.SpotEntity
import com.gauthier.affut.data.local.entity.WeatherCacheEntity

@Database(
    entities = [
        SpotEntity::class,
        PendingDeletionEntity::class,
        DownloadedRegionEntity::class,
        WeatherCacheEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spotDao(): SpotDao
    abstract fun pendingDeletionDao(): PendingDeletionDao
    abstract fun downloadedRegionDao(): DownloadedRegionDao
    abstract fun weatherCacheDao(): WeatherCacheDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "affut.db",
                )
                    // App en développement, pas encore distribuée : une migration destructive
                    // est acceptable pour les changements de schéma tant qu'il n'y a pas de vraies données à préserver.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
