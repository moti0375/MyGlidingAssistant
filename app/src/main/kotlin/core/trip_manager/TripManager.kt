package core.trip_manager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.util.Log
import com.bartovapps.gpstriprec.core.db.TripsDataSource
import com.bartovapps.gpstriprec.core.kml.KmlManager
import com.bartovapps.gpstriprec.core.map_helper.ImageMarker
import com.bartovapps.gpstriprec.core.map_helper.MapHelper
import com.bartovapps.gpstriprec.core.timer.TripTimer
import com.bartovapps.gpstriprec.data.enums.MovementState
import com.bartovapps.gpstriprec.data.enums.SaveStatus
import com.bartovapps.gpstriprec.kmlhleper.KmlParser
import com.bartovapps.gpstriprec.kmlhleper.KmlParserImpl.Companion.FAIL_TO_OPEN_KML
import com.bartovapps.gpstriprec.kmlhleper.KmlParserImpl.Companion.KML_OPENED
import com.bartovapps.gpstriprec.utils.Utils
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import data.model.Trip
import java.sql.Date
import java.text.SimpleDateFormat
import javax.inject.Inject
import kotlin.math.max

interface TripManager {
    fun updateLocation(location: Location)
    fun resetRoute(resetMap: Boolean)
    fun updateRouteStatus(location: Location)
    fun saveTrip() : SaveStatus
    fun setCurrentLocation(location: Location)
    fun updateAccuracy(accuracy: Float)
    fun setSpeedFilter(speedFilter : Double)
    fun mergeTrips(tripA: Trip, tripB: Trip) : Int
    fun getAddress(location: LatLng) : String?
    fun uploadTrip(trip: Trip) : Int
    fun addImageMarker(capturedImageUri: Uri)
}


class TripManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapHelper: MapHelper,
    private val datasource: TripsDataSource,
    private val timer: TripTimer,
    private val kmlManager: KmlManager,
    private val kmlParser : KmlParser,
    private val geocoder: Geocoder
) : TripManager {
    private var startLocation: Location? = null

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

    init {
        resetRoute(true)
    }

    /**
     * @param newLocation update the current with the new location and calculate the
     * distance between them
     */
    override fun updateLocation(newLocation: Location) {
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
            if (newLocation.accuracy < (accuracy + accuracy * ACCURACY_TOLERANCE)) { // for first location accuracy is
                // less important, no speed
                // required.
                this.startLocation = newLocation
                this.currentLocation = newLocation
                mapHelper.goToLocation(newLocation)
                locations.add(markplace.toString())
                latLngList.add(LatLng(this.latitude, this.longitude))
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
                mapHelper.goToLocation(newLocation)
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
                    mapHelper.mapCameraCloseup()
                }
            }
            updateRouteStatus(newLocation)
        }
    }

    /**
     * Initialize the measured distance to 0.0
     */
    override fun resetRoute(resetMap: Boolean) {
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
            mapHelper.clearEverything()
        }
        locations.clear()
        latLngList.clear()
        imageMarkers.clear()
    }

    override fun updateRouteStatus(location: Location) {
        if (location.hasBearing()) {
            heading = location.bearing
        } else {
//            Log.i(LOG_TAG, "Location has no bearing..");
        }

        if ((!location.hasSpeed() || location.speed == 0f) && !location.hasBearing()) { //this means that we stopped!!
            this.speed = 0.0
            if (moveState == MovementState.Moving) {
                moveState = MovementState.Stopped
                stopTime = System.currentTimeMillis()
                mapHelper.mapCameraLongshot()
                currentLocation?.let {
                    mapHelper.goToLocation(it)
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
    override fun saveTrip(): SaveStatus {
        return if (latLngList.size > 1) {
            mapHelper.viewRoute(latLngList)
            kmlManager.openRawDocument()
            val timestamp = System.currentTimeMillis()
            val mapImageFile = context.getExternalFilesDir(null)
                .toString() + MAP_IMAGES_DIR + "/" + "trip_" + timestamp + ".jpeg"
            val sdf = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm")
            val date = sdf.format(Date(System.currentTimeMillis()))
            val mapFile = kmlManager.updateTripLatLng(latLngList) // creating and

            // saving the
            // trip kml file
            // to external
            // storage!
//			startAddress = getAddress(new LatLng(startLocation.getLatitude(),
//					startLocation.getLongitude()), context);
//			stopAddress = getAddress(new LatLng(currentLocation.getLatitude(),
//					currentLocation.getLongitude()), context);
//			Log.i(LOG_TAG, "End address: " + stopAddress);
            duration = timer.timeMillis //mSec
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

            datasource.open()
            val tripId = datasource.create(trip)
            if (imageMarkers.isNotEmpty()) {
                datasource.insertImageMarkers(imageMarkers, tripId.toDouble())
            }
            datasource.close()

            //            Log.i(LOG_TAG, "data.model.Trip file " + mapFile + " saved..");
            mapHelper.saveMapAsImage(mapImageFile)
             SaveStatus.PASSED
        } else {
             SaveStatus.NOT_ENOUGH_DATA
        }
    }

    override fun uploadTrip(trip: Trip): Int {
        var kmlStatus = KML_OPENED
        this.uploadedTrip = trip.also {
            this.distance = it.distance
            this.maxSpeed = it.maxSpeed
            this.duration = it.duration
            this.movementTime = it.moveTime
            this.overallStopTime = it.stopTime
            timer.setStartTime(it.duration)
        }


        //        KmlParser parser = new KmlParser(this.uploadedTrip.getKml());
//        kml_status = parser.openTripKml();
//        if(kml_status != KmlParser.KML_OPENED){
//            return kml_status;
//        }
        latLngList.clear()
        trip.kml?.let {
            this.latLngList.addAll(kmlParser.parsKmlString(trip.kml))
        }

        if (this.latLngList.isEmpty()) {
            kmlStatus = FAIL_TO_OPEN_KML
            return kmlStatus
        }

        datasource.open()
        imageMarkers.clear()
        imageMarkers.addAll(datasource.findAllMarkersForTrip(trip.id))
        datasource.close()

        mapHelper.clearEverything()
        mapHelper.overlayRoute(latLngList, 10f, Color.CYAN)
        for (marker in imageMarkers) {
            mapHelper.addImageMarker(marker, context)
        }
        mapHelper.viewRoute(latLngList)
        return kmlStatus
    }

    @Throws(Exception::class)
    override fun addImageMarker(markerUri: Uri) {
        val imageMarker =
            ImageMarker(markerUri, currentLocation!!.latitude, currentLocation!!.longitude)
        imageMarkers.add(imageMarker)
        mapHelper.addImageMarker(imageMarker, context)
        //        Log.i(LOG_TAG, imageMarkers.size() + " ImageMarkers for this trip");
    }

    fun addImageMarker(markerUri: Uri?, location: Location) {
        val imageMarker = ImageMarker(markerUri, location.latitude, location.longitude)
        imageMarkers!!.add(imageMarker)
        mapHelper.addImageMarker(imageMarker, context)
        //        Log.i(LOG_TAG, imageMarkers.size() + " ImageMarkers for this trip");
    }

    override fun setCurrentLocation(location: Location) {
        currentLocation = location
    }

    override fun updateAccuracy(accuracy: Float) {
        this.accuracy = accuracy
    }

    override fun setSpeedFilter(speedFilter: Double) {
        TODO("Not yet implemented")
    }


    override fun mergeTrips(
        tripA: Trip,
        tripB: Trip,
    ): Int {
        val status = 1
        val latLngList = mutableListOf<LatLng>()

        val tripALastLoc: LatLng
        val TripBFirstLoc: LatLng
        val gap = FloatArray(3)

        if(tripA.kml == null || tripB.kml == null){
            return KML_NOT_FOUND
        }

        if (!Utils.isFileExists(tripA.kml) || !Utils.isFileExists(tripB.kml)) {
            return KML_NOT_FOUND
        }


        val tripALocations = kmlParser.parsKmlString(tripA.kml)
        val tripBLocations = kmlParser.parsKmlString(tripB.kml)

        latLngList.addAll(tripALocations)
        latLngList.addAll(tripBLocations)

        tripALastLoc = tripALocations.last()
        TripBFirstLoc = tripBLocations.first()

        Location.distanceBetween(
            tripALastLoc.latitude,
            tripALastLoc.longitude, TripBFirstLoc.latitude,
            TripBFirstLoc.longitude, gap
        )

        if (gap[0] > CONTINUE_TRIPS_GAP) {
            return UNABLE_TO_MERGE
        }

        kmlManager.openRawDocument()
        val mapFile = kmlManager.updateTripLatLng(latLngList)

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
            moveTime = tripsMoveTime
        )
        val newTripMarkers = ArrayList<ImageMarker>()
        datasource.apply {
            open()
            newTripMarkers.addAll(findAllMarkersForTrip(tripA.id))
            newTripMarkers.addAll(findAllMarkersForTrip(tripB.id))
            val insertId = create(trip)
            insertImageMarkers(newTripMarkers, insertId.toDouble())
            close()
        }
        return MERGE_SUCCESS
    }


    override fun getAddress(location: LatLng): String? {
        var address: String?
        try {
            geocoder.getFromLocation(
                location.latitude,
                location.longitude, 1
            )?.let {
                list.addAll(it)
            }

            if (list.isNotEmpty()) { // list maybe not null
                // but still with
                // size of 0!
                val returnedAddress: Address = list[0]
                val strReturnedAddress = StringBuilder("")

                for (i in 0 until returnedAddress
                    .maxAddressLineIndex) {
                    strReturnedAddress.append(
                        returnedAddress.getAddressLine(i)
                    ).append("\n")
                }
                address = strReturnedAddress.toString()
            } else {
                address = "Unavailable"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            address = "Unavailable"
        } finally {
            list.clear()
        }

        return address
    }


    companion object {
        private const val TAG = "TAG_TripManager"

        const val MERGE_SUCCESS: Int = 1
        const val KML_NOT_FOUND: Int = 2
        const val UNABLE_TO_MERGE: Int = 3
        const val ACCURACY_TOLERANCE = 0.1F
        const val DEFAULT_ACCURACY = 25.0F
        const val SPEED_FILTER: Float = 0.832f // 0.833 < 3km/h

        private const val CONTINUE_TRIPS_GAP = 300


        var list = mutableListOf<Address>()
        const val TRIPS_DIR: String = "/trips"
        const val MAP_IMAGES_DIR: String = "/mapImages"

    }
}