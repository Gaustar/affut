package com.gauthier.affut.data.routing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NavigationTarget(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

/**
 * Destination en cours de guidage, partagée entre les écrans (on la choisit depuis la fiche d'un
 * spot ou depuis le partage de position, mais c'est la carte qui affiche l'itinéraire).
 * Volontairement en mémoire seulement : un guidage ne survit pas à la fermeture de l'app.
 */
object NavigationTargets {
    private val _target = MutableStateFlow<NavigationTarget?>(null)
    val target: StateFlow<NavigationTarget?> = _target.asStateFlow()

    fun start(target: NavigationTarget) {
        _target.value = target
    }

    fun stop() {
        _target.value = null
    }
}
