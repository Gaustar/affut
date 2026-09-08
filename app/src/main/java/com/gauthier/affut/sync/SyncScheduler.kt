package com.gauthier.affut.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_NOW = "spot_sync_now"
private const val UNIQUE_WORK_PERIODIC = "spot_sync_periodic"
private const val UNIQUE_WORK_LIVE_SHARE_RETRY = "live_share_retry"

/**
 * Planifie la synchro via WorkManager, avec contrainte réseau : si hors-ligne,
 * WorkManager attend automatiquement le retour du réseau pour l'exécuter.
 */
object SyncScheduler {
    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** À appeler juste après chaque création/modification/suppression locale. */
    fun scheduleNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SpotSyncWorker>()
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    /** À appeler quand l'envoi d'une position live échoue faute de réseau. */
    fun scheduleLiveShareRetry(context: Context) {
        val request = OneTimeWorkRequestBuilder<LiveShareSyncWorker>()
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_LIVE_SHARE_RETRY, ExistingWorkPolicy.REPLACE, request)
    }

    /** À appeler une fois au démarrage de l'app, pour récupérer les changements de l'autre utilisateur. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SpotSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
