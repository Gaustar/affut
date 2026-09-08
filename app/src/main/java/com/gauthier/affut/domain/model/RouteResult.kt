package com.gauthier.affut.domain.model

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
)

data class RouteResult(
    val points: List<RoutePoint>,
    val distanceMeters: Double,
    val durationSeconds: Int,
    /** true = itinéraire réel sur les chemins ; false = repli en ligne droite (cap + distance). */
    val followsPaths: Boolean,
    /** true = aucune zone de routage installée, d'où le repli en ligne droite. */
    val missingRoutingData: Boolean = false,
    /** true = le calcul a échoué (erreur, plantage) plutôt que de conclure à une absence de chemin. */
    val computeFailed: Boolean = false,
)
