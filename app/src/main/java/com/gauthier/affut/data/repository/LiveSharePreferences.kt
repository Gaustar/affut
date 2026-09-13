package com.gauthier.affut.data.repository

import android.content.Context

/** État local minimal du partage (actif/expiration + dernière position en attente d'envoi). */
class LiveSharePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("live_share", Context.MODE_PRIVATE)

    var isActive: Boolean
        get() = prefs.getBoolean(KEY_IS_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ACTIVE, value).apply()

    var expiresAt: Long
        get() = prefs.getLong(KEY_EXPIRES_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_EXPIRES_AT, value).apply()

    var lastSentAt: Long
        get() = prefs.getLong(KEY_LAST_SENT_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SENT_AT, value).apply()

    /** Portée choisie au démarrage du partage, relue à chaque envoi de position (le service
     * tourne indépendamment de l'écran et n'a que ces préférences pour se souvenir à qui envoyer). */
    var sharedWithUids: List<String>
        get() = prefs.getString(KEY_SHARED_WITH_UIDS, "")
            .let { if (it.isNullOrBlank()) emptyList() else it.split(",") }
        set(value) = prefs.edit().putString(KEY_SHARED_WITH_UIDS, value.joinToString(",")).apply()

    fun savePendingPosition(latitude: Double, longitude: Double, accuracy: Float, updatedAt: Long) {
        prefs.edit()
            .putFloat(KEY_PENDING_LAT, latitude.toFloat())
            .putFloat(KEY_PENDING_LON, longitude.toFloat())
            .putFloat(KEY_PENDING_ACCURACY, accuracy)
            .putLong(KEY_PENDING_UPDATED_AT, updatedAt)
            .putBoolean(KEY_HAS_PENDING, true)
            .apply()
    }

    fun getPendingPosition(): PendingPosition? {
        if (!prefs.getBoolean(KEY_HAS_PENDING, false)) return null
        return PendingPosition(
            latitude = prefs.getFloat(KEY_PENDING_LAT, 0f).toDouble(),
            longitude = prefs.getFloat(KEY_PENDING_LON, 0f).toDouble(),
            accuracy = prefs.getFloat(KEY_PENDING_ACCURACY, 0f),
            updatedAt = prefs.getLong(KEY_PENDING_UPDATED_AT, 0L),
        )
    }

    fun clearPendingPosition() {
        prefs.edit().putBoolean(KEY_HAS_PENDING, false).apply()
    }

    /** Réinitialise tout l'état temporaire — appelé à l'arrêt manuel ou à l'expiration. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    data class PendingPosition(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val updatedAt: Long,
    )

    private companion object {
        const val KEY_IS_ACTIVE = "is_active"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_LAST_SENT_AT = "last_sent_at"
        const val KEY_SHARED_WITH_UIDS = "shared_with_uids"
        const val KEY_HAS_PENDING = "has_pending"
        const val KEY_PENDING_LAT = "pending_lat"
        const val KEY_PENDING_LON = "pending_lon"
        const val KEY_PENDING_ACCURACY = "pending_accuracy"
        const val KEY_PENDING_UPDATED_AT = "pending_updated_at"
    }
}
