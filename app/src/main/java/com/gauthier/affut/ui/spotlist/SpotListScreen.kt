package com.gauthier.affut.ui.spotlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gauthier.affut.domain.model.SpotType
import com.gauthier.affut.ui.map.markerColorFor
import com.gauthier.affut.ui.spotedit.formatObservedDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotListScreen(
    onBack: () -> Unit,
    onSpotClick: (String) -> Unit,
    viewModel: SpotListViewModel = viewModel(),
) {
    val entries by viewModel.spots.collectAsState()
    val query by viewModel.query.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val counts by viewModel.countsByType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes spots") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Rechercher (titre, notes, type)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = typeFilter == null,
                    onClick = { viewModel.onTypeFilterChange(null) },
                    label = { Text("Tous") },
                )
                SpotType.entries.forEach { type ->
                    val count = counts[type] ?: 0
                    if (count > 0) {
                        FilterChip(
                            selected = typeFilter == type,
                            onClick = { viewModel.onTypeFilterChange(if (typeFilter == type) null else type) },
                            label = { Text("${type.label} ($count)") },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Trier :", style = MaterialTheme.typography.labelLarge)
                SpotSort.entries.forEach { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { viewModel.onSortChange(option) },
                        label = { Text(option.label) },
                    )
                }
            }

            HorizontalDivider()

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (query.isBlank() && typeFilter == null) {
                            "Aucun spot enregistré.\nAppui long sur la carte pour en créer un."
                        } else {
                            "Aucun spot ne correspond à cette recherche."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries, key = { it.spot.id }) { entry ->
                    SpotRow(entry = entry, onClick = { onSpotClick(entry.spot.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SpotRow(entry: SpotListEntry, onClick: () -> Unit) {
    val spot = entry.spot
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(markerColorFor(spot.type), CircleShape),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                spot.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append(spot.type.label)
                    spot.observedAt?.let { append(" · observé le ${formatObservedDate(it)}") }
                    entry.distanceMeters?.let { append(" · ${formatDistance(it)}") }
                    if (spot.sharedWithUids.isNotEmpty()) append(" · partagé")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (spot.notes.isNotBlank()) {
                Text(
                    spot.notes.lineSequence().first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.roundToInt()} m" else "%.1f km".format(meters / 1000)
