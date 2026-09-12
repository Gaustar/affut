package com.gauthier.affut.util

/**
 * true si [remoteVersion] est strictement plus récente que [currentVersion].
 * Compare composant par composant ("1.9" < "1.10", pas une comparaison de texte brut),
 * tolère un préfixe "v" (tags GitHub du type "v1.2"). Ne renvoie jamais un faux positif :
 * un format illisible retourne false plutôt que de risquer d'annoncer une mise à jour fictive.
 */
fun isNewerVersion(currentVersion: String, remoteVersion: String): Boolean {
    val current = parseVersionParts(currentVersion) ?: return false
    val remote = parseVersionParts(remoteVersion) ?: return false
    val length = maxOf(current.size, remote.size)
    for (i in 0 until length) {
        val c = current.getOrElse(i) { 0 }
        val r = remote.getOrElse(i) { 0 }
        if (r != c) return r > c
    }
    return false
}

private fun parseVersionParts(version: String): List<Int>? {
    val cleaned = version.trim().removePrefix("v").removePrefix("V")
    if (cleaned.isBlank()) return null
    val numbers = cleaned.split(".").map { it.trim().toIntOrNull() }
    return if (numbers.all { it != null }) numbers.filterNotNull() else null
}
