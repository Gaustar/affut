package com.gauthier.affut.domain.model

/** Mise à jour disponible détectée sur GitHub Releases. */
data class AppUpdate(
    /** Ex: "v1.1" — tel qu'affiché, pas reparsé. */
    val versionLabel: String,
    /** Page GitHub de la release, ouverte dans le navigateur pour télécharger l'APK. */
    val releaseUrl: String,
)
