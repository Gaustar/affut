package com.gauthier.affut.data.remote.firebase.dto

/**
 * Reflète un document Firestore de la collection "friendships". L'identifiant du document
 * est toujours les deux UID triés et joints par "_", ce qui rend l'amitié mutuelle dès sa
 * création par l'un des deux comptes (pas d'étape d'acceptation séparée : échanger le code
 * suffit, dans l'esprit "entre proches" de l'application).
 */
data class FriendshipDto(
    var uidA: String = "",
    var uidB: String = "",
    var createdAt: Long = 0,
)
