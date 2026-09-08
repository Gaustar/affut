package com.gauthier.affut.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gauthier.affut.data.repository.LiveShareRepository

/** Envoie la dernière position en attente dès que le réseau revient. */
class LiveShareSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            LiveShareRepository.create(applicationContext).retryPendingPosition()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
