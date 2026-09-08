package com.gauthier.affut.domain.model

/** Modèle métier d'un spot, indépendant de Room et de Firestore. */
data class Spot(
    val id: String,
    val ownerId: String,
    val title: String,
    val type: SpotType,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val notes: String,
    val isShared: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    /** Date de l'observation sur le terrain (bois de mue, champignons…), choisie par l'utilisateur. */
    val observedAt: Long? = null,
)
