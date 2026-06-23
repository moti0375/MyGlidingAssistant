package presentation.screens.main_screen
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.data.enums.AltitudeUnits
import com.dunihuliapps.myglidingassistnat.data.enums.DistanceUnits
import com.dunihuliapps.myglidingassistnat.data.enums.data.enums.SpeedUnits
import com.dunihuliapps.myglidingassistnat.domain.formatters.UnitsFormatter
import com.dunihuliapps.myglidingassistnat.presentation.map.MapReadyListener
import com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen.FlightsListScreen
import com.dunihuliapps.myglidingassistnat.presentation.screens.gliders.gliders_screen.GlidersActivity
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.FeetFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.HmsFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.KmhFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MetricAltFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MetricFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MillageFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.presentation.units_formatters.KnotsFormatter
import dagger.hilt.android.AndroidEntryPoint
import data.model.Flight
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import presentation.map.CustomSupportMapFragment
import presentation.screens.license_screen.GmsLicenseScreen
import presentation.screens.settings_screen.SettingsActivity
import services.GlidingAssistanceService

@AndroidEntryPoint
class MainScreen : AppCompatActivity(), OnSharedPreferenceChangeListener {

    // Compose UI state
    var timerText by mutableStateOf(AnnotatedString("00:00:00"))
    var speedText by mutableStateOf(AnnotatedString("0.0"))
    var distanceText by mutableStateOf(AnnotatedString("0.0"))
    var altitudeText by mutableStateOf(AnnotatedString("0"))
    var isRecording by mutableStateOf(false)
    var showSaveDialog by mutableStateOf(false)
    var showNewFlightDialog by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var loadingMessage by mutableStateOf("")
    var gliders by mutableStateOf<List<Glider>>(emptyList())
    var flightDraft by mutableStateOf(FlightDraft())
    var speedUnitValue by mutableStateOf("1")
    var distanceUnitValue by mutableStateOf("1")
    var altitudeUnitValue by mutableStateOf("1")

    private var cameraZoom = 15f
    private var lineWidth = 5f

    private val tripManagerViewModel by viewModels<TripManagerViewModel>()

    private lateinit var speedFormatter: UnitsFormatter
    private lateinit var distanceFormatter: UnitsFormatter
    private lateinit var altitudeFormatter: UnitsFormatter
    private val timeFormatter = HmsFormatter()

    private lateinit var lm: LocationManager
    private lateinit var settings: SharedPreferences
    private var distanceUnits = DistanceUnits.Metric
    private var speedUnits = SpeedUnits.Metric
    private var altUnits = AltitudeUnits.Feet

    private var mapFrag: CustomSupportMapFragment? = null
    private var gaugesHeightPx = 0
    private var altitudeHeightPx = 0
    private var recordingService: GlidingAssistanceService? = null
    private var serviceBounded: Boolean = false
    private lateinit var serviceIntent: Intent

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settings = PreferenceManager.getDefaultSharedPreferences(this)
        settings.registerOnSharedPreferenceChangeListener(this)
        lm = getSystemService(LOCATION_SERVICE) as LocationManager
        serviceIntent = Intent(this, GlidingAssistanceService::class.java)

        setContent {
            MaterialTheme {
                MainScreenContent(
                    timerText = timerText,
                    speedText = speedText,
                    distanceText = distanceText,
                    altitudeText = altitudeText,
                    isRecording = isRecording,
                    showSaveDialog = showSaveDialog,
                    showNewFlightDialog = showNewFlightDialog,
                    gliders = gliders,
                    initialFlightGlider = flightDraft.glider,
                    initialFlightFirstPilot = flightDraft.firstPilot,
                    initialFlightSecondPilot = flightDraft.secondPilot,
                    isLoading = isLoading,
                    loadingMessage = loadingMessage,
                    speedUnitValue = speedUnitValue,
                    distanceUnitValue = distanceUnitValue,
                    altitudeUnitValue = altitudeUnitValue,
                    onUnitChanged = { key, value ->
                        settings.edit().putString(key, value).apply()
                        updatePreferences()
                    },
                    onStartStopClick = {
                        if (isRecording) {
                            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.StartStopButtonClicked)
                        } else {
                            showNewFlightDialog = true
                        }
                    },
                    onTakeOffConfirmed = { glider, firstPilot, secondPilot ->
                        showNewFlightDialog = false
                        tripManagerViewModel.addTripEvent(
                            MainScreenViewModelEvent.StartFlight(glider, firstPilot, secondPilot)
                        )
                    },
                    onNewFlightDismiss = { glider, firstPilot, secondPilot ->
                        showNewFlightDialog = false
                        tripManagerViewModel.updateFlightDraft(glider, firstPilot, secondPilot)
                    },
                    onFlightsClick = {
                        @Suppress("DEPRECATION")
                        startActivityForResult(
                            Intent(this, FlightsListScreen::class.java),
                            resources.getInteger(R.integer.GPS_TRIPS_LIST)
                        )
                    },
                    onSettingsClick = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onGlidersClick = { startActivity(Intent(this, GlidersActivity::class.java)) },
                    onLicenseClick = { startActivity(Intent(this, GmsLicenseScreen::class.java)) },
                    onMapReady = { fragment -> onFragmentReady(fragment) },
                    onGaugesHeightChanged = { px ->
                        gaugesHeightPx = px
                        updateMapPadding()
                    },
                    onAltitudeHeightChanged = { px ->
                        altitudeHeightPx = px
                        updateMapPadding()
                    },
                    onSaveConfirm = {
                        showSaveDialog = false
                        saveTrip()
                    },
                    onSaveCancel = {
                        showSaveDialog = false
                        stopRecording()
                    },
                    onLoadingCancel = {
                        isLoading = false
                        Toast.makeText(this, getString(R.string.Canceled), Toast.LENGTH_SHORT).show()
                        stopRecording()
                    }
                )
            }
        }

        subscribeTimerChanges()
        subscribeGliders()
        subscribeFlightDraft()
    }

    private fun onFragmentReady(fragment: CustomSupportMapFragment) {
        mapFrag = fragment
        fragment.lineWidth = lineWidth
        fragment.setMapReadyCallback(object : MapReadyListener {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun onMapReady() {
                onMapReadyCallback()
            }
        })
        if (fragment.isMapReady) {
            onMapReadyCallback()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun onMapReadyCallback() {
        updatePreferences()
        moveMapToInitialPosition()
        updateMapPadding()
    }

    private fun updateMapPadding() {
        mapFrag?.setPadding(0, gaugesHeightPx, 0, altitudeHeightPx)
    }

    private fun observeTripState() {
        job?.cancel()
        job = lifecycleScope.launch {
            tripManagerViewModel.flightStateFlow.collect {
                Log.i(TAG, "onTripStateChanged: $it")
                processTripState(it)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun processTripState(state: FlightState) {
        when (state) {
            is FlightState.Initiated -> mapFrag?.clearEverything()
            is FlightState.NewImageMarker -> mapFrag?.addImageMarker(state.imageMarker)
            is FlightState.FlightUpdated -> processTripUpdate(state)
            is FlightState.OnGoing -> mapFrag?.mapCameraCloseup()
            is FlightState.FlightLoaded -> processTripLoaded(state.tripUploadedResult)
            is FlightState.Stopped -> mapFrag?.mapCameraLongShot()
            is FlightState.FlightSaved -> processTripSaved(state)
            is FlightState.StartLocation -> mapFrag?.goToLocation(state.location)
            is FlightState.StartRecording -> startRecording()
            is FlightState.StopAndSave -> saveTrip()
            is FlightState.ShowSaveDialog -> showSaveDialog = true
            is FlightState.ShowRecordingInBackground -> {
                Toast.makeText(this, getString(R.string.StillRecording), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processTripLoaded(loadedResult: TripUploadedResult) {
        isLoading = false
        when (loadedResult) {
            is TripUploadedResult.Success -> {
                mapFrag?.let {
                    it.overlayRoute(loadedResult.route)
                    it.zoom = loadedResult.zoom
                    it.lineColor = loadedResult.color
                }
                Toast.makeText(this, getString(R.string.TripLoaded), Toast.LENGTH_LONG).show()
            }
            is TripUploadedResult.Failed -> {
                when (loadedResult.failures) {
                    TripLoadFailures.GenericUploadFailure ->
                        Toast.makeText(this, R.string.unable_to_open_kml, Toast.LENGTH_LONG).show()
                    TripLoadFailures.UnableLoadingDuringRecording ->
                        Toast.makeText(this, getString(R.string.upload_while_recording), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun processTripSaved(state: FlightState.FlightSaved) {
        isLoading = false
        when (state.saveStatus) {
            is SaveStatus.Success -> {
                mapFrag?.fitCameraToRoute(state.saveStatus.locations)
                Toast.makeText(this, getString(R.string.TripSaved), Toast.LENGTH_LONG).show()
            }
            is SaveStatus.NotEnoughData -> {
                Toast.makeText(this, getString(R.string.NotEnoughData), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun processTripUpdate(state: FlightState.FlightUpdated) {
        mapFrag?.moveToLocationAndDrawLine(state.location)
        speedText = speedFormatter.formatUnits(state.location.speed.toDouble()).toAnnotatedString()
        distanceText = distanceFormatter.formatUnits(state.distance.toDouble()).toAnnotatedString()
        altitudeText = altitudeFormatter.formatUnits(state.location.altitude).toAnnotatedString()
        recordingService?.updateService(state.distance)
    }

    private fun subscribeTimerChanges() {
        lifecycleScope.launch {
            tripManagerViewModel.timerStateFlow.collect {
                timerText = AnnotatedString(timeFormatter.formatTime(it))
            }
        }
    }

    private fun subscribeGliders() {
        lifecycleScope.launch {
            tripManagerViewModel.gliders.collect { gliders = it }
        }
    }

    private fun subscribeFlightDraft() {
        lifecycleScope.launch {
            tripManagerViewModel.flightDraft.collect { flightDraft = it }
        }
    }

    private fun updatePreferences() {
        val distanceUnits = settings.getString(resources.getString(R.string.distance_units_key), "1")?.toInt()
        val speedUnits = settings.getString(resources.getString(R.string.speed_units_key), "1")?.toInt()
        val color = settings.getString(resources.getString(R.string.LineColorPref), "1")?.toInt()
        val zoom = settings.getString(resources.getString(R.string.ZoomPref), "15f")?.toFloat() ?: 15f
        val width = settings.getString(resources.getString(R.string.LineWidthPref), "5f")?.toFloat() ?: 5f
        val altitudeUnits = settings.getString(resources.getString(R.string.altitude_units_key), "1")?.toInt()

        this.distanceUnits = if (distanceUnits == 2) DistanceUnits.Millage else DistanceUnits.Metric
        this.altUnits = if (altitudeUnits == 1) AltitudeUnits.Feet else AltitudeUnits.Metric
        this.speedUnits = if (speedUnits == 1) SpeedUnits.Metric else SpeedUnits.Knots

        speedUnitValue = settings.getString(resources.getString(R.string.speed_units_key), "1") ?: "1"
        distanceUnitValue = settings.getString(resources.getString(R.string.distance_units_key), "1") ?: "1"
        altitudeUnitValue = settings.getString(resources.getString(R.string.altitude_units_key), "1") ?: "1"

        setFormatters()

        mapFrag?.lineColor = when (color) {
            2 -> Color.GREEN
            3 -> Color.YELLOW
            4 -> Color.BLUE
            else -> Color.RED
        }
        mapFrag?.lineWidth = width
        mapFrag?.zoom = zoom
        cameraZoom = zoom
    }

    private fun setFormatters() {
        distanceFormatter = if (distanceUnits == DistanceUnits.Millage) MillageFormatter() else MetricFormatter()
        speedFormatter = if (speedUnits == SpeedUnits.Knots) KnotsFormatter() else KmhFormatter()
        altitudeFormatter = if (altUnits == AltitudeUnits.Feet) FeetFormatter() else MetricAltFormatter()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startRecording() {
        isRecording = true
        enableLocationListener(fixListener)
        mapFrag?.zoom = cameraZoom
        mapFrag?.tilt = 0f
        isLoading = true
        loadingMessage = getString(R.string.WaitForGPS)
    }

    private fun stopRecording() {
        disableLocationListener(fixListener)
        disableGpsLocationListener(trackLocationListener)
        tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.FinishFlight)
        isRecording = false
        stopService()
    }

    override fun onResume() {
        super.onResume()
        observeTripState()
    }

    private val fixListener = object : LocationListener {
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onLocationChanged(location: Location) {
            disableGpsLocationListener(this)
            isLoading = false
            startService()
            enableGPSLocationListener(trackLocationListener)
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.LocationFounded(location))
            Toast.makeText(this@MainScreen, resources.getString(R.string.LocationFounded), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        stopRecording()
        mapFrag?.takeMapSnapshot {
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.TripEnded(it))
        }
        try {
            stopService(serviceIntent)
            unbindService(mConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Toast.makeText(this, resources.getString(R.string.Goodbye), Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun enableGPSLocationListener(listener: LocationListener) {
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIME_INTERVAL, 0f, listener)
    }

    private fun disableGpsLocationListener(listener: LocationListener) {
        try { lm.removeUpdates(listener) } catch (e: Exception) { e.printStackTrace() }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun enableLocationListener(listener: LocationListener) {
        lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
    }

    private fun disableLocationListener(listener: LocationListener) {
        try { lm.removeUpdates(listener) } catch (e: Exception) { e.printStackTrace() }
    }

    var trackLocationListener: LocationListener =
        LocationListener { location: Location ->
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.OnNewLocation(location))
        }

    private fun saveTrip() {
        isLoading = true
        loadingMessage = getString(R.string.SavingTrip)
        stopRecording()
        mapFrag?.takeMapSnapshot {
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.TripEnded(it))
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun moveMapToInitialPosition() {
        try {
            val path = intent.getStringExtra("kml_path")
            if (path == null) moveToLastKnownLocation()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun moveToLastKnownLocation() {
        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
            mapFrag?.zoom = cameraZoom
            mapFrag?.goToLocation(it)
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as GlidingAssistanceService.LocalBinder
            recordingService = binder.service
            serviceBounded = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBounded = false
        }
    }

    private fun startService() {
        if (this::serviceIntent.isInitialized) {
            startService(serviceIntent)
            bindService(serviceIntent, mConnection, BIND_AUTO_CREATE)
        }
    }

    private fun stopService() {
        try { stopService(serviceIntent) } catch (e: Exception) { e.printStackTrace() }
        try { unbindService(mConnection) } catch (e: Exception) { e.printStackTrace() }
        serviceBounded = false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePreferences()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val hasExtras = data?.hasExtra("UploadedTrip") == true
        if (requestCode == TRIP_LIST_ACTIVITY && resultCode == RESULT_OK && hasExtras) {
            val uploadedFlight = data.getSerializableExtra("UploadedTrip") as Flight
            isLoading = true
            loadingMessage = getString(R.string.displaying_trip)
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.UploadTrip(uploadedFlight))
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    companion object {
        private val TAG: String = MainScreen::class.java.simpleName
        private const val TRIP_LIST_ACTIVITY = 100
        private const val TIME_INTERVAL: Long = 2000
    }
}

const val MAIN_MAP_FRAGMENT_TAG = "main_map_fragment"

private fun SpannableString.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    append(this@toAnnotatedString.toString())
    val allSpans = getSpans(0, length, Any::class.java)
    for (span in allSpans) {
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        when (span) {
            is RelativeSizeSpan -> addStyle(
                SpanStyle(fontSize = span.sizeChange.em),
                start, end
            )
            is ForegroundColorSpan -> addStyle(
                SpanStyle(color = ComposeColor(span.foregroundColor)),
                start, end
            )
        }
    }
}