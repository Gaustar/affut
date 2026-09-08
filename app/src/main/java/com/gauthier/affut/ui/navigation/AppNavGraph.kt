package com.gauthier.affut.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gauthier.affut.ui.compass.CompassScreen
import com.gauthier.affut.ui.forecast.ForecastScreen
import com.gauthier.affut.ui.liveshare.LiveShareScreen
import com.gauthier.affut.ui.map.MapScreen
import com.gauthier.affut.ui.settings.SettingsScreen
import com.gauthier.affut.ui.spotlist.SpotListScreen
import com.gauthier.affut.ui.spotdetail.SpotDetailScreen
import com.gauthier.affut.ui.spotedit.SpotEditScreen

/** Graphe de navigation de l'application. */
object Routes {
    const val MAP = "map"
    const val SPOT_DETAIL = "spot_detail/{spotId}"
    const val SPOT_CREATE = "spot_create/{lat}/{lon}"
    const val SPOT_EDIT = "spot_edit/{spotId}"
    const val FORECAST = "forecast"
    const val FORECAST_AT = "forecast_at/{lat}/{lon}/{label}"
    const val COMPASS = "compass"
    const val LIVE_SHARE = "live_share"
    const val SETTINGS = "settings"
    const val SPOT_LIST = "spot_list"

    fun spotDetail(id: String) = "spot_detail/$id"
    fun forecastAt(lat: Double, lon: Double, label: String) = "forecast_at/$lat/$lon/${Uri.encode(label)}"
    fun spotCreate(lat: Double, lon: Double) = "spot_create/$lat/$lon"
    fun spotEdit(id: String) = "spot_edit/$id"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MAP) {
        composable(Routes.MAP) {
            MapScreen(
                onSpotClick = { id -> navController.navigate(Routes.spotDetail(id)) },
                onCreateSpot = { lat, lon -> navController.navigate(Routes.spotCreate(lat, lon)) },
                onOpenForecast = { navController.navigate(Routes.FORECAST) },
                onOpenCompass = { navController.navigate(Routes.COMPASS) },
                onOpenLiveShare = { navController.navigate(Routes.LIVE_SHARE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSpotList = { navController.navigate(Routes.SPOT_LIST) },
            )
        }
        composable(Routes.FORECAST) {
            ForecastScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.FORECAST_AT,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType },
                navArgument("label") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            ForecastScreen(
                onBack = { navController.popBackStack() },
                latitude = args?.getString("lat")?.toDoubleOrNull(),
                longitude = args?.getString("lon")?.toDoubleOrNull(),
                placeLabel = args?.getString("label"),
            )
        }
        composable(Routes.COMPASS) {
            CompassScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SPOT_LIST) {
            SpotListScreen(
                onBack = { navController.popBackStack() },
                onSpotClick = { id -> navController.navigate(Routes.spotDetail(id)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LIVE_SHARE) {
            LiveShareScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.SPOT_DETAIL,
            arguments = listOf(navArgument("spotId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val spotId = backStackEntry.arguments?.getString("spotId") ?: return@composable
            SpotDetailScreen(
                spotId = spotId,
                onEdit = { id -> navController.navigate(Routes.spotEdit(id)) },
                onBack = { navController.popBackStack() },
                onOpenForecast = { lat, lon, label ->
                    navController.navigate(Routes.forecastAt(lat, lon, label))
                },
            )
        }
        composable(
            route = Routes.SPOT_CREATE,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
            SpotEditScreen(
                spotId = null,
                initialLatitude = lat,
                initialLongitude = lon,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.SPOT_EDIT,
            arguments = listOf(navArgument("spotId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val spotId = backStackEntry.arguments?.getString("spotId") ?: return@composable
            SpotEditScreen(
                spotId = spotId,
                initialLatitude = 0.0,
                initialLongitude = 0.0,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
