package com.gauthier.affut.ui.liveshare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gauthier.affut.data.routing.NavigationTarget
import com.gauthier.affut.data.routing.NavigationTargets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveShareScreen(
    onBack: () -> Unit,
    viewModel: LiveShareViewModel = viewModel(),
) {
    val context = LocalContext.current
    val isActive by viewModel.isActive.collectAsState()
    val expiresAt by viewModel.expiresAt.collectAsState()
    val lastSentAt by viewModel.lastSentAt.collectAsState()
    val friendPosition by viewModel.friendPosition.collectAsState()
    val hasUnsentPosition by viewModel.hasUnsentPosition.collectAsState()

    var selectedDuration by remember { mutableStateOf(SHARE_DURATIONS[1]) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.start(selectedDuration)
        } else {
            // Android refuse souvent d'accorder "Toujours autoriser" via une simple boîte de dialogue
            // (surtout Android 11+) : il faut alors passer par les paramètres système de l'app.
            permissionDeniedMessage =
                "Le partage en arrière-plan nécessite la permission de localisation \"Toujours autoriser\". " +
                    "Android ne permet pas de l'accorder directement ici : ouvre les paramètres de " +
                    "l'application, puis Autorisations > Position > \"Toujours autoriser\"."
        }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    fun requestStart() {
        permissionDeniedMessage = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val needsBackgroundPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
            PackageManager.PERMISSION_GRANTED

        if (needsBackgroundPermission) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            viewModel.start(selectedDuration)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partage de position") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isActive) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Partage actif", style = MaterialTheme.typography.titleMedium)
                        Text(remainingLabel(expiresAt))
                        Text(lastSentLabel(lastSentAt), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (hasUnsentPosition) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Dernière position non envoyée : ton ami ne te voit pas bouger. " +
                                "L'envoi reprendra automatiquement au retour du réseau.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Button(onClick = viewModel::stop, modifier = Modifier.fillMaxWidth()) {
                    Text("Arrêter le partage")
                }
            } else {
                Text("Durée du partage", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SHARE_DURATIONS.take(4).forEach { duration ->
                        FilterChip(
                            selected = duration == selectedDuration,
                            onClick = { selectedDuration = duration },
                            label = { Text(duration.label) },
                            modifier = Modifier.wrapContentWidth(),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SHARE_DURATIONS.drop(4).forEach { duration ->
                        FilterChip(
                            selected = duration == selectedDuration,
                            onClick = { selectedDuration = duration },
                            label = { Text(duration.label) },
                            modifier = Modifier.wrapContentWidth(),
                        )
                    }
                }

                permissionDeniedMessage?.let {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                            TextButton(onClick = { openAppSettings() }) {
                                Text("Ouvrir les paramètres de l'application")
                            }
                        }
                    }
                }

                Text(
                    "Ta position sera visible par l'autre utilisateur pendant toute la durée choisie. " +
                        "Aucun historique n'est conservé, seule ta dernière position est partagée.",
                    style = MaterialTheme.typography.bodySmall,
                )

                Button(onClick = { requestStart() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Démarrer le partage")
                }
            }

            friendPosition?.let { friend ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Position de ton ami partagée", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Dernière mise à jour : il y a ${((System.currentTimeMillis() - friend.updatedAt) / 60_000L).coerceAtLeast(0)} min",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(
                            onClick = {
                                NavigationTargets.start(
                                    NavigationTarget(friend.latitude, friend.longitude, "ton ami"),
                                )
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Le rejoindre")
                        }
                    }
                }
            }

            OutlinedButton(onClick = viewModel::refreshState, modifier = Modifier.fillMaxWidth()) {
                Text("Actualiser l'état")
            }
        }
    }
}

private fun remainingLabel(expiresAt: Long): String {
    val remaining = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    val totalMinutes = remaining / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "Temps restant : " + if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}

private fun lastSentLabel(lastSentAt: Long): String {
    if (lastSentAt == 0L) return "Dernière position envoyée : jamais encore"
    val ageMinutes = ((System.currentTimeMillis() - lastSentAt) / 60_000L).coerceAtLeast(0)
    return "Dernière position envoyée : il y a $ageMinutes min"
}
