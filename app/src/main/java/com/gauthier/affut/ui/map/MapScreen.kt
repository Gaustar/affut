package com.gauthier.affut.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gauthier.affut.R
import com.gauthier.affut.data.repository.AppPreferences
import com.gauthier.affut.sync.SyncStatus
import com.gauthier.affut.util.bearingDegrees
import com.gauthier.affut.util.haversineMeters
import com.gauthier.affut.util.windDirectionLabel
import kotlin.math.roundToInt
import com.gauthier.affut.sensor.FusedMyLocationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Centre approximatif de la France — juste un point de départ tant qu'on n'a pas la position réelle.
private val DEFAULT_CENTER = GeoPoint(46.6, 2.4)
private const val DEFAULT_ZOOM = 6.0
/** Niveau de zoom utilisé pour centrer automatiquement sur la position réelle (échelle d'un massif forestier). */
private const val FOCUSED_ZOOM = 16.0
/** Cadence de suivi du guidage, et distance parcourue au-delà de laquelle on recalcule. */
private const val ROUTE_REFRESH_INTERVAL_MS = 5_000L
private const val ROUTE_REFRESH_DISTANCE_M = 25.0

@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val state = produceState(initialValue = true) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        value = connectivityManager?.activeNetwork != null
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = true
            }

            override fun onLost(network: Network) {
                value = connectivityManager?.activeNetwork != null
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(callback)
        awaitDispose { connectivityManager?.unregisterNetworkCallback(callback) }
    }
    return state.value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onSpotClick: (String) -> Unit,
    onCreateSpot: (Double, Double) -> Unit,
    onOpenForecast: () -> Unit,
    onOpenCompass: () -> Unit,
    onOpenLiveShare: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenSpotList: () -> Unit = {},
    viewModel: MapViewModel = viewModel(),
) {
    val context = LocalContext.current
    val spots by viewModel.spots.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val otherLiveShares by viewModel.otherLiveShares.collectAsState()
    val navigationTarget by viewModel.navigationTarget.collectAsState()
    val route by viewModel.route.collectAsState()
    val syncState by SyncStatus.state.collectAsState()
    val isOnline = rememberIsOnline()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasLocationPermission = results.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var showLegendDialog by remember { mutableStateOf(false) }
    var weatherLocation by remember { mutableStateOf<GeoPoint?>(null) }
    // Ne centre/zoome automatiquement qu'une fois, pour ne pas gêner une navigation manuelle ultérieure sur la carte.
    var hasAutoZoomed by remember { mutableStateOf(false) }

    // Actualisation météo automatique, à l'intervalle choisi dans les Paramètres.
    LaunchedEffect(weatherLocation) {
        val loc = weatherLocation ?: return@LaunchedEffect
        val intervalMs = AppPreferences(context).weatherRefreshMinutes * 60_000L
        while (isActive) {
            viewModel.refreshWeather(loc.latitude, loc.longitude)
            delay(intervalMs)
        }
    }

    // AndroidView.update() n'est pas ré-appelé automatiquement quand le GPS trouve un fix en tâche
    // de fond (ce n'est pas un état Compose) : on attend donc activement le premier fix ici, pour
    // centrer/zoomer la carte et corriger la position météo (au lieu de rester bloqué sur DEFAULT_CENTER).
    LaunchedEffect(locationOverlay) {
        val overlay = locationOverlay ?: return@LaunchedEffect
        while (!hasAutoZoomed) {
            val location = overlay.myLocation
            if (location != null) {
                hasAutoZoomed = true
                mapViewRef?.controller?.animateTo(location, FOCUSED_ZOOM, null)
                weatherLocation = location
            } else {
                delay(500)
            }
        }
    }

    // Suivi du guidage : tant qu'une destination est active, on recalcule l'itinéraire au fur
    // et à mesure des déplacements (sinon la distance affichée resterait figée au départ).
    LaunchedEffect(navigationTarget) {
        val target = navigationTarget ?: return@LaunchedEffect
        var lastRoutedFrom: GeoPoint? = null
        var framedOnce = false

        while (true) {
            val myLocation = locationOverlay?.myLocation
            if (myLocation != null) {
                val movedEnough = lastRoutedFrom == null || haversineMeters(
                    lastRoutedFrom!!.latitude,
                    lastRoutedFrom!!.longitude,
                    myLocation.latitude,
                    myLocation.longitude,
                ) >= ROUTE_REFRESH_DISTANCE_M

                if (movedEnough) {
                    lastRoutedFrom = GeoPoint(myLocation.latitude, myLocation.longitude)
                    viewModel.computeRoute(myLocation.latitude, myLocation.longitude)
                }

                if (!framedOnce) {
                    framedOnce = true
                    // Cadre le trajet une fois au départ, pour voir d'un coup d'oeil où on va.
                    mapViewRef?.controller?.animateTo(
                        GeoPoint(
                            (myLocation.latitude + target.latitude) / 2,
                            (myLocation.longitude + target.longitude) / 2,
                        ),
                    )
                }
            }
            delay(ROUTE_REFRESH_INTERVAL_MS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Affût", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                // IconButton (48dp) plutôt que TextButton (64dp min) : avec 6 actions, la largeur
                // minimale de TextButton écrasait le titre au point de le faire passer à la ligne
                // lettre par lettre.
                actions = {
                    IconButton(onClick = onOpenSpotList) { Text("📋") }
                    IconButton(onClick = onOpenSettings) { Text("⚙️") }
                    IconButton(onClick = { showLegendDialog = true }) { Text("🎨") }
                    IconButton(onClick = onOpenLiveShare) { Text("📡") }
                    IconButton(onClick = onOpenCompass) { Text("🧭") }
                    IconButton(onClick = onOpenForecast) { Text("🌦️") }
                },
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FloatingActionButton(
                    onClick = {
                        locationOverlay?.myLocation?.let { location ->
                            val map = mapViewRef ?: return@FloatingActionButton
                            val targetZoom = maxOf(map.zoomLevelDouble, FOCUSED_ZOOM)
                            map.controller.animateTo(location, targetZoom, null)
                        }
                    },
                ) {
                    Text("📍")
                }
                FloatingActionButton(
                    onClick = {
                        val center = locationOverlay?.myLocation ?: mapViewRef?.mapCenter
                        onCreateSpot(
                            center?.latitude ?: DEFAULT_CENTER.latitude,
                            center?.longitude ?: DEFAULT_CENTER.longitude,
                        )
                    },
                ) {
                    Text("➕")
                }
                FloatingActionButton(
                    onClick = { showOfflineDialog = true },
                ) {
                    Text("⬇️")
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(DEFAULT_ZOOM)
                        controller.setCenter(DEFAULT_CENTER)

                        val receiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                p?.let { onCreateSpot(it.latitude, it.longitude) }
                                return true
                            }
                        }
                        overlays.add(MapEventsOverlay(receiver))
                    }.also { mapViewRef = it }
                },
                update = { mapView ->
                    if (hasLocationPermission && locationOverlay == null) {
                        val overlay = MyLocationNewOverlay(FusedMyLocationProvider(context), mapView)
                        ContextCompat.getDrawable(context, R.drawable.ic_my_location_arrow)?.let {
                            overlay.setDirectionIcon(it.toBitmap())
                        }
                        ContextCompat.getDrawable(context, R.drawable.ic_my_location_dot)?.let {
                            overlay.setPersonIcon(it.toBitmap())
                        }
                        overlay.setDirectionAnchor(0.5f, 0.5f)
                        overlay.setPersonAnchor(0.5f, 0.5f)
                        overlay.enableMyLocation()
                        mapView.overlays.add(overlay)
                        locationOverlay = overlay
                    }
                    // Position de secours pour la météo tant que le premier fix GPS n'est pas encore
                    // arrivé (voir le LaunchedEffect(locationOverlay) plus haut, qui corrige avec la
                    // vraie position et zoome la carte dès que le fix arrive).
                    if (weatherLocation == null) {
                        weatherLocation = DEFAULT_CENTER
                    }

                    // Liste courte de spots/positions : on retire les anciens marqueurs et on redessine, coût négligeable.
                    mapView.overlays.removeAll { it is Marker || it is Polyline }

                    route?.let { computed ->
                        mapView.overlays.add(
                            Polyline(mapView).apply {
                                setPoints(computed.points.map { GeoPoint(it.latitude, it.longitude) })
                                outlinePaint.strokeWidth = 12f
                                outlinePaint.color = android.graphics.Color.parseColor("#FF6D00")
                            },
                        )
                    }

                    val now = System.currentTimeMillis()
                    otherLiveShares
                        .filter { it.isActive && now < it.expiresAt }
                        .forEach { position ->
                            val ageMinutes = ((now - position.updatedAt) / 60_000L).coerceAtLeast(0)
                            val status = if (ageMinutes > 5) "position ancienne" else "position récente"
                            mapView.overlays.add(
                                Marker(mapView).apply {
                                    this.position = GeoPoint(position.latitude, position.longitude)
                                    title = "Position partagée — $status (il y a $ageMinutes min)"
                                    icon = ContextCompat.getDrawable(context, R.drawable.marker_live_position)
                                },
                            )
                        }

                    spots.forEach { spot ->
                        mapView.overlays.add(
                            Marker(mapView).apply {
                                position = GeoPoint(spot.latitude, spot.longitude)
                                title = spot.title
                                icon = markerIconFor(context, spot.type)
                                setOnMarkerClickListener { _, _ ->
                                    onSpotClick(spot.id)
                                    true
                                }
                            },
                        )
                    }
                    mapView.invalidate()
                },
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                navigationTarget?.let { target ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "🧭 Vers ${target.label}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                route?.let { r ->
                                    val km = r.distanceMeters / 1000.0
                                    val minutes = r.durationSeconds / 60
                                    val mode = when {
                                        r.missingRoutingData -> "à vol d'oiseau — aucune carte de routage installée"
                                        r.computeFailed -> "à vol d'oiseau — échec du calcul d'itinéraire, réessaie"
                                        !r.followsPaths -> "à vol d'oiseau — aucun chemin trouvé"
                                        else -> "par les chemins"
                                    }
                                    // Hors chemin, le cap est la seule indication de direction utilisable.
                                    val cap = if (!r.followsPaths && r.points.size >= 2) {
                                        val from = r.points.first()
                                        val to = r.points.last()
                                        val bearing = bearingDegrees(
                                            from.latitude, from.longitude, to.latitude, to.longitude,
                                        )
                                        " · cap ${bearing.roundToInt()}° ${windDirectionLabel(bearing)}"
                                    } else {
                                        ""
                                    }
                                    "%.2f km · ~%d min · %s%s".format(km, minutes, mode, cap)
                                } ?: "Calcul de l'itinéraire…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            TextButton(onClick = viewModel::stopNavigation) { Text("Arrêter le guidage") }
                        }
                    }
                }
                if (syncState.isFailing) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Synchronisation en échec — tes spots ne sont pas partagés pour le moment.",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                if (!isOnline) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Hors connexion — les zones non téléchargées peuvent ne pas s'afficher.",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                weather?.let {
                    WeatherWidget(weather = it, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }

    if (showOfflineDialog) {
        mapViewRef?.let { mapView ->
            OfflineDownloadDialog(mapView = mapView, onDismiss = { showOfflineDialog = false })
        }
    }

    if (showLegendDialog) {
        SpotLegendDialog(onDismiss = { showLegendDialog = false })
    }

    DisposableEffect(Unit) {
        onDispose {
            locationOverlay?.disableMyLocation()
            mapViewRef?.onDetach()
        }
    }
}
