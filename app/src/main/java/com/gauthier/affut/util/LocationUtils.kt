package com.gauthier.affut.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlin.math.pow

/** Dernière position connue via les providers système, sans déclencher de nouvelle recherche GPS. */
@SuppressLint("MissingPermission")
fun lastKnownLocation(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return null
    }
    val locationManager = context.getSystemService(LocationManager::class.java) ?: return null
    return locationManager.getProviders(true)
        .mapNotNull { locationManager.getLastKnownLocation(it) }
        .maxByOrNull { it.time }
}

/** Distance à vol d'oiseau entre deux points, en mètres. */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).pow(2) +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).pow(2)
    return 2 * earthRadius * kotlin.math.asin(kotlin.math.sqrt(a))
}

/** Cap en degrés (0 = nord) de [lat1],[lon1] vers [lat2],[lon2]. */
fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = kotlin.math.sin(dLon) * kotlin.math.cos(Math.toRadians(lat2))
    val x = kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.sin(Math.toRadians(lat2)) -
        kotlin.math.sin(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.cos(dLon)
    return (Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0
}
