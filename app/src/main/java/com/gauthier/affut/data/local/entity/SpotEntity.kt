package com.gauthier.affut.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spots")
data class SpotEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val title: String,
    /** Nom de la constante SpotType (ex: "BIVOUAC"), stocké en texte. */
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val notes: String,
    val isShared: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val observedAt: Long? = null,
    /** true = modification locale pas encore envoyée à Firestore. */
    val pendingSync: Boolean = true,
)
