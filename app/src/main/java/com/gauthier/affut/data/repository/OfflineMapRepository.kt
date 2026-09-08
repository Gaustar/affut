package com.gauthier.affut.data.repository

import android.content.Context
import com.gauthier.affut.data.local.AppDatabase
import com.gauthier.affut.data.local.entity.DownloadedRegionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import java.util.UUID
import kotlin.coroutines.resume

class OfflineMapRepository(
    private val context: Context,
    private val regionDao: com.gauthier.affut.data.local.dao.DownloadedRegionDao,
) {
    fun observeRegions(): Flow<List<DownloadedRegionEntity>> = regionDao.observeAll()

    suspend fun deleteRegion(id: String) = regionDao.delete(id)

    /** Télécharge les tuiles de [boundingBox] pour les niveaux [zoomMin]..[zoomMax]. Retourne true si succès. */
    suspend fun downloadRegion(
        mapView: MapView,
        boundingBox: BoundingBox,
        zoomMin: Int,
        zoomMax: Int,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Boolean {
        var total = 1
        val success = suspendCancellableCoroutine<Boolean> { continuation ->
            val cacheManager = CacheManager(mapView)
            cacheManager.downloadAreaAsync(
                context,
                boundingBox,
                zoomMin,
                zoomMax,
                object : CacheManager.CacheManagerCallback {
                    override fun onTaskComplete() {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onTaskFailed(errors: Int) {
                        if (continuation.isActive) continuation.resume(false)
                    }

                    override fun updateProgress(progress: Int, currentZoomLevel: Int, zoommin: Int, zoommax: Int) {
                        onProgress(progress, total)
                    }

                    override fun downloadStarted() = Unit

                    override fun setPossibleTilesInArea(totalTiles: Int) {
                        total = totalTiles
                    }
                },
            )
        }

        if (success) {
            regionDao.insert(
                DownloadedRegionEntity(
                    id = UUID.randomUUID().toString(),
                    minLat = boundingBox.latSouth,
                    maxLat = boundingBox.latNorth,
                    minLon = boundingBox.lonWest,
                    maxLon = boundingBox.lonEast,
                    zoomMin = zoomMin,
                    zoomMax = zoomMax,
                    downloadedAt = System.currentTimeMillis(),
                ),
            )
        }
        return success
    }

    companion object {
        fun create(context: Context): OfflineMapRepository =
            OfflineMapRepository(context, AppDatabase.getInstance(context).downloadedRegionDao())
    }
}
