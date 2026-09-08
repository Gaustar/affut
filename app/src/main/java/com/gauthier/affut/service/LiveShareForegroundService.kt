package com.gauthier.affut.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.gauthier.affut.MainActivity
import com.gauthier.affut.data.repository.AppPreferences
import com.gauthier.affut.data.repository.LiveShareRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val NOTIFICATION_ID = 42
private const val CHANNEL_ID = "live_share"
private const val LOCATION_MIN_DISTANCE_M = 50f
private const val NOTIFICATION_REFRESH_MS = 30_000L

class LiveShareForegroundService : Service() {
    private lateinit var repository: LiveShareRepository
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcherJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = LiveShareRepository.create(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val expiresAt = repository.expiresAt
        startForeground(NOTIFICATION_ID, buildNotification(expiresAt - System.currentTimeMillis()))
        // Android peut relancer un service persistant : sans ces retraits, chaque relance
        // empilerait un écouteur de position et un minuteur supplémentaires.
        stopLocationUpdates()
        watcherJob?.cancel()
        startLocationUpdates()
        startExpiryWatcher(expiresAt)
        return START_STICKY
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    override fun onDestroy() {
        stopLocationUpdates()
        watcherJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // Intervalle réglable depuis l'écran Paramètres (compromis suivi / batterie).
        val intervalMs = AppPreferences(this).locationIntervalMinutes * 60_000L
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(LOCATION_MIN_DISTANCE_M)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                serviceScope.launch {
                    repository.onLocationUpdate(location.latitude, location.longitude, location.accuracy)
                }
            }
        }
        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun startExpiryWatcher(expiresAt: Long) {
        watcherJob = serviceScope.launch {
            while (isActive) {
                val remaining = expiresAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    repository.stop()
                    stopSelf()
                    return@launch
                }
                updateNotification(remaining)
                delay(NOTIFICATION_REFRESH_MS)
            }
        }
    }

    private fun updateNotification(remainingMillis: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(remainingMillis))
    }

    private fun buildNotification(remainingMillis: Long): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Partage de position actif")
            .setContentText("Temps restant : ${formatRemaining(remainingMillis)}")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Partage de position",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, LiveShareForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveShareForegroundService::class.java))
        }
    }
}

private fun formatRemaining(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}
