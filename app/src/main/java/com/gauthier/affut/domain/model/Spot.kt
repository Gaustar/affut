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
    /** Amis et groupes choisis pour le partage — reflètent la sélection faite à l'écran. */
    val sharedWithFriendIds: List<String> = emptyList(),
    val sharedWithGroupIds: List<String> = emptyList(),
    /** UID à jour au moment de l'enregistrement (amis + membres des groupes choisis, aplatis).
     * C'est ce champ, pas les deux précédents, que lisent les règles Firestore. */
    val sharedWithUids: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    /** Date de l'observation sur le terrain (bois de mue, champignons…), choisie par l'utilisateur. */
    val observedAt: Long? = null,
)
