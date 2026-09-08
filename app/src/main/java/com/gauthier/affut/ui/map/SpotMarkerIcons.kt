package com.gauthier.affut.ui.map

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.gauthier.affut.R
import com.gauthier.affut.domain.model.SpotType

fun markerIconFor(context: Context, type: SpotType): Drawable? {
    val resId = when (type) {
        SpotType.BIVOUAC -> R.drawable.marker_bivouac
        SpotType.FROTTIS -> R.drawable.marker_frottis
        SpotType.BOIS_DE_MUE -> R.drawable.marker_bois_de_mue
        SpotType.OBSERVATION -> R.drawable.marker_observation
        SpotType.INDICE -> R.drawable.marker_indice
        SpotType.CHAMPIGNONS -> R.drawable.marker_champignons
        SpotType.AUTRE -> R.drawable.marker_autre
    }
    return ContextCompat.getDrawable(context, resId)
}

/** Couleur du marqueur pour chaque type — doit rester alignée avec res/drawable/marker_*.xml. */
fun markerColorFor(type: SpotType): Color = when (type) {
    SpotType.BIVOUAC -> Color(0xFF8B5A2B)
    SpotType.FROTTIS -> Color(0xFFD98E32)
    SpotType.BOIS_DE_MUE -> Color(0xFFC2B280)
    SpotType.OBSERVATION -> Color(0xFF3B6EA5)
    SpotType.INDICE -> Color(0xFF7B5EA7)
    SpotType.CHAMPIGNONS -> Color(0xFFC2185B)
    SpotType.AUTRE -> Color(0xFF808080)
}
