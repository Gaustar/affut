package com.gauthier.affut.data.remote.firebase.dto

/** Reflète un document Firestore de la collection "groups". Pas d'administrateur : quiconque
 * connaît le code rejoint librement (voir "groups/{id}/members"), et peut repartir de même. */
data class GroupDto(
    var id: String = "",
    var name: String = "",
    var code: String = "",
    var createdBy: String = "",
    var createdAt: Long = 0,
)

/** Document de la sous-collection "groups/{groupId}/members", un par membre (id du document = uid). */
data class GroupMemberDto(
    var uid: String = "",
    var joinedAt: Long = 0,
)
