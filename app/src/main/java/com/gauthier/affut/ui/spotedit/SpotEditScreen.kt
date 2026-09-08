package com.gauthier.affut.ui.spotedit

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.gauthier.affut.domain.model.SpotType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotEditScreen(
    spotId: String?,
    initialLatitude: Double,
    initialLongitude: Double,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: SpotEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SpotEditViewModel(application, spotId, initialLatitude, initialLongitude) }
        },
    )
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.observedAt ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onObservedAtChange(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Modifier le spot" else "Nouveau spot") },
                navigationIcon = { IconButton(onClick = onDone) { Text("←") } },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.isMissing) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Ce spot n'existe plus : il a été supprimé entre-temps.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SpotType.entries.forEach { type ->
                        FilterChip(
                            selected = type == state.type,
                            onClick = { viewModel.onTypeChange(type) },
                            label = { Text(type.label) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Date d'observation", style = MaterialTheme.typography.labelLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(state.observedAt?.let(::formatObservedDate) ?: "Choisir une date")
                    }
                    if (state.observedAt != null) {
                        TextButton(onClick = { viewModel.onObservedAtChange(null) }) { Text("Effacer") }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.isShared, onCheckedChange = viewModel::onSharedChange)
                Text("Partagé avec l'autre utilisateur")
            }

            Text(
                // Locale.US : en français, "%.5f" produirait "49,79498, 5,00084" — virgule
                // décimale et virgule séparatrice mélangées, illisible pour des coordonnées.
                "Position : %s".format(formatCoordinates(state.latitude, state.longitude)),
                style = MaterialTheme.typography.bodySmall,
            )

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Enregistrer")
            }
        }
    }
}

private val OBSERVED_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Coordonnées au point décimal, telles qu'on les recopie dans n'importe quel outil de cartographie. */
internal fun formatCoordinates(latitude: Double, longitude: Double): String =
    String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude)

internal fun formatObservedDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(OBSERVED_DATE_FORMATTER)
