package presentation.screens.flight_details_screen

import android.graphics.Bitmap
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.presentation.map.CustomSupportMapFragment
import com.dunihuliapps.myglidingassistnat.presentation.map.MapReadyListener
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.HmsFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MetricFormatter
import data.model.Flight

private const val MAP_FRAGMENT_TAG = "flight_map_fragment"

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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                MapFragmentContainer(
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

@Composable
private fun MapFragmentContainer(
    modifier: Modifier = Modifier,
    onFragmentReady: (CustomSupportMapFragment) -> Unit,
) {
    val context = LocalContext.current
    val fragmentManager = (context as FragmentActivity).supportFragmentManager
    val containerId = remember { View.generateViewId() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FragmentContainerView(ctx).apply { id = containerId }
        }
    )

    LaunchedEffect(containerId) {
        val existing = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as? CustomSupportMapFragment
        val fragment = existing ?: CustomSupportMapFragment()
        if (!fragment.isAdded) {
            fragmentManager.beginTransaction()
                .add(containerId, fragment, MAP_FRAGMENT_TAG)
                .commitNow()
        }
        onFragmentReady(fragment)
    }

    DisposableEffect(Unit) {
        onDispose {
            fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG)?.let { fragment ->
                fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }
}

@Composable
private fun FlightDetailsPanel(
    modifier: Modifier = Modifier,
    flight: Flight?,
) {
    val timeFormatter = remember { HmsFormatter() }
    val distanceFormatter = remember { MetricFormatter() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        flight?.let { f ->
            f.date?.let {
                FlightDetailRow(Icons.Default.CalendarToday, it)
            }
            FlightDetailRow(
                icon = Icons.Default.Timer,
                value = timeFormatter.formatTime(f.duration)
            )
            FlightDetailRow(
                icon = Icons.Default.Explore,
                value = distanceFormatter.formatUnits(f.maxDistance.toDouble()).toString()
            )
            FlightDetailRow(
                icon = Icons.Default.Straighten,
                value = distanceFormatter.formatUnits(f.overallDistance.toDouble()).toString()
            )
            f.glider?.let { gliderName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GliderIcon(
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = gliderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            f.firstPilot?.let {
                FlightDetailRow(Icons.Default.Person, it)
            }
            f.secondPilot?.let {
                FlightDetailRow(Icons.Default.Person, it)
            }
            f.name?.let {
                FlightDetailRow(Icons.Default.Notes, it)
            }
        }
    }
}

@Composable
private fun FlightDetailRow(
    icon: ImageVector,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GliderIcon(modifier: Modifier = Modifier, tint: Color) {
    val context = LocalContext.current
    val tintArgb = tint.toArgb()
    val drawable = remember(tintArgb) {
        AppCompatResources.getDrawable(context, R.drawable.ic_glider_icon)?.mutate()?.also {
            it.setTint(tintArgb)
        }
    }
    Canvas(modifier = modifier) {
        drawable?.let { d ->
            d.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawIntoCanvas { canvas ->
                d.draw(canvas.nativeCanvas)
            }
        }
    }
}
