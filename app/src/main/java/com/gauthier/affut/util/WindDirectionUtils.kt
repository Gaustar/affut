package com.gauthier.affut.util

private val DIRECTIONS = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")

/** Convertit un cap en degrés (0-360) en direction cardinale française (N, NE, E...). */
fun windDirectionLabel(degrees: Double): String {
    val normalized = ((degrees % 360) + 360) % 360
    val index = (((normalized + 22.5) / 45).toInt()) % 8
    return DIRECTIONS[index]
}

/**
 * Relation entre le cap du téléphone (direction vers laquelle on est tourné) et la provenance du vent.
 * [windFromDeg] : direction d'où vient le vent (convention météo standard).
 */
fun windRelationLabel(phoneHeadingDeg: Float, windFromDeg: Double): String {
    val relative = ((windFromDeg - phoneHeadingDeg) % 360 + 360) % 360
    return when {
        relative <= 45.0 || relative >= 315.0 -> "Vent de face"
        relative in 135.0..225.0 -> "Vent de dos"
        else -> "Vent de travers"
    }
}
