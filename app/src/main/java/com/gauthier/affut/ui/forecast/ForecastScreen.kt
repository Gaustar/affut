package com.gauthier.affut.ui.forecast

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gauthier.affut.domain.model.HourlyForecast
import com.gauthier.affut.ui.common.DeerActivityBadge
import com.gauthier.affut.util.DayPart
import com.gauthier.affut.util.DeerActivityCalculator
import com.gauthier.affut.util.SegmentForecast
import com.gauthier.affut.util.buildSegments
import com.gauthier.affut.util.moonPhaseFor
import com.gauthier.affut.util.weatherAlerts
import com.gauthier.affut.util.weatherEmoji
import com.gauthier.affut.util.windDirectionLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val DETAILED_HOURS = 24

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    onBack: () -> Unit,
    latitude: Double? = null,
    longitude: Double? = null,
    placeLabel: String? = null,
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ForecastViewModel = viewModel(
        key = "forecast_${latitude}_$longitude",
        factory = viewModelFactory {
            initializer { ForecastViewModel(application, latitude, longitude) }
        },
    )
    val weather by viewModel.weather.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(placeLabel ?: "Prévisions") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
                actions = { IconButton(onClick = viewModel::refresh) { Text("🔄") } },
            )
        },
    ) { padding ->
        val current = weather
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (isLoading) CircularProgressIndicator() else Text("Météo indisponible.")
            }
            return@Scaffold
        }

        val alerts = weatherAlerts(current)
        val next24h = remember(current) { current.hourly.take(DETAILED_HOURS) }
        // Les tranches ne commencent qu'après le détail horaire, pour ne pas répéter les mêmes heures.
        val segments = remember(current) {
            buildSegments(current.hourly.drop(DETAILED_HOURS))
        }
        val dailyByDate = remember(current) { current.daily.associateBy { it.date } }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${weatherEmoji(current.weatherCode)} ${current.temperature.roundToInt()}°C",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        "Vent : ${current.windSpeedKmh.roundToInt()} km/h ${windDirectionLabel(current.windDirectionDeg)} · " +
                            "Pluie ${current.precipitationProbabilityNextHour}%",
                    )
                }
            }

            item { DeerActivityBadge(result = DeerActivityCalculator.compute(current)) }

            if (alerts.isNotEmpty()) {
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            alerts.forEach { alert ->
                                Text("⚠️ $alert", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            if (next24h.isNotEmpty()) {
                item { SectionTitle("Prochaines 24 h, heure par heure") }
                items(next24h) { hour -> HourRow(hour) }
            }

            if (segments.isNotEmpty()) {
                item { SectionTitle("Jours suivants") }
                var lastDate: LocalDate? = null
                segments.forEach { segment ->
                    if (segment.date != lastDate) {
                        lastDate = segment.date
                        item(key = "day_${segment.date}") {
                            DayHeader(
                                date = segment.date,
                                sunrise = dailyByDate[segment.date.toString()]?.sunrise,
                                sunset = dailyByDate[segment.date.toString()]?.sunset,
                            )
                        }
                    }
                    item(key = "seg_${segment.date}_${segment.part}") { SegmentRow(segment) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DayHeader(date: LocalDate, sunrise: String?, sunset: String?) {
    val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
        .replaceFirstChar { it.uppercase() }
    val moon = moonPhaseFor(date)
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "$dayLabel ${date.dayOfMonth}/${date.monthValue} ${moon.emoji}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        if (sunrise != null && sunset != null) {
            Text(
                "Lever ${sunrise.takeLast(5)} · Coucher ${sunset.takeLast(5)} · ${moon.label}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HourRow(hour: HourlyForecast) {
    val label = runCatching { LocalDateTime.parse(hour.time) }.getOrNull()
        ?.let { "%02dh".format(it.hour) } ?: hour.time.takeLast(5)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.width(44.dp), style = MaterialTheme.typography.bodyMedium)
        Text(if (hour.isDay) weatherEmoji(hour.weatherCode) else "🌙")
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "${hour.temperature.roundToInt()}°C (ressenti ${hour.apparentTemperature.roundToInt()}°) · " +
                    "${hour.cloudCoverPercent}% nuages",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Vent ${hour.windSpeedKmh.roundToInt()} km/h " +
                    "(raf. ${hour.windGustKmh.roundToInt()}) ${windDirectionLabel(hour.windDirectionDeg)} · " +
                    "Pluie ${hour.precipitationProbability}% (${formatMm(hour.precipitationMm)}) · " +
                    "Hum. ${hour.humidityPercent}% · Vis. ${formatVisibility(hour.visibilityMeters)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SegmentRow(segment: SegmentForecast) {
    val partEmoji = when (segment.part) {
        DayPart.MATIN -> "🌅"
        DayPart.APRES_MIDI -> "☀️"
        DayPart.NUIT -> "🌙"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "$partEmoji ${segment.part.label} — ${segment.tempMin.roundToInt()}° / ${segment.tempMax.roundToInt()}° " +
                weatherEmoji(segment.weatherCode),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Vent ${segment.windSpeedAvgKmh.roundToInt()} km/h " +
                "(raf. ${segment.windGustMaxKmh.roundToInt()}) ${windDirectionLabel(segment.windDirectionDeg)} · " +
                "Pluie ${segment.precipitationProbabilityMax}% (${formatMm(segment.precipitationMm)})",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Ressenti min ${segment.apparentTempMin.roundToInt()}° · " +
                "${segment.cloudCoverAvgPercent}% nuages · Hum. ${segment.humidityAvgPercent}%",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatMm(mm: Double): String = if (mm < 0.05) "0 mm" else "%.1f mm".format(mm)

/** Visibilité : en dessous du kilomètre, c'est le brouillard — l'info compte de nuit. */
private fun formatVisibility(meters: Double): String = when {
    meters <= 0.0 -> "n.d."
    meters < 1000 -> "${meters.roundToInt()} m"
    else -> "${(meters / 1000).roundToInt()} km"
}
