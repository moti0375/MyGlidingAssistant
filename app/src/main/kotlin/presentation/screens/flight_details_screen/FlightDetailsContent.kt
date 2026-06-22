package presentation.screens.flight_details_screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dunihuliapps.myglidingassistnat.presentation.map.MapReadyListener
import presentation.composables.FlightDetailsPanel
import presentation.composables.MapContainer
import presentation.map.CustomSupportMapFragment


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsContent(
    viewModel: FlightDetailsViewModel,
    tripId: Long,
    onBack: () -> Unit,
    onShareMapImage: (Bitmap, String, String?) -> Unit,
) {
    val state by viewModel.tripDetailsStateFlow.collectAsState()
    var mapFragmentRef by remember { mutableStateOf<CustomSupportMapFragment?>(null) }

    LaunchedEffect(state) {
        when (val s = state) {
            is FlightDetailsState.FlightLoaded -> {
                mapFragmentRef?.apply {
                    clearEverything()
                    if (s.locations.isNotEmpty()) overlayRoute(s.locations)
                }
            }
            is FlightDetailsState.MapImageFileReady -> {
                mapFragmentRef?.takeMapSnapshot { bitmap ->
                    bitmap?.let { onShareMapImage(it, s.file, s.tripTitle) }
                }
            }
            else -> {}
        }
    }

    val flight = (state as? FlightDetailsState.FlightLoaded)?.flight
    val isLoading = state is FlightDetailsState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(flight?.name ?: "Flight Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.addEvent(FlightDetailsEvent.ShareFlightMapImage)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                MapContainer(
                    modifier = Modifier.fillMaxSize(),
                    onFragmentReady = { fragment ->
                        mapFragmentRef = fragment
                        if (fragment.isMapReady) {
                            viewModel.addEvent(FlightDetailsEvent.LoadFlight(tripId))
                        } else {
                            fragment.setMapReadyCallback(object : MapReadyListener {
                                override fun onMapReady() {
                                    viewModel.addEvent(FlightDetailsEvent.LoadFlight(tripId))
                                }
                            })
                        }
                    }
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            HorizontalDivider()
            FlightDetailsPanel(
                modifier = Modifier.weight(1f),
                flight = flight
            )
        }
    }
}