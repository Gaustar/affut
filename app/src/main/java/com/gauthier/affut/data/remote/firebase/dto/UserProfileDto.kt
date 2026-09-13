package com.gauthier.affut.data.remote.firebase.dto

/** Reflète le document Firestore de la collection "users" (un document par compte). */
data class UserProfileDto(
    var uid: String = "",
    var displayName: String = "",
    /** Code court communiqué de vive voix/par message pour être ajouté comme ami. */
    var friendCode: String = "",
    var createdAt: Long = 0,
    /** Dénormalisé plutôt que retrouvé par une requête collectionGroup sur "members" : Firestore
     * refuse une requête de liste dont la règle dépend d'un champ que la requête elle-même ne
     * contraint pas (même souci que pour le partage de position, voir FirestoreLiveDataSource). */
    var groupIds: List<String> = emptyList(),
)
