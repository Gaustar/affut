package com.gauthier.affut.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncState(
    val lastSuccessAt: Long = 0L,
    val lastFailureAt: Long = 0L,
    val lastError: String? = null,
) {
    /** true quand la dernière tentative a échoué et qu'aucune n'a réussi depuis. */
    val isFailing: Boolean get() = lastError != null && lastFailureAt > lastSuccessAt
}

/**
 * État de la dernière synchronisation, pour pouvoir le montrer à l'écran.
 * Sans ça, un échec répété reste totalement invisible : l'utilisateur croit ses spots
 * partagés alors que rien n'est jamais parti.
 */
object SyncStatus {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    fun reportSuccess() {
        _state.value = SyncState(lastSuccessAt = System.currentTimeMillis())
    }

    fun reportFailure(error: Throwable) {
        _state.value = _state.value.copy(
            lastFailureAt = System.currentTimeMillis(),
            lastError = error.message ?: error::class.java.simpleName,
        )
    }
}
