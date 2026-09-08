package com.gauthier.affut.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Suppression effectuée localement, en attente d'être répercutée sur Firestore. */
@Entity(tableName = "pending_deletions")
data class PendingDeletionEntity(
    @PrimaryKey val spotId: String,
    val deletedAt: Long,
)
