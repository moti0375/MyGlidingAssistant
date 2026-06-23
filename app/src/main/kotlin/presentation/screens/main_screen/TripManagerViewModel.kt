package presentation.screens.main_screen
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dunihuliapps.myglidingassistnat.data.enums.RecordingMode
import com.dunihuliapps.myglidingassistnat.data.enums.RecordingState
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.data.repositories.flights.FlightsRepository
import com.dunihuliapps.myglidingassistnat.data.repositories.gliders.GlidersRepository
import com.dunihuliapps.myglidingassistnat.domain.files.kml.KmlManager
import com.dunihuliapps.myglidingassistnat.domain.files.path_provider.PathProvider
import com.dunihuliapps.myglidingassistnat.domain.timer.TripTimer
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Flight
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.sql.Date
import java.text.SimpleDateFormat
import javax.inject.Inject

@HiltViewModel
class TripManagerViewModel @Inject constructor(
    private val pathProvider: PathProvider,
    private val flightsRepository: FlightsRepository,
    private val glidersRepository: GlidersRepository,
    private val timer: TripTimer,
    private val kmlManager: KmlManager,
    private val sharedPreferences: SharedPreferences
) : ViewModel(), DefaultLifecycleObserver {
    private val tripMutableStateFlow = MutableSharedFlow<FlightState>(replay = 0, extraBufferCapacity = 64)
    val flightStateFlow: SharedFlow<FlightState> = tripMutableStateFlow.asSharedFlow()
    val timerStateFlow = timer.timerStateFlow

    val gliders: StateFlow<List<Glider>> = glidersRepository.getAllGliders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _flightDraft = MutableStateFlow(FlightDraft())
    val flightDraft: StateFlow<FlightDraft> = _flightDraft.asStateFlow()

    private var startLocation: Location? = null
    private var recordingMode = RecordingMode.NEW_TRIP
    private var takeOffTime: Long = 0
    private var landingTime: Long = 0
    private var currentFlightGlider: String? = null
    private var currentFlightFirstPilot: String? = null
    private var currentFlightSecondPilot: String? = null
    /**
     * The current location
     */
    private var currentLocation: Location? = null
    private var accuracy = DEFAULT_ACCURACY
    private var overallDistance = 0.0f
    private var maxDistance = 0.0f
    private var heading = 0f

    private var speed: Double = 0.0

    private val locations = mutableListOf<String>()
    private var latLngList = mutableListOf<LatLng>()
    private val markplace = StringBuilder()
    private var altitude: Double = 0.0
    private var maxAltitude = 0.0
    private var duration: Long = 0
    private var uploadedFlight: Flight? = null
    private var recordingState = RecordingState.Idle
    private var autoSave: Boolean = false

    init {
        resetRoute(true)
        autoSave = sharedPreferences.getBoolean("AutoSavePrefKey", true)
    }


    fun addTripEvent(mainScreenViewModelEvent: MainScreenViewModelEvent) {
        mapEventToState(mainScreenViewModelEvent)
    }

    private fun mapEventToState(event: MainScreenViewModelEvent) {
        when (event) {
            is MainScreenViewModelEvent.AccuracyChanged -> updateAccuracy(event.accuracy)
            is MainScreenViewModelEvent.LocationFounded -> setCurrentLocation(event.location)
            is MainScreenViewModelEvent.OnNewLocation -> updateLocation(event.location)
            is MainScreenViewModelEvent.SpeedFilterUpdated -> setSpeedFilter(event.filter)
            is MainScreenViewModelEvent.TripEnded -> saveTrip(event.bitmap)
            is MainScreenViewModelEvent.UploadTrip -> uploadTrip(event.flight)
            is MainScreenViewModelEvent.RecordingModeChanged -> updateRecordingMode(event.recordingMode)
            is MainScreenViewModelEvent.TakeOff -> takeOff()
            is MainScreenViewModelEvent.FinishFlight -> finishFlight()
            is MainScreenViewModelEvent.StartStopButtonClicked -> handleStartStopClicked()
            is MainScreenViewModelEvent.StartFlight -> startFlight(event.glider, event.firstPilot, event.secondPilot)
        }
    }

    private fun handleStartStopClicked() {
        if (recordingState == RecordingState.Idle) {
            uploadedFlight?.let {
                if (recordingMode == RecordingMode.LOADED_FROM_INTENT) {
                    takeOff()
                } else {
                    //todo implement logic
                    //loadedTripDialog()
                }
            } ?: run {
                resetRoute(true)
                recordingMode = RecordingMode.NEW_TRIP
                takeOff()
            }

        } else {
            if (autoSave) {
                publishFlightState(FlightState.StopAndSave)
            } else {
                publishFlightState(FlightState.ShowSaveDialog)
            }
        }
    }

    fun updateFlightDraft(glider: String?, firstPilot: String, secondPilot: String) {
        _flightDraft.value = FlightDraft(glider, firstPilot, secondPilot)
    }

    private fun startFlight(glider: String?, firstPilot: String?, secondPilot: String?) {
        currentFlightGlider = glider
        currentFlightFirstPilot = firstPilot
        currentFlightSecondPilot = secondPilot
        _flightDraft.value = FlightDraft(glider, firstPilot ?: "", secondPilot ?: "")
        resetRoute(true)
        recordingMode = RecordingMode.NEW_TRIP
        takeOff()
    }

    private fun takeOff() {
        resetRoute(recordingMode != RecordingMode.FOLLOW_TRIP && recordingMode != RecordingMode.CONTINUE_TRIP)
        timer.apply {
            resetTimer()
            startTimer()
        }
        publishFlightState(FlightState.StartRecording)
        recordingState = RecordingState.Recording
    }

    private fun finishFlight() {
        timer.stopTimer()
    }

    /**
     * @param newLocation update the current with the new location and calculate the
     * distance between them
     */
    private fun updateLocation(newLocation: Location) {
        Log.i(
            TAG,
            "New Location accuracy: " + newLocation.accuracy + ", speed: " + newLocation.speed + ", hasSpeed: " + newLocation.hasSpeed()
        )

        markplace.replace(
            0, markplace.length, ("" + newLocation.longitude + ","
                    + newLocation.latitude)
        )

        //        Log.i(LOG_TAG, "new location accuracy " + newLocation.getAccuracy());
        if (this.startLocation == null) { // taking care the first location...
            if (newLocation.accuracy < (accuracy + accuracy * ACCURACY_TOLERANCE)) { // for first location accuracy is less important, no speed required.
                this.startLocation = newLocation
                this.currentLocation = newLocation
                locations.add(markplace.toString())
                latLngList.add(LatLng(newLocation.latitude, newLocation.longitude))
                publishFlightState(FlightState.FlightUpdated(newLocation, overallDistance))
            }
        } else { // all other locations
            // only locations that has speed, bearing, and bigger than the
            // speed_filter and accurate will be taken! all other will be
            // ignored!
            if ((newLocation.accuracy < accuracy)) {
                locations.add(markplace.toString())
                latLngList.add(LatLng(newLocation.latitude, newLocation.longitude))

                val portionLength = FloatArray(1)
                val distanceFromStart = FloatArray(1)
                currentLocation?.let {
                    Location.distanceBetween(
                        it.latitude,
                        it.longitude, newLocation.latitude,
                        newLocation.longitude, portionLength
                    )

                    Location.distanceBetween(
                        it.latitude,
                        it.longitude, newLocation.latitude,
                        newLocation.longitude, distanceFromStart
                    )
                }


                this.overallDistance += portionLength[0]
                if (maxDistance < portionLength[0]) {
                    maxDistance = portionLength[0]
                }

                this.currentLocation = newLocation
                this.speed = newLocation.speed.toDouble()
                publishFlightState(FlightState.FlightUpdated(newLocation, overallDistance))

                if (newLocation.hasAltitude()) {
                    this.altitude = newLocation.altitude
                    if (this.altitude > this.maxAltitude) {
                        this.maxAltitude = this.altitude
                    }
                }
            }
            updateRouteStatus(newLocation)
        }
    }

    /**
     * Initialize the measured distance to 0.0
     */
    private fun resetRoute(resetMap: Boolean) {
        this.overallDistance = 0f
        this.speed = 0.0
        this.altitude = 0.0
        this.maxAltitude = 0.0
        this.startLocation = null
        this.currentLocation = null
        this.maxAltitude = 0.0

        if (resetMap) {
            publishFlightState(FlightState.Initiated)
        }

        locations.clear()
        latLngList.clear()
    }

    private fun updateRouteStatus(location: Location) {
        if (location.hasBearing()) {
            heading = location.bearing
        }
        this.speed = location.speed.toDouble()
    }


    @SuppressLint("SimpleDateFormat")
    fun saveTrip(flightMapImage: Bitmap?) {
        Log.i("TripManager", "About to save trip")
        viewModelScope.launch {
            if (latLngList.size > 1) {
                val timestamp = System.currentTimeMillis()
                val mapImageFile = "${pathProvider.providerImagesFilesPath()}/trip_$timestamp.jpeg"
                val sdf = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm")
                val date = sdf.format(Date(System.currentTimeMillis()))
                val mapFile = kmlManager.generateKmlFromLocationList(latLngList) // creating and
                duration = timer.getDuration() //mSec
                val flight = Flight(
                    kml = mapFile,
                    date = date,
                    overallDistance = overallDistance,
                    duration = duration,
                    maxDistance = maxDistance,
                    maxAlt = maxAltitude,
                    imageFileName = mapImageFile,
                    glider = currentFlightGlider,
                    firstPilot = currentFlightFirstPilot,
                    secondPilot = currentFlightSecondPilot,
                )

                val tripId = flightsRepository.insertFlight(flight)

                Log.i(TAG, "data.model.Trip file $mapFile saved")
                flightMapImage?.let {
                    saveTripImage(it, tripId, mapImageFile)
                }
                publishFlightState(FlightState.FlightSaved(SaveStatus.Success(latLngList)))
            } else {
                publishFlightState(FlightState.FlightSaved(SaveStatus.NotEnoughData))
            }
        }
        recordingState = RecordingState.Idle
    }

    private fun uploadTrip(flight: Flight) {
        val tripRoute = flight.kml?.let {
            kmlManager.getLocationsFromKml(it)
        } ?: emptyList()

        if (tripRoute.isEmpty()) {
            publishFlightState(FlightState.FlightLoaded(TripUploadedResult.Failed(TripLoadFailures.GenericUploadFailure)))
            return
        } else {
            latLngList.apply {
                clear()
                addAll(tripRoute)
            }
        }

        this.uploadedFlight = flight.also {
            this.overallDistance = it.overallDistance
            this.duration = it.duration
            timer.setStartTime(it.duration)
        }

        publishFlightState(
            FlightState.FlightLoaded(
                tripUploadedResult = TripUploadedResult.Success(
                    latLngList,
                    10f,
                    Color.CYAN,
                )
            )
        )
    }

    private suspend fun saveTripImage(image: Bitmap, tripId: Long, mapImageFile: String) {
        flightsRepository.updateFlightImage(image, tripId, mapImageFile)
    }

    private fun updateAccuracy(accuracy: Float) {
        this.accuracy = accuracy
    }

    private fun setCurrentLocation(location: Location) {
        currentLocation = location
        publishFlightState(FlightState.StartLocation(location))
        if (recordingState == RecordingState.Recording) {
            if (recordingMode == RecordingMode.CONTINUE_TRIP) {
                timer.resumeTimer()
            } else {
                timer.startTimer(true)
            }
        }
    }

    private fun updateRecordingMode(recordingMode: RecordingMode) {
        this.recordingMode = recordingMode
    }

    private fun setSpeedFilter(speedFilter: Double) {
        SPEED_FILTER
    }


    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (recordingState == RecordingState.Recording) {
            publishFlightState(FlightState.ShowRecordingInBackground)
        }
    }

    private fun publishFlightState(state: FlightState) {
        tripMutableStateFlow.tryEmit(state)
    }

    companion object {
        private const val TAG =
            "com.bartovapps.gpstriprec.presentation.screens.main_screen.TripManager"

        const val MERGE_SUCCESS: Int = 1
        const val KML_NOT_FOUND: Int = 2
        const val UNABLE_TO_MERGE: Int = 3
        const val ACCURACY_TOLERANCE = 0.1F
        const val DEFAULT_ACCURACY = 25.0F
        const val SPEED_FILTER: Float = 0.832f // 0.833 < 3km/h
        private const val CONTINUE_TRIPS_GAP = 300
    }
}

data class FlightDraft(
    val glider: String? = null,
    val firstPilot: String = "",
    val secondPilot: String = "",
)