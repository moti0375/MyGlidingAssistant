package presentation.navigation

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.utils.Utils
import data.model.Flight
import presentation.screens.flight_details_screen.FlightDetailsContent
import presentation.screens.flight_details_screen.FlightDetailsViewModel
import presentation.screens.flights_screen.FlightsListContent
import presentation.screens.flights_screen.FlightsListViewModel
import presentation.screens.gliders.edit_glider_screen.EditGliderScreen
import presentation.screens.gliders.edit_glider_screen.EditGliderViewModel
import presentation.screens.gliders.gliders_screen.GlidersScreen
import presentation.screens.gliders.gliders_screen.GlidersViewModel
import presentation.screens.settings_screen.SettingsContent
import presentation.screens.settings_screen.SettingsViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    onUploadTrip: (Flight) -> Unit,
) {
    val context = LocalContext.current as ComponentActivity

    NavHost(navController = navController, startDestination = "main") {

        composable("main") {
            // Main screen is rendered outside the NavHost (in MainActivity's Box),
            // keeping it always in composition so the map fragment is never detached.
        }

        composable("flights") {
            val viewModel: FlightsListViewModel = hiltViewModel()
            FlightsListContent(
                viewModel = viewModel,
                onFlightClick = { flight ->
                    navController.navigate("flight_details/${flight.id}")
                },
                onUploadConfirmed = { flight ->
                    onUploadTrip(flight)
                    navController.popBackStack("main", inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "flight_details/{trip_id}",
            arguments = listOf(navArgument("trip_id") { type = NavType.LongType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments!!.getLong("trip_id")
            val viewModel: FlightDetailsViewModel = hiltViewModel()
            FlightDetailsContent(
                viewModel = viewModel,
                tripId = tripId,
                onBack = { navController.popBackStack() },
                onShareMapImage = { bitmap, imageFileName, tripTitle ->
                    shareMapImage(context, bitmap, imageFileName, tripTitle)
                }
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsContent(
                viewModel = viewModel,
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("gliders") {
            val viewModel: GlidersViewModel = hiltViewModel()
            GlidersScreen(
                viewModel = viewModel,
                onAddClick = { glider ->
                    if (glider == null) {
                        navController.navigate("edit_glider")
                    } else {
                        val route = buildString {
                            append("edit_glider")
                            append("?id=${glider.id}")
                            append("&type=${Uri.encode(glider.type)}")
                            append("&callsign=${Uri.encode(glider.callsign)}")
                            append("&seats=${glider.seats}")
                            append("&ratio=${glider.ratio}")
                            append("&image=${Uri.encode(glider.gliderImage ?: "")}")
                        }
                        navController.navigate(route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_glider?id={id}&type={type}&callsign={callsign}&seats={seats}&ratio={ratio}&image={image}",
            arguments = listOf(
                navArgument("id")       { type = NavType.LongType;   defaultValue = 0L },
                navArgument("type")     { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("callsign") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("seats")    { type = NavType.IntType;    defaultValue = 1 },
                navArgument("ratio")    { type = NavType.IntType;    defaultValue = 20 },
                navArgument("image")    { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            val viewModel: EditGliderViewModel = hiltViewModel()
            EditGliderScreen(
                viewModel = viewModel,
                onSaved = { navController.popBackStack() }
            )
        }

    }
}

private fun shareMapImage(
    activity: ComponentActivity,
    bitmap: Bitmap,
    imageFileName: String,
    tripTitle: String?,
) {
    val icon = BitmapFactory.decodeResource(activity.resources, R.drawable.ic_launcher)
    val image = Utils.overlay(bitmap, icon, activity.getString(R.string.app_name))

    val share = Intent(Intent.ACTION_SEND).apply { type = "image/png" }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, tripTitle ?: activity.getString(R.string.app_name))
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TripRecorder")
    }
    val uri: Uri? = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    try {
        uri?.let {
            activity.contentResolver.openOutputStream(it)?.use { os ->
                image?.compress(Bitmap.CompressFormat.PNG, 100, os)
                image?.recycle()
            }
            share.putExtra(Intent.EXTRA_STREAM, it)
            activity.startActivity(Intent.createChooser(share, activity.getString(R.string.share_map_image)))
        }
    } catch (e: Exception) {
        Log.e("AppNavHost", "Failed to share map image", e)
        Toast.makeText(activity, "Failed to share map image", Toast.LENGTH_LONG).show()
    }
}
