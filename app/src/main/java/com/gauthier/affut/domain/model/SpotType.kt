package com.gauthier.affut.domain.model

enum class SpotType(val label: String) {
    BIVOUAC("Bivouac"),
    FROTTIS("Frottis"),
    BOIS_DE_MUE("Bois de mue"),
    OBSERVATION("Observation"),
    INDICE("Indice"),
    CHAMPIGNONS("Champignons"),
    AUTRE("Autre");

    companion object {
        fun fromStorageValue(value: String): SpotType =
            entries.find { it.name == value } ?: AUTRE
    }
}
