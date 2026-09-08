package com.gauthier.affut

import android.app.Application
import com.gauthier.affut.sync.SyncScheduler
import org.osmdroid.config.Configuration

class AffutApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            load(this@AffutApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
        SyncScheduler.schedulePeriodic(this)
        // Récupère tout de suite les changements de l'autre utilisateur : le travail périodique
        // seul pouvait laisser attendre un quart d'heure après une installation.
        SyncScheduler.scheduleNow(this)
    }
}
