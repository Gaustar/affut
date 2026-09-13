package com.gauthier.affut.data.remote.firebase.dto

/**
 * Reflète le document Firestore de la collection "spots" (voir section 12 du cahier des charges).
 * Champs var + valeurs par défaut : requis par le mapping automatique de Firestore.
 */
data class SpotDto(
    var id: String = "",
    var ownerId: String = "",
    var title: String = "",
    var type: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var accuracy: Float = 0f,
    var notes: String = "",
    var sharedWithFriendIds: List<String> = emptyList(),
    var sharedWithGroupIds: List<String> = emptyList(),
    var sharedWithUids: List<String> = emptyList(),
    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var observedAt: Long? = null,
)
