package com.gauthier.affut.data.remote.firebase.dto

/** Reflète le document Firestore de la collection "users" (un document par compte). */
data class UserProfileDto(
    var uid: String = "",
    var displayName: String = "",
    /** Code court communiqué de vive voix/par message pour être ajouté comme ami. */
    var friendCode: String = "",
    var createdAt: Long = 0,
)
