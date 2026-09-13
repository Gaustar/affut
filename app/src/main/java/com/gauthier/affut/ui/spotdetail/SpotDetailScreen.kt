package com.gauthier.affut.ui.spotdetail

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import com.gauthier.affut.data.routing.NavigationTarget
import com.gauthier.affut.data.routing.NavigationTargets
import com.gauthier.affut.ui.common.DeerActivityBadge
import com.gauthier.affut.ui.spotedit.formatCoordinates
import com.gauthier.affut.ui.spotedit.formatObservedDate
import com.gauthier.affut.util.DeerActivityCalculator
import com.gauthier.affut.util.weatherEmoji
import com.gauthier.affut.util.windDirectionLabel
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailScreen(
    spotId: String,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
    onOpenForecast: (Double, Double, String) -> Unit = { _, _, _ -> },
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SpotDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SpotDetailViewModel(application, spotId) }
        },
    )
    val spot by viewModel.spot.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val weatherError by viewModel.weatherError.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    val current = spot

    Scaffold(
        topBar = { TopAppBar(title = { Text(current?.title ?: "Spot") }) },
    ) { padding ->
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Spot introuvable ou supprimé.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(current.type.label, style = MaterialTheme.typography.titleMedium)
            current.observedAt?.let {
                Text("Observé le ${formatObservedDate(it)}")
            }
            Text("Position : " + formatCoordinates(current.latitude, current.longitude))
            if (current.notes.isNotBlank()) {
                Text(current.notes)
            }
            val shareCount = current.sharedWithFriendIds.size + current.sharedWithGroupIds.size
            Text(if (shareCount > 0) "Partagé (avec $shareCount ami·e·s/groupe·s)" else "Privé")

            weatherError?.let { message ->
                if (weather == null) {
                    Text(message, style = MaterialTheme.typography.bodySmall)
                }
            }

            weather?.let { w ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${weatherEmoji(w.weatherCode)} ${w.temperature.roundToInt()}°C")
                        Text("${w.windSpeedKmh.roundToInt()} km/h ${windDirectionLabel(w.windDirectionDeg)}")
                        TextButton(onClick = viewModel::refreshWeather) { Text("Actualiser") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        w.daily.take(3).forEach { day ->
                            Text(
                                "${day.date.takeLast(2)}/${day.date.substring(5, 7)} : " +
                                    "${day.tempMin.roundToInt()}°/${day.tempMax.roundToInt()}°",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                DeerActivityBadge(result = DeerActivityCalculator.compute(w))
            }

            TextButton(onClick = { onOpenForecast(current.latitude, current.longitude, current.title) }) {
                Text("Météo détaillée de ce spot")
            }

            Button(
                onClick = {
                    NavigationTargets.start(
                        NavigationTarget(current.latitude, current.longitude, current.title),
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("S'y rendre")
            }

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onEdit(current.id) }, modifier = Modifier.weight(1f)) {
                    Text("Modifier")
                }
                OutlinedButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.weight(1f)) {
                    Text("Supprimer")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer ce spot ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
            },
        )
    }
}
