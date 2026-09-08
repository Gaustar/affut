package com.gauthier.affut.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gauthier.affut.data.local.AppDatabase
import com.gauthier.affut.data.repository.SyncRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SpotSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()
        val db = AppDatabase.getInstance(applicationContext)
        val syncRepository = SyncRepository(db.spotDao(), db.pendingDeletionDao())

        return try {
            syncRepository.pushPendingChanges()
            syncRepository.pullRemoteChanges(uid)
            SyncStatus.reportSuccess()
            Result.success()
        } catch (e: Exception) {
            // Remonté à l'écran : un échec répété doit être visible, pas seulement réessayé.
            SyncStatus.reportFailure(e)
            android.util.Log.w("AffutSync", "echec de synchronisation", e)
            Result.retry()
        }
    }
}
