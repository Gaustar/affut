package com.gauthier.affut.data.routing

import android.content.Context
import btools.router.OsmNodeNamed
import btools.router.RoutingContext
import btools.router.RoutingEngine
import com.gauthier.affut.domain.model.RoutePoint
import com.gauthier.affut.domain.model.RouteResult
import com.gauthier.affut.util.haversineMeters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PROFILE_NAME = "foret"
private const val PROFILE_FILE = "$PROFILE_NAME.brf"
private const val LOOKUPS_FILE = "lookups.dat"
private const val ROUTING_TIMEOUT_MS = 30_000L

/**
 * Calcul d'itinéraire 100 % hors-ligne via BRouter (aucun serveur, aucune connexion).
 *
 * Nécessite des fichiers de données `.rd5` dans [segmentsDir] : sans eux, le routage sur chemins
 * est impossible et on retombe sur un guidage en ligne droite (cap + distance).
 */
class OfflineRouter(private val context: Context) {

    // Stockage externe applicatif : accessible en USB depuis un PC
    // (Android/data/com.gauthier.affut/files/brouter/segments4), pour y déposer les .rd5 à la main.
    private val baseDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "brouter")
    private val segmentsDir = File(baseDir, "segments4")
    private val profilesDir = File(baseDir, "profiles2")

    /** Dossier où déposer les fichiers .rd5 téléchargés (une fois, depuis un PC ou en wifi). */
    fun segmentsDirectory(): File = segmentsDir.apply { mkdirs() }

    /** true si au moins une zone de routage est installée. */
    fun hasRoutingData(): Boolean =
        segmentsDir.listFiles { f -> f.name.endsWith(".rd5") }?.isNotEmpty() == true

    /**
     * Itinéraire de [from] vers [to] en suivant les chemins.
     * Retourne un tracé en ligne droite si les données manquent ou si aucun chemin n'est trouvé
     * (cas courant : un affût en plein bois, hors de tout sentier cartographié).
     */
    suspend fun route(from: RoutePoint, to: RoutePoint): RouteResult = withContext(Dispatchers.IO) {
        if (!hasRoutingData()) return@withContext straightLine(from, to, RouteMode.NO_DATA)

        runCatching { computeWithBRouter(from, to) }
            .onFailure { android.util.Log.w("AffutRouting", "echec routage", it) }
            .fold(
                onSuccess = { it ?: straightLine(from, to, RouteMode.NO_PATH_FOUND) },
                // Une exception (profil corrompu, données abîmées…) est une vraie panne, distincte
                // du cas normal "aucun chemin cartographié par ici" — sinon l'utilisateur ne peut
                // jamais savoir s'il faut réessayer ou si le hors-chemin est simplement attendu.
                onFailure = { straightLine(from, to, RouteMode.COMPUTE_ERROR) },
            )
    }

    private fun computeWithBRouter(from: RoutePoint, to: RoutePoint): RouteResult? {
        installProfilesIfNeeded()

        System.setProperty("segmentBaseDir", segmentsDir.absolutePath)
        System.setProperty("profileBaseDir", profilesDir.absolutePath)

        val routingContext = RoutingContext().apply {
            // Nom nu : avec profileBaseDir défini, BRouter compose lui-même dossier + nom + ".brf".
            localFunction = PROFILE_NAME
        }
        // Liste mutable obligatoire : BRouter vide et réécrit cette liste pendant le calcul.
        val waypoints = mutableListOf(from.toOsmNode("départ"), to.toOsmNode("arrivée"))

        // BRouter ne renseigne sa trace que lorsqu'un fichier de sortie est demandé (c'est aussi ce
        // que fait son app officielle). On le dirige vers le cache, et on vide ce dossier avant chaque
        // calcul : sinon, un itinéraire identique au précédent est considéré déjà écrit et ignoré.
        val outputDir = File(context.cacheDir, "brouter").apply { mkdirs() }
        outputDir.listFiles()?.forEach { it.delete() }
        val outfileBase = File(outputDir, "track").absolutePath

        val engine = RoutingEngine(outfileBase, null, segmentsDir, waypoints, routingContext)
        engine.doRun(ROUTING_TIMEOUT_MS)

        if (engine.errorMessage != null) {
            android.util.Log.w("AffutRouting", "BRouter: ${engine.errorMessage}")
            return null
        }
        val track = engine.foundTrack
        if (track == null || track.nodes.isNullOrEmpty()) {
            android.util.Log.w("AffutRouting", "BRouter: trace vide (track=$track)")
            return null
        }

        val points = track.nodes.map { node ->
            RoutePoint(
                latitude = node.iLat / 1_000_000.0 - 90.0,
                longitude = node.iLon / 1_000_000.0 - 180.0,
            )
        }
        return RouteResult(
            points = points,
            distanceMeters = track.distance.toDouble(),
            durationSeconds = track.totalSeconds,
            followsPaths = true,
        )
    }

    /** Repli : segment direct, avec la distance réelle à vol d'oiseau. */
    private fun straightLine(from: RoutePoint, to: RoutePoint, mode: RouteMode): RouteResult {
        val distance = haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
        return RouteResult(
            points = listOf(from, to),
            distanceMeters = distance,
            // ~4 km/h en terrain forestier.
            durationSeconds = (distance / 1.1).toInt(),
            followsPaths = false,
            missingRoutingData = mode == RouteMode.NO_DATA,
            computeFailed = mode == RouteMode.COMPUTE_ERROR,
        )
    }

    private enum class RouteMode { NO_DATA, NO_PATH_FOUND, COMPUTE_ERROR }

    /** Copie le profil et la table de correspondance des tags depuis les assets (une seule fois). */
    private fun installProfilesIfNeeded() {
        profilesDir.mkdirs()
        listOf(PROFILE_FILE, LOOKUPS_FILE).forEach { name ->
            val target = File(profilesDir, name)
            if (!target.exists()) {
                context.assets.open("brouter/$name").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}

private fun RoutePoint.toOsmNode(label: String): OsmNodeNamed = OsmNodeNamed().apply {
    name = label
    // Convention BRouter : coordonnées en micro-degrés décalées (lon+180, lat+90).
    ilon = ((longitude + 180.0) * 1_000_000.0 + 0.5).toInt()
    ilat = ((latitude + 90.0) * 1_000_000.0 + 0.5).toInt()
}
