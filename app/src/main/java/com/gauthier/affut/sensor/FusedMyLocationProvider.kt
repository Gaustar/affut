package com.gauthier.affut.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

private const val UPDATE_INTERVAL_MS = 3_000L
private const val FASTEST_INTERVAL_MS = 1_000L

/**
 * Fournit la position à la carte via le provider "fused" de Google Play Services.
 *
 * Le provider par défaut d'osmdroid n'interroge que le GPS satellite et le réseau : à l'intérieur
 * ou sous couvert forestier, les deux peuvent n'avoir aucun point alors que le "fused" (qui combine
 * GPS, wifi, réseau et capteurs) en a un. C'est justement là qu'on a le plus besoin de se situer.
 */
class FusedMyLocationProvider(context: Context) : IMyLocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var consumer: IMyLocationConsumer? = null
    private var lastLocation: Location? = null
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
        consumer = myLocationConsumer

        // Dernière position connue tout de suite : évite d'attendre le premier point.
        runCatching {
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null && lastLocation == null) {
                    lastLocation = location
                    consumer?.onLocationChanged(location, this)
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                lastLocation = location
                consumer?.onLocationChanged(location, this@FusedMyLocationProvider)
            }
        }
        callback = locationCallback

        return runCatching {
            client.requestLocationUpdates(request, locationCallback, null)
        }.isSuccess
    }

    override fun stopLocationProvider() {
        callback?.let { runCatching { client.removeLocationUpdates(it) } }
        callback = null
        consumer = null
    }

    override fun getLastKnownLocation(): Location? = lastLocation

    override fun destroy() {
        stopLocationProvider()
        lastLocation = null
    }
}
