package com.gauthier.affut.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Zone de carte téléchargée pour une utilisation hors-ligne. */
@Entity(tableName = "downloaded_regions")
data class DownloadedRegionEntity(
    @PrimaryKey val id: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val zoomMin: Int,
    val zoomMax: Int,
    val downloadedAt: Long,
)
