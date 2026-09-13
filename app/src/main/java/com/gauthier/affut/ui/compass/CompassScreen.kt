package com.gauthier.affut.ui.compass

import android.graphics.Paint
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gauthier.affut.util.weatherEmoji
import com.gauthier.affut.util.windDirectionLabel
import com.gauthier.affut.util.windRelationLabel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val CARDINALS = listOf("N" to 0f, "NE" to 45f, "E" to 90f, "SE" to 135f, "S" to 180f, "SO" to 225f, "O" to 270f, "NO" to 315f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    onBack: () -> Unit,
    viewModel: CompassViewModel = viewModel(),
) {
    val compass by viewModel.compass.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val weatherError by viewModel.weatherError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boussole") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!viewModel.hasSensor) {
                Text("Ce téléphone n'a pas de capteur de boussole.")
                return@Column
            }

            val reading = compass
            if (reading == null) {
                Text("Initialisation de la boussole…")
                return@Column
            }

            val azimuth = reading.azimuthDegrees
            val needsCalibration = reading.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW

            Text(
                "${azimuth.roundToInt()}° ${windDirectionLabel(azimuth.toDouble())}",
                style = MaterialTheme.typography.headlineMedium,
            )

            if (needsCalibration) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Faites un 8 avec votre téléphone pour calibrer la boussole",
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                CompassRose(azimuthDegrees = azimuth, windFromDegrees = weather?.windDirectionDeg)
            }

            weatherError?.let { message ->
                if (weather == null) {
                    Text(message, style = MaterialTheme.typography.bodySmall)
                }
            }

            weather?.let { w ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${weatherEmoji(w.weatherCode)} Vent : ${w.windSpeedKmh.roundToInt()} km/h " +
                            windDirectionLabel(w.windDirectionDeg),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        windRelationLabel(azimuth, w.windDirectionDeg),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassRose(azimuthDegrees: Float, windFromDegrees: Double?) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val windColor = MaterialTheme.colorScheme.error

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.85f

        // Cadran tournant : les points cardinaux restent alignés avec les vraies directions
        // géographiques au fur et à mesure qu'on tourne le téléphone.
        rotate(degrees = -azimuthDegrees, pivot = center) {
            drawCircle(color = onSurfaceColor, radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))

            val textPaint = Paint().apply {
                color = onSurfaceColor.toArgb()
                textSize = 14.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            CARDINALS.forEach { (label, angleDeg) ->
                val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
                val x = center.x + (radius * 0.88f) * cos(rad).toFloat()
                val y = center.y + (radius * 0.88f) * sin(rad).toFloat() + textPaint.textSize / 3
                drawContext.canvas.nativeCanvas.drawText(label, x, y, textPaint)
            }

            // Flèche de vent, positionnée dans le même repère géographique que le cadran.
            if (windFromDegrees != null) {
                rotate(degrees = windFromDegrees.toFloat(), pivot = center) {
                    drawLine(
                        color = windColor,
                        start = center,
                        end = Offset(center.x, center.y - radius * 0.7f),
                        strokeWidth = 6.dp.toPx(),
                    )
                    drawCircle(color = windColor, radius = 8.dp.toPx(), center = Offset(center.x, center.y - radius * 0.7f))
                }
            }
        }

        // Pointeur fixe : direction vers laquelle le téléphone est tourné (toujours vers le haut de l'écran).
        drawLine(
            color = primaryColor,
            start = center,
            end = Offset(center.x, center.y - radius * 0.95f),
            strokeWidth = 8.dp.toPx(),
        )
    }
}
