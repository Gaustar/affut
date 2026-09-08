package com.gauthier.affut.data.remote.firebase.dto

/** Reflète le document Firestore de la collection "live" (un document par utilisateur, écrasé à chaque MAJ). */
data class LivePositionDto(
    var uid: String = "",
    var isActive: Boolean = false,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var accuracy: Float = 0f,
    var updatedAt: Long = 0,
    var expiresAt: Long = 0,
)
