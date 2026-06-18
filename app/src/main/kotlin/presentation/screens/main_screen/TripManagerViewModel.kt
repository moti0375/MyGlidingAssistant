package com.dunihuliapps.myglidingassistnat.presentation.screens.main_screen

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.dunihuliapps.myglidingassistnat.data.enums.MovementState
import com.dunihuliapps.myglidingassistnat.data.enums.RecordingMode
import com.dunihuliapps.myglidingassistnat.data.enums.RecordingState
import com.dunihuliapps.myglidingassistnat.domain.db.TripsDBOpenHelper
import com.dunihuliapps.myglidingassistnat.domain.db.TripsDataSource
import com.dunihuliapps.myglidingassistnat.domain.files.kml.KmlManager
import com.dunihuliapps.myglidingassistnat.domain.files.path_provider.PathProvider
import com.dunihuliapps.myglidingassistnat.domain.map_helper.ImageMarker
import com.dunihuliapps.myglidingassistnat.domain.timer.TripTimer
import com.dunihuliapps.myglidingassistnat.utils.Utils
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.sql.Date
import java.text.SimpleDateFormat
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class TripManagerViewModel @Inject constructor(
    private val pathProvider: PathProvider,
    private val datasource: TripsDataSource,
    private val timer: TripTimer,
    private val kmlManager: KmlManager,
    private val geocoder: Geocoder,
    private val sharedPreferences: SharedPreferences
) : ViewModel(), DefaultLifecycleObserver {
    private val tripMutableStateFlow = MutableStateFlow<TripState>(TripState.Initiated)
    val tripStateFlow: StateFlow<TripState> = tripMutableStateFlow.asStateFlow()
    val timerStateFlow = timer.timerStateFlow

    private var startLocation: Location? = null
    private var recordingMode = RecordingMode.NEW_TRIP

    /**
     * The current location
     */
    private var currentLocation: Location? = null
    private var accuracy = DEFAULT_ACCURACY
    private var distance = 0f
    private val portionLength = FloatArray(3)
    private var heading = 0f

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var speed: Double = 0.0

    private val locations = mutableListOf<String>()
    private var latLngList = mutableListOf<LatLng>()
    private val markplace = StringBuilder()
    private var averageSpeed = 0.0
    private var averageMoveSpeed = 0.0
    private var maxSpeed = 0.0
    private var altitude: Double = 0.0
    private var maxAltitude = 0.0
    private var stopTime: Long = 0
    private var overallStopTime: Long = 0
    private var movementTime: Long = 0
    private var duration: Long = 0
    private val startAddress = "none"
    private val stopAddress = "none"
    private var moveState: MovementState? = null
    private var uploadedTrip: Trip? = null
    private var imageMarkers = mutableListOf<ImageMarker>()
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
            is MainScreenViewModelEvent.MergeTrips -> mergeTrips(event.tripA, event.tripB)
            is MainScreenViewModelEvent.OnNewLocation -> updateLocation(event.location)
            is MainScreenViewModelEvent.PictureTaken -> addImageMarker(event.capturedImageUri)
            is MainScreenViewModelEvent.SpeedFilterUpdated -> setSpeedFilter(event.filter)
            is MainScreenViewModelEvent.TripEnded -> saveTrip(event.bitmap)
            is MainScreenViewModelEvent.UploadTrip -> uploadTrip(event.trip)
            is MainScreenViewModelEvent.RecordingModeChanged -> updateRecordingMode(event.recordingMode)
            is MainScreenViewModelEvent.StartTrip -> startTrip()
            is MainScreenViewModelEvent.StopTrip -> stopTrip()
            is MainScreenViewModelEvent.StartStopButtonClicked -> handleStartStopClicked()
        }
    }

    private fun handleStartStopClicked() {

        if (recordingState == RecordingState.Idle) {
            if (uploadedTrip != null) {
                if (recordingMode == RecordingMode.LOADED_FROM_INTENT) {
                    startTrip()
                } else {
                    //todo implement logic
                    //loadedTripDialog()
                }
            } else {
                resetRoute(true)
                recordingMode = RecordingMode.NEW_TRIP
                startTrip()
            }
        } else {
            if (autoSave) {
                publishTripState(TripState.StopAndSave)
            } else {
                publishTripState(TripState.ShowSaveDialog)
            }
        }
    }

    private fun startTrip() {
        resetRoute(recordingMode != RecordingMode.FOLLOW_TRIP && recordingMode != RecordingMode.CONTINUE_TRIP)
        timer.apply {
            resetTimer()
            startTimer()
        }
        publishTripState(TripState.StartRecording)
        recordingState = RecordingState.Recording
    }

    private fun stopTrip() {
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
        this.latitude = newLocation.latitude
        this.longitude = newLocation.longitude

        markplace.replace(
            0, markplace.length, ("" + this.longitude + ","
                    + this.latitude)
        )

        //        Log.i(LOG_TAG, "new location accuracy " + newLocation.getAccuracy());
        if (this.startLocation == null) { // taking care the first location...
            if (newLocation.accuracy < (accuracy + accuracy * ACCURACY_TOLERANCE)) { // for first location accuracy is less important, no speed required.
                this.startLocation = newLocation
                this.currentLocation = newLocation
                locations.add(markplace.toString())
                latLngList.add(LatLng(this.latitude, this.longitude))
                publishTripState(TripState.TripUpdated(newLocation, distance))
            }
        } else { // all other locations
            // only locations that has speed, bearing, and bigger than the
            // speed_filter and accurate will be taken! all other will be
            // ignored!
            if ((newLocation.accuracy < accuracy)) {
                locations.add(markplace.toString())
                latLngList.add(LatLng(this.latitude, this.longitude))

                currentLocation?.let {
                    Location.distanceBetween(
                        it.latitude,
                        it.longitude, this.latitude,
                        this.longitude, portionLength
                    )
                }

                this.distance += portionLength[0]
                this.currentLocation = newLocation
                this.speed = newLocation.speed.toDouble()
                publishTripState(TripState.TripUpdated(newLocation, distance))
                if (speed > maxSpeed) {
                    maxSpeed = speed
                }

                if (newLocation.hasAltitude()) {
                    this.altitude = newLocation.altitude
                    if (this.altitude > this.maxAltitude) {
                        this.maxAltitude = this.altitude
                    }
                }

                if (moveState == null) {
                    moveState = MovementState.Moving
                } else if (moveState == MovementState.Stopped) {
                    moveState = MovementState.Moving
                    stopTime = System.currentTimeMillis() - stopTime
                    overallStopTime += stopTime
                    publishTripState(TripState.OnGoing)
                }
            }
            updateRouteStatus(newLocation)
        }
    }

    /**
     * Initialize the measured distance to 0.0
     */
    private fun resetRoute(resetMap: Boolean) {
        this.distance = 0f
        this.speed = 0.0
        this.altitude = 0.0
        this.maxAltitude = 0.0
        this.startLocation = null
        this.currentLocation = null
        this.maxSpeed = 0.0
        this.moveState = null
        this.overallStopTime = 0
        this.maxAltitude = 0.0
        this.movementTime = 0

        if (resetMap) {
            publishTripState(TripState.Initiated)
        }

        locations.clear()
        latLngList.clear()
        imageMarkers.clear()
    }

    private fun updateRouteStatus(location: Location) {
        if (location.hasBearing()) {
            heading = location.bearing
        }
        if ((!location.hasSpeed() || location.speed == 0f) && !location.hasBearing()) { //this means that we stopped!!
            this.speed = 0.0
            if (moveState == MovementState.Moving) {
                moveState = MovementState.Stopped
                stopTime = System.currentTimeMillis()
                publishTripState(TripState.Stopped)
                currentLocation?.let {
                    publishTripState(TripState.TripUpdated(it, distance))
                }
                speed = location.speed.toDouble() // m/sec
            }
        } else {
            this.speed = location.speed.toDouble()
        }
    }

    /**
     * @return the route current heading direction
     */
    fun getHeading(): Double {
        return heading.toDouble()
    }

    /**
     * @return returns the distance that passed so far in Meters.
     */
    fun getDistance(): Float {
        if (currentLocation == null) {
            return 0.0F
        }
        return this.distance
    }


    @SuppressLint("SimpleDateFormat")
    fun saveTrip(tripImage: Bitmap?) {
        Log.i("TripManager", "About to save trip")
        if (latLngList.size > 1) {
            val timestamp = System.currentTimeMillis()
            val mapImageFile = "${pathProvider.providerImagesFilesPath()}/trip_$timestamp.jpeg"
            val sdf = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm")
            val date = sdf.format(Date(System.currentTimeMillis()))
            val mapFile = kmlManager.generateKmlFromLocationList(latLngList) // creating and
            val startAddress = startLocation?.let { getAddress(LatLng(it.latitude, it.longitude)) }
            val stopAddress = currentLocation?.let { getAddress(LatLng(it.latitude, it.longitude)) }

            Log.i(TAG, "End address: $stopAddress");
            duration = timer.getDuration() //mSec
            averageSpeed = (distance / (duration / 1000).toInt()).toDouble() // m/sec
            movementTime = duration - overallStopTime //mSec
            averageMoveSpeed = (distance / (movementTime / 1000).toInt()).toDouble() //m/sec
            val trip = Trip(
                kml = mapFile,
                date = date,
                distance = distance,
                moveAverageSpeed = averageSpeed,
                duration = duration,
                moveTime = movementTime,
                stopTime = overallStopTime,
                startAddress = startAddress,
                stopAddress = stopAddress,
                maxSpeed = maxSpeed,
                imageFileName = mapImageFile,
                maxAlt = this.maxAltitude,
            )

            val tripId = datasource.create(trip)

            if (imageMarkers.isNotEmpty()) {
                datasource.insertImageMarkers(imageMarkers, tripId.toDouble())
            }

            Log.i(TAG, "data.model.Trip file $mapFile saved")
            if (tripImage != null) {
                saveTripImage(tripImage, tripId, mapImageFile)
            }
            publishTripState(TripState.TripSaved(SaveStatus.Success(latLngList)))
        } else {
            publishTripState(TripState.TripSaved(SaveStatus.NotEnoughData))
        }
        recordingState = RecordingState.Idle
    }

    private fun uploadTrip(trip: Trip) {
        val tripRoute = trip.kml?.let {
            kmlManager.getLocationsFromKml(it)
        } ?: emptyList()

        if (tripRoute.isEmpty()) {
            publishTripState(TripState.TripLoaded(TripUploadedResult.Failed(TripLoadFailures.GenericUploadFailure)))
            return
        } else {
            latLngList.apply {
                clear()
                addAll(tripRoute)
            }
        }

        this.uploadedTrip = trip.also {
            this.distance = it.distance
            this.maxSpeed = it.maxSpeed
            this.duration = it.duration
            this.movementTime = it.moveTime
            this.overallStopTime = it.stopTime
            timer.setStartTime(it.duration)
        }

        imageMarkers.apply {
            clear()
            addAll(datasource.findAllMarkersForTrip(trip.id))
        }
        publishTripState(
            TripState.TripLoaded(
                tripUploadedResult = TripUploadedResult.Success(
                    latLngList,
                    10f,
                    Color.CYAN,
                    imageMarkers
                )
            )
        )
    }

    @Throws(Exception::class)
    fun addImageMarker(markerUri: Uri) {
        val imageMarker =
            ImageMarker(markerUri, currentLocation!!.latitude, currentLocation!!.longitude)
        imageMarkers.add(imageMarker)
        publishTripState(TripState.NewImageMarker(imageMarker))
    }

    private fun saveTripImage(image: Bitmap, tripId: Long, mapImageFile: String) {
        try {
            val out = FileOutputStream(mapImageFile)
            val bm: Bitmap = Bitmap.createScaledBitmap(
                image, 500,
                500, false
            )
            bm.compress(Bitmap.CompressFormat.JPEG, 50, out)
            bm.recycle()

            datasource.updateTripData(tripId, TripsDBOpenHelper.COLUMN_MAP_IMAGE, mapImageFile)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                "com.bartovapps.gpstriprec.core.map_helper.com.bartovapps.gpstriprec.domain.map_helper.MapHelper",
                "There was an exception: " + e.message
            )
        }

    }

    private fun updateAccuracy(accuracy: Float) {
        this.accuracy = accuracy
    }

    private fun setCurrentLocation(location: Location) {
        currentLocation = location
        publishTripState(TripState.StartLocation(location))
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


    fun mergeTrips(
        tripA: Trip,
        tripB: Trip,
    ): Int {
        val status = 1
        val latLngList = mutableListOf<LatLng>()

        val tripALastLoc: LatLng
        val tripBFirstLoc: LatLng
        val gap = FloatArray(3)

        if (tripA.kml == null || tripB.kml == null) {
            return KML_NOT_FOUND
        }

        if (!Utils.isFileExists(tripA.kml) || !Utils.isFileExists(tripB.kml)) {
            return KML_NOT_FOUND
        }

        val tripALocations = kmlManager.getLocationsFromKml(tripA.kml)
        val tripBLocations = kmlManager.getLocationsFromKml(tripB.kml)

        latLngList.addAll(tripALocations)
        latLngList.addAll(tripBLocations)

        tripALastLoc = tripALocations.last()
        tripBFirstLoc = tripBLocations.first()

        Location.distanceBetween(
            tripALastLoc.latitude,
            tripALastLoc.longitude, tripBFirstLoc.latitude,
            tripBFirstLoc.longitude, gap
        )

        if (gap[0] > CONTINUE_TRIPS_GAP) {
            return UNABLE_TO_MERGE
        }
        val mapFile = kmlManager.generateKmlFromLocationList(latLngList)
        val tripsDuration = tripA.duration + tripB.duration
        val tripsDistance = tripA.distance + tripB.distance
        val averageSpeed = (tripsDistance / (tripsDuration / 1000).toInt()).toDouble() // m/sec
        val sdf = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm")
        val date = sdf.format(Date(System.currentTimeMillis()))

        val maxSpeed = max(tripA.maxSpeed, tripB.maxSpeed)
        val maxAlt = max(tripA.maxAlt, tripB.maxAlt)
        val tripsStopTime = tripA.stopTime + tripB.stopTime
        val tripsMoveTime = tripsDuration - tripsStopTime
        val averageMoveSpeed = (tripsDistance / (tripsMoveTime / 1000)).toDouble() //m/Sec

        val stopLocation = tripBLocations.last()
        val startAddress = startLocation?.let { getAddress(LatLng(it.latitude, it.longitude)) }
        val stopAddress = stopLocation.let { getAddress(LatLng(it.latitude, it.longitude)) }

        val trip = Trip(
            kml = mapFile,
            date = date,
            distance = tripsDistance,
            averageSpeed = averageSpeed,
            moveAverageSpeed = averageMoveSpeed,
            duration = tripsDuration,
            maxSpeed = maxSpeed,
            maxAlt = maxAlt,
            stopTime = tripsStopTime,
            moveTime = tripsMoveTime,
            startAddress = startAddress,
            stopAddress = stopAddress
        )

        val newTripMarkers = ArrayList<ImageMarker>()
        datasource.apply {
            newTripMarkers.addAll(findAllMarkersForTrip(tripA.id))
            newTripMarkers.addAll(findAllMarkersForTrip(tripB.id))
            val insertId = create(trip)
            insertImageMarkers(newTripMarkers, insertId.toDouble())
        }
        return MERGE_SUCCESS
    }

    private fun getAddress(location: LatLng) = try {
        geocoder.getFromLocation(location.latitude, location.longitude, 1)?.let {
            if (it.isNotEmpty()) { // list maybe not null but still with size of 0!
                val returnedAddress: Address = it.first()
                val strReturnedAddress = StringBuilder("")

                for (i in 0 until returnedAddress.maxAddressLineIndex) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n")
                }
                strReturnedAddress.toString()
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }


    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (recordingState == RecordingState.Recording) {
            publishTripState(TripState.ShowRecordingInBackground)
        }
    }

    private fun publishTripState(state: TripState) {
        CoroutineScope(Dispatchers.Main).launch {
            tripMutableStateFlow.value = state
        }
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