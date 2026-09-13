package com.gauthier.affut.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gauthier.affut.R
import com.gauthier.affut.data.repository.AppPreferences
import com.gauthier.affut.data.repository.AuthRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val REGION_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenFriendsGroups: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val regions by viewModel.regions.collectAsState()
    val weatherMinutes by viewModel.weatherMinutes.collectAsState()
    val locationMinutes by viewModel.locationMinutes.collectAsState()
    val routing by viewModel.routingStatus.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val scope = rememberCoroutineScope()
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Connexion", style = MaterialTheme.typography.titleMedium)
                if (isSignedIn) {
                    Text("✅ Connecté — le partage avec l'autre utilisateur est actif.")
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Non connecté : la carte, les spots déjà enregistrés, la boussole, " +
                                    "la météo et la navigation restent utilisables, mais rien n'est " +
                                    "partagé avec l'autre utilisateur tant que la connexion échoue.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(
                                enabled = !isConnecting,
                                onClick = {
                                    connectionError = null
                                    isConnecting = true
                                    scope.launch {
                                        val result = AuthRepository().ensureSignedIn(context, webClientId)
                                        isConnecting = false
                                        if (result.isFailure) {
                                            connectionError = "Connexion impossible. Réessaie avec une connexion stable."
                                        }
                                        viewModel.refreshAuthState()
                                    }
                                },
                            ) {
                                Text(if (isConnecting) "Connexion…" else "Se connecter")
                            }
                            connectionError?.let {
                                Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                OutlinedButton(onClick = onOpenFriendsGroups, modifier = Modifier.fillMaxWidth()) {
                    Text("Amis et groupes")
                }
            }

            item {
                HorizontalDivider()
                Text("Rafraîchissement météo", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPreferences.WEATHER_CHOICES.forEach { minutes ->
                        FilterChip(
                            selected = minutes == weatherMinutes,
                            onClick = { viewModel.setWeatherMinutes(minutes) },
                            label = { Text("$minutes min") },
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
                Text("Envoi de position (partage)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPreferences.LOCATION_CHOICES.forEach { minutes ->
                        FilterChip(
                            selected = minutes == locationMinutes,
                            onClick = { viewModel.setLocationMinutes(minutes) },
                            label = { Text("$minutes min") },
                        )
                    }
                }
                Text(
                    "Un intervalle plus court suit mieux les déplacements, mais consomme plus de batterie.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            item {
                HorizontalDivider()
                Text("Navigation hors-ligne", style = MaterialTheme.typography.titleMedium)
                if (routing.installed) {
                    Text("✅ ${routing.fileNames.size} zone(s) installée(s) — ${routing.totalMegabytes} Mo")
                    routing.fileNames.forEach { name ->
                        Text("· $name", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Aucune carte de routage installée : le guidage se fera à vol d'oiseau " +
                                    "(cap + distance) au lieu de suivre les chemins.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Text(
                    "Fichiers .rd5 à déposer dans :\n${routing.directoryPath}",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = viewModel::refreshRoutingStatus) { Text("Revérifier") }
            }

            item {
                HorizontalDivider()
                Text("Zones de carte téléchargées", style = MaterialTheme.typography.titleMedium)
                if (regions.isEmpty()) {
                    Text("Aucune zone téléchargée.", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(regions, key = { it.id }) { region ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.7f)) {
                        Text(
                            "%.3f, %.3f → %.3f, %.3f".format(
                                region.minLat, region.minLon, region.maxLat, region.maxLon,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Zoom ${region.zoomMin}-${region.zoomMax} · ${formatRegionDate(region.downloadedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { viewModel.deleteRegion(region.id) }) { Text("Supprimer") }
                }
            }

            item {
                HorizontalDivider()
                OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Se déconnecter")
                }
                Text(
                    "La reconnexion est automatique au prochain lancement, avec le même compte Google.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun formatRegionDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(REGION_DATE_FORMATTER)
