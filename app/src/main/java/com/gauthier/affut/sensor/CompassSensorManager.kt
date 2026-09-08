package com.gauthier.affut.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

data class CompassReading(
    /** Cap vers le nord GÉOGRAPHIQUE (déclinaison appliquée), donc aligné avec la carte. */
    val azimuthDegrees: Float,
    /** SensorManager.SENSOR_STATUS_* — utilisé pour détecter le besoin de calibration. */
    val accuracy: Int,
    /** Écart nord magnétique / nord géographique appliqué, en degrés. */
    val declinationDegrees: Float = 0f,
)

// Lissage adaptatif : réactif quand on pivote franchement, très stable quand on est immobile
// (sinon soit l'aiguille "traîne" pendant la rotation, soit elle vibre à l'arrêt).
private const val SLOW_ALPHA = 0.08f
private const val FAST_ALPHA = 0.5f
private const val FAST_ROTATION_THRESHOLD_DEG = 12f

// Au-delà de cette inclinaison, le téléphone est tenu à la verticale (on vise devant soi) :
// le calcul "à plat" devient instable, il faut changer de repère.
private const val UPRIGHT_PITCH_THRESHOLD_DEG = 65f

/** Cap du téléphone via TYPE_ROTATION_VECTOR (fusion gyroscope + magnétomètre + accéléromètre). */
class CompassSensorManager(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    @Volatile
    private var declination = 0f

    fun hasCompassSensor(): Boolean = rotationSensor != null

    /**
     * Renseigne la position pour corriger l'écart entre nord magnétique et nord géographique.
     * Sans ça, la boussole est décalée par rapport à la carte (l'écart varie selon le lieu).
     */
    fun updateLocation(latitude: Double, longitude: Double, altitudeMeters: Double = 0.0) {
        declination = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            altitudeMeters.toFloat(),
            System.currentTimeMillis(),
        ).declination
    }

    fun observeCompass(): Flow<CompassReading> = callbackFlow {
        var smoothedAzimuth = 0f
        var initialized = false
        var currentAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // 1) Inclinaison : téléphone à plat (carte) ou tenu à la verticale (visée) ?
                SensorManager.getOrientation(rotationMatrix, orientation)
                val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val isUpright = abs(pitchDeg) > UPRIGHT_PITCH_THRESHOLD_DEG

                // 2) Compensation de la rotation de l'écran (paysage, écran retourné…).
                val displayRotation = ContextCompat.getDisplayOrDefault(context).rotation
                val (axisX, axisY) = if (isUpright) {
                    when (displayRotation) {
                        Surface.ROTATION_90 -> SensorManager.AXIS_Z to SensorManager.AXIS_MINUS_X
                        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Z
                        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Z to SensorManager.AXIS_X
                        else -> SensorManager.AXIS_X to SensorManager.AXIS_Z
                    }
                } else {
                    when (displayRotation) {
                        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                    }
                }
                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
                SensorManager.getOrientation(remappedMatrix, orientation)

                // 3) Nord magnétique -> nord géographique, pour coller à la carte.
                var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination
                azimuthDeg = ((azimuthDeg % 360f) + 360f) % 360f

                smoothedAzimuth = if (!initialized) {
                    initialized = true
                    azimuthDeg
                } else {
                    val alpha = if (abs(angleDelta(smoothedAzimuth, azimuthDeg)) > FAST_ROTATION_THRESHOLD_DEG) {
                        FAST_ALPHA
                    } else {
                        SLOW_ALPHA
                    }
                    lowPassAngle(smoothedAzimuth, azimuthDeg, alpha)
                }
                trySend(CompassReading(smoothedAzimuth, currentAccuracy, declination))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                currentAccuracy = accuracy
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}

/** Écart signé le plus court entre deux caps, dans [-180°, 180°]. */
private fun angleDelta(from: Float, to: Float): Float {
    var delta = to - from
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return delta
}

/** Interpole en gérant proprement le passage 359° -> 0°. */
private fun lowPassAngle(current: Float, target: Float, alpha: Float): Float {
    var result = current + alpha * angleDelta(current, target)
    if (result < 0f) result += 360f
    if (result >= 360f) result -= 360f
    return result
}
