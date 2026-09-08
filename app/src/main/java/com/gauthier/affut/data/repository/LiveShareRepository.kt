package com.gauthier.affut.data.repository

import android.content.Context
import com.gauthier.affut.data.remote.firebase.FirestoreLiveDataSource
import com.gauthier.affut.data.remote.firebase.dto.LivePositionDto
import com.gauthier.affut.service.LiveShareForegroundService
import com.gauthier.affut.sync.SyncScheduler
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class LiveShareRepository(
    private val context: Context,
    private val preferences: LiveSharePreferences,
    private val remoteDataSource: FirestoreLiveDataSource = FirestoreLiveDataSource(),
) {
    val isActive: Boolean get() = preferences.isActive
    val expiresAt: Long get() = preferences.expiresAt
    val lastSentAt: Long get() = preferences.lastSentAt

    /** true quand un envoi a échoué et que la position attend toujours de partir. */
    val hasUnsentPosition: Boolean get() = preferences.getPendingPosition() != null

    fun start(durationMillis: Long) {
        preferences.isActive = true
        preferences.expiresAt = System.currentTimeMillis() + durationMillis
        LiveShareForegroundService.start(context)
    }

    suspend fun stop() {
        val uid = Firebase.auth.currentUser?.uid
        preferences.clearAll()
        if (uid != null) {
            runCatching {
                remoteDataSource.push(
                    LivePositionDto(uid = uid, isActive = false, updatedAt = System.currentTimeMillis()),
                )
            }
        }
        LiveShareForegroundService.stop(context)
    }

    /** Envoie la position ; si hors-ligne, ne garde que la dernière en attente (jamais d'historique). */
    suspend fun onLocationUpdate(latitude: Double, longitude: Double, accuracy: Float) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        val position = LivePositionDto(
            uid = uid,
            isActive = true,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            updatedAt = now,
            expiresAt = preferences.expiresAt,
        )
        try {
            remoteDataSource.push(position)
            preferences.clearPendingPosition()
            preferences.lastSentAt = now
        } catch (e: Exception) {
            preferences.savePendingPosition(latitude, longitude, accuracy, now)
            SyncScheduler.scheduleLiveShareRetry(context)
        }
    }

    suspend fun retryPendingPosition() {
        val pending = preferences.getPendingPosition() ?: return
        onLocationUpdate(pending.latitude, pending.longitude, pending.accuracy)
    }

    fun observeOtherActiveShares(): Flow<List<LivePositionDto>> {
        val uid = Firebase.auth.currentUser?.uid ?: return flowOf(emptyList())
        return remoteDataSource.observeOtherActiveShares(uid)
    }

    companion object {
        fun create(context: Context): LiveShareRepository =
            LiveShareRepository(context, LiveSharePreferences(context))
    }
}
