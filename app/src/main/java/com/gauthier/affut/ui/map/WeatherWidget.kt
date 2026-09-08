package com.gauthier.affut.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gauthier.affut.domain.model.WeatherSnapshot
import com.gauthier.affut.util.weatherEmoji
import kotlin.math.roundToInt

@Composable
fun WeatherWidget(weather: WeatherSnapshot, modifier: Modifier = Modifier) {
    // Recalculé toutes les 30 s : sinon une donnée d'il y a une heure continue d'afficher "il y a 2 min".
    val ageMinutes by produceState(0L, weather.fetchedAt) {
        while (true) {
            value = ((System.currentTimeMillis() - weather.fetchedAt) / 60_000L).coerceAtLeast(0)
            delay(30_000L)
        }
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(weatherEmoji(weather.weatherCode), style = MaterialTheme.typography.headlineSmall)
            Column {
                Text("${weather.temperature.roundToInt()}°C · ${weather.windSpeedKmh.roundToInt()} km/h")
                Text(
                    "Pluie ${weather.precipitationProbabilityNextHour}% · il y a $ageMinutes min" +
                        if (weather.isStale) " · hors ligne" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (weather.isStale) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.typography.bodySmall.color
                    },
                )
            }
        }
    }
}
