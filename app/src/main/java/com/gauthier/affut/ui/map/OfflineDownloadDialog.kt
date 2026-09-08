package com.gauthier.affut.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gauthier.affut.data.repository.OfflineMapRepository
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.views.MapView

private const val MAX_EXTRA_ZOOM_LEVELS = 6
private const val ABSOLUTE_MAX_ZOOM = 19

/**
 * Au-delà de ce nombre de tuiles, on bloque le téléchargement : sans cette limite, une zone
 * affichée trop dézoomée (ex: toute l'Europe) pourrait déclencher un téléchargement de plusieurs
 * heures et saturer le stockage/la connexion de l'utilisateur.
 */
private const val MAX_TILES = 4000
private const val APPROX_KB_PER_TILE = 15

@Composable
fun OfflineDownloadDialog(
    mapView: MapView,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { OfflineMapRepository.create(context) }

    var extraZoomLevels by remember { mutableFloatStateOf(3f) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val zoomMin = mapView.zoomLevelDouble.toInt()
    val zoomMax = (zoomMin + extraZoomLevels.toInt()).coerceAtMost(ABSOLUTE_MAX_ZOOM)
    val estimatedTiles = remember(zoomMin, zoomMax) {
        runCatching { CacheManager(mapView).possibleTilesInArea(mapView.boundingBox, zoomMin, zoomMax) }
            .getOrDefault(0)
    }
    val zoneTooLarge = estimatedTiles > MAX_TILES

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text("Télécharger cette zone hors-ligne") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    resultMessage != null -> Text(resultMessage!!)
                    isDownloading -> {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("Téléchargement en cours…")
                    }
                    else -> {
                        Text("Zone actuellement affichée à l'écran.")
                        Text("Niveau de détail supplémentaire : ${extraZoomLevels.toInt()}")
                        Slider(
                            value = extraZoomLevels,
                            onValueChange = { extraZoomLevels = it },
                            valueRange = 1f..MAX_EXTRA_ZOOM_LEVELS.toFloat(),
                            steps = MAX_EXTRA_ZOOM_LEVELS - 2,
                        )
                        Text(
                            "Plus de niveaux = zoom possible plus précis une fois hors-ligne, mais plus de données à télécharger.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "≈ $estimatedTiles tuiles (≈ ${(estimatedTiles * APPROX_KB_PER_TILE / 1024).coerceAtLeast(1)} Mo)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (zoneTooLarge) {
                            Text(
                                "Zone trop grande pour un téléchargement raisonnable. " +
                                    "Zoome davantage sur la carte (ferme cette fenêtre, rapproche-toi de ta zone de chasse) avant de réessayer.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                resultMessage != null -> TextButton(onClick = onDismiss) { Text("Fermer") }
                isDownloading -> Unit
                else -> TextButton(
                    enabled = !zoneTooLarge,
                    onClick = {
                        isDownloading = true
                        scope.launch {
                            val success = repository.downloadRegion(
                                mapView,
                                mapView.boundingBox,
                                zoomMin,
                                zoomMax,
                            ) { current, total ->
                                progress = if (total > 0) current.toFloat() / total else 0f
                            }
                            isDownloading = false
                            resultMessage = if (success) {
                                "Zone téléchargée avec succès."
                            } else {
                                "Le téléchargement a échoué. Réessaie avec une connexion stable."
                            }
                        }
                    },
                ) { Text("Télécharger") }
            }
        },
        dismissButton = {
            if (!isDownloading && resultMessage == null) {
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
    )
}
