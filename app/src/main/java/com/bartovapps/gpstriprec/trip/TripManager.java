//package com.bartovapps.gpstriprec.trip;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.content.Context;
//import android.graphics.Color;
//import android.location.Address;
//import android.location.Geocoder;
//import android.location.Location;
//import android.net.Uri;
//import android.os.Environment;
//import android.util.Log;
//
//import com.bartovapps.gpstriprec.db.TripsDataSource;
//import com.bartovapps.gpstriprec.enums.MovementState;
//import com.bartovapps.gpstriprec.enums.SaveStatus;
//import com.bartovapps.gpstriprec.kmlhleper.com.bartovapps.gpstriprec.core.kml.KmlManager;
//import com.bartovapps.gpstriprec.kmlhleper.KmlParser;
//import com.bartovapps.gpstriprec.maphelper.ImageMarker;
//import com.bartovapps.gpstriprec.maphelper.com.bartovapps.gpstriprec.core.map_helper.MapHelper;
//import com.bartovapps.gpstriprec.timer.com.bartovapps.gpstriprec.core.timer.TimerManager;
//import com.bartovapps.gpstriprec.utils.Utils;
//import com.google.android.gms.maps.model.LatLng;
//
//import java.sql.Date;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//
//public class TripManager {
//
//    public static final int MERGE_SUCCESS = 1;
//    public static final int KML_NOT_FOUND = 2;
//    public static final int UNABLE_TO_MERGE = 3;
//
//    private static final int CONTINUE_TRIPS_GAP = 300;
//
//    private static final String TAG = "TAG_TripManager";
//
//    private Location startLocation;
//    /**
//     * The current location
//     */
//    private Location currentLocation;
//    /**
//     * The calculated distance
//     */
//    Context context;
//    private float distance = 0;
//    private float portionLength[] = new float[3];
//    private float ACCURACY = 25;
//    private final float ACCURACY_TOLLERANCE = (float) 0.1;
//    private float heading;
//    private double newLat = 0.0;
//    private double newLng = 0.0;
//    private double speed = 0.0;
//    private float accuracy = 0;
//    private static double SPEED_FILTER = 1.5;
//    private com.bartovapps.gpstriprec.core.map_helper.MapHelper mapHelper;
//    private ArrayList<String> locations = new ArrayList<String>();
//    private ArrayList<LatLng> latLngList = new ArrayList<>();
//    private StringBuilder markplace;
//    private TripsDataSource datasource;
//    private double averageSpeed = 0;
//    private double averageMoveSpeed = 0;
//    private double maxSpeed = 0;
//    private double altitude = 0;
//    private double maxAltitude = 0;
//    private com.bartovapps.gpstriprec.core.timer.TimerManager timer;
//    private long stopTime = 0;
//    private long overallStopTime = 0;
//    private long movementTime = 0;
//    private long duration = 0;
//    private String startAddress = "none";
//    private String stopAddress = "none";
//    private MovementState moveState = null;
//    static List<Address> list;
//    private Trip uploadedTrip;
//    private List<ImageMarker> imageMarkers;
//    public static final String TRIPS_DIR = "/GpsRecorder/trips";
//    public static final String MAP_IMAGES_DIR = "/mapImages";
//
//    public TripManager(Context context, float accuracy, double speedFilter,
//                       com.bartovapps.gpstriprec.core.map_helper.MapHelper helper, TripsDataSource datasource, com.bartovapps.gpstriprec.core.timer.TimerManager timer) {
//        this.ACCURACY = accuracy;
//        SPEED_FILTER = speedFilter;
//        this.context = context;
//        this.mapHelper = helper;
//        this.markplace = new StringBuilder();
//        this.datasource = datasource;
//        this.timer = timer;
//        imageMarkers = new ArrayList<>();
//        resetRoute(true);
//        moveState = null;
//    }
//
//    /**
//     * @param newLocation update the current with the new location and calculate the
//     *                    distance between them
//     */
//    public void updateLocation(Location newLocation) {
//        Log.i(TAG, "New Location accuracy: " + newLocation.getAccuracy() + ", speed: " + newLocation.getSpeed() + ", hasSpeed: " + newLocation.hasSpeed());
//        // Toast.makeText(context, "speed: " + newLocation.getSpeed() +
//        // ", accuracy: " + newLocation.getAccuracy(),
//        // Toast.LENGTH_SHORT).show();
//        this.newLat = newLocation.getLatitude();
//        this.newLng = newLocation.getLongitude();
//
//        markplace.replace(0, markplace.length(), "" + this.newLng + ","
//                + this.newLat);
//
////        Log.i(LOG_TAG, "new location accuracy " + newLocation.getAccuracy());
//
//        if (this.startLocation == null) { // taking care the first location...
//            if (newLocation.getAccuracy() < (ACCURACY + ACCURACY
//                    * ACCURACY_TOLLERANCE)) { // for first location accuracy is
//                // less important, no speed
//                // required.
//                this.startLocation = newLocation;
//                this.currentLocation = newLocation;
//                mapHelper.goToLocation(newLocation);
//                locations.add(markplace.toString());
//                latLngList.add(new LatLng(this.newLat, this.newLng));
//            }
//        } else { // all other locations
//            // only locations that has speed, bearing, and bigger than the
//            // speed_filter and accurate will be taken! all other will be
//            // avoided!
////            if ((newLocation.getAccuracy() < ACCURACY)
////                    && (newLocation.hasSpeed() && (newLocation.getSpeed() >= 0))
////                    && (newLocation.hasBearing())) {
//            if ((newLocation.getAccuracy() < ACCURACY)) {
////				Log.i(LOG_TAG,
////						"New location on movement.. updating current location with new location");
//                locations.add(markplace.toString());
//                latLngList.add(new LatLng(this.newLat, this.newLng));
//
//                Location.distanceBetween(currentLocation.getLatitude(),
//                        currentLocation.getLongitude(), this.newLat,
//                        this.newLng, portionLength);
//
//                this.distance += portionLength[0];
//                this.currentLocation = newLocation;
//                this.speed = newLocation.getSpeed();
//                mapHelper.goToLocation(newLocation);
//                if (speed > maxSpeed) {
//                    maxSpeed = speed;
//                }
//
//                if (newLocation.hasAltitude()) {
//                    this.altitude = newLocation.getAltitude();
//                    if (this.altitude > this.maxAltitude) {
//                        this.maxAltitude = this.altitude;
//                    }
//                }
//
//                if (moveState == null) {
//                    moveState = MovementState.Moving;
////					Toast.makeText(context, "Moving...", Toast.LENGTH_LONG)
////							.show();
//                } else if (moveState == MovementState.Stopped) {
//                    moveState = MovementState.Moving;
//                    stopTime = System.currentTimeMillis() - stopTime;
//                    overallStopTime += stopTime;
////                    Toast.makeText(activity, "Stopped for " + (int) (stopTime / 1000) + " Seconds", Toast.LENGTH_SHORT)
////                            .show();
////                    Toast.makeText(activity, "Overall stop time: " + (int) (overallStopTime / 1000) + " Seconds", Toast.LENGTH_LONG).show();
//                    mapHelper.mapCameraCloseup();
//                }
//            }
////			Log.i(LOG_TAG, "distance: " + this.distance + " M");
//            updateRouteStatus(newLocation);
//
//        }
//    }
//
//    /**
//     * Initialize the measured distance to 0.0
//     */
//    public void resetRoute(boolean resetMap) {
//        this.distance = 0;
//        this.speed = 0;
//        this.altitude = 0;
//        this.maxAltitude = 0;
//        this.startLocation = null;
//        this.currentLocation = null;
//        this.maxSpeed = 0;
//        this.moveState = null;
//        this.overallStopTime = 0;
//        this.maxAltitude = 0;
//        this.movementTime = 0;
//        if (resetMap) {
//            mapHelper.clearEverything();
//        }
//        this.locations.clear();
//        if (this.latLngList != null) {
//            this.latLngList.clear();
//        } else {
//            this.latLngList = new ArrayList<>();
//        }
//        imageMarkers.clear();
//    }
//
//    public void updateRouteStatus(Location location) {
//
//        if (location.hasBearing()) {
//            heading = location.getBearing();
//        } else {
////            Log.i(LOG_TAG, "Location has no bearing..");
//        }
//
//        if ((!location.hasSpeed() || location.getSpeed() == 0) && !location.hasBearing()) { //this means that we stopped!!
//            this.speed = 0;
//            if (moveState == MovementState.Moving) {
//                moveState = MovementState.Stopped;
//                stopTime = System.currentTimeMillis();
////				Toast.makeText(context, "Stopped...", Toast.LENGTH_LONG).show();
//                mapHelper.mapCameraLongshot();
//                mapHelper.goToLocation(currentLocation);
//                speed = location.getSpeed(); // m/sec
////                Log.i(LOG_TAG, "Speed: " + speed + "mSec");
//            }
//        } else {
//            this.speed = location.getSpeed();
//        }
//    }
//
//    /**
//     * @return return the route current speed
//     */
//    public double getSpeed() {
//        return this.speed;
//    }
//
//    /**
//     * @return the route current heading direction
//     */
//    public double getHeading() {
//        return heading;
//    }
//
//    /**
//     * @return returns the distance that passed so far in Meters.
//     */
//    public float getDistance() {
//        if (currentLocation == null) {
//            return 0;
//        }
//        return this.distance;
//    }
//
//    /**
//     * @return the current location latitude
//     */
//    public double getLatitude() {
//        return this.newLat;
//    }
//
//    /**
//     * @return the current location longitude
//     */
//    public double getLongitude() {
//        return this.newLng;
//    }
//
//    /**
//     * @return the current accuracy
//     */
//    public double getAccuracy() {
//        return this.accuracy;
//    }
//
//
//    /**
//     * @return the current altitude
//     */
//    public double getAltitude() {
//        return this.altitude;
//    }
//
//    @SuppressLint("SimpleDateFormat")
//    public SaveStatus saveTrip() {
//
//        if (latLngList.size() > 1) {
//            mapHelper.viewRoute(latLngList);
//            com.bartovapps.gpstriprec.core.kml.KmlManager kmlHelper = new com.bartovapps.gpstriprec.core.kml.KmlManager(context);
//            kmlHelper.openRawDocument();
//            long timestamp = System.currentTimeMillis();
//            String mapImageFile = context.getExternalFilesDir(null) + MAP_IMAGES_DIR + "/" + "trip_" + timestamp + ".jpeg";
//            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm");
//
//            String date = sdf.format(new Date(System.currentTimeMillis()));
//
//            String mapFile = kmlHelper.updateTripLatLng(latLngList); // creating and
//            // saving the
//            // trip kml file
//            // to external
//            // storage!
////			startAddress = getAddress(new LatLng(startLocation.getLatitude(),
////					startLocation.getLongitude()), context);
////			stopAddress = getAddress(new LatLng(currentLocation.getLatitude(),
////					currentLocation.getLongitude()), context);
////			Log.i(LOG_TAG, "End address: " + stopAddress);
//
//            duration = timer.getTimeMillis(); //mSec
//            averageSpeed = distance / (int) (duration / 1000); // m/sec
//
//            movementTime = duration - overallStopTime; //mSec
//            averageMoveSpeed = distance / (int) (movementTime / 1000); //m/sec
//
//
//            //Toast.makeText(activity.getApplicationContext(), "Movement Time: " + movementTime + " Sec", Toast.LENGTH_LONG).show();
//
//
//            Trip trip = new Trip(mapFile, date, distance, averageSpeed);
//            trip.setDuration(duration);
//            trip.setMoveTime(movementTime);
//            trip.setStopTime(overallStopTime);
//            trip.setStartAddress(startAddress);
//            trip.setStopAddress(stopAddress);
//            trip.setMaxSpeed(maxSpeed);
//            trip.setImageFileName(mapImageFile);
//            trip.setMaxAlt(this.maxAltitude);
//            trip.setMove_average_speed(this.averageMoveSpeed);
//
//            datasource.open();
//
//            trip.setId(datasource.create(trip).getId());
//            if (imageMarkers != null) {
//                if (imageMarkers.size() > 0) {
//                    datasource.insertImageMarkers(imageMarkers, trip.getId());
//                }
//            }
//            datasource.close();
//
////            Log.i(LOG_TAG, "Trip file " + mapFile + " saved..");
//            mapHelper.saveMapAsImage(mapImageFile);
//
//            return SaveStatus.PASSED;
//        } else {
//            return SaveStatus.NOT_ENOUGH_DATA;
//        }
//    }
//
//    public static String getAddress(LatLng location, Context cntxt) {
//        String address = null;
//        Geocoder gc = new Geocoder(cntxt);
//        if (Utils.isNetworkAvailable(cntxt)) {
//
//            try {
//                list = gc.getFromLocation(location.latitude,
//                        location.longitude, 1);
//                if (list != null && (list.size() > 0)) { // list maybe not null
//                    // but still with
//                    // size of 0!
//                    Address returnedAddress = list.get(0);
//                    StringBuilder strReturnedAddress = new StringBuilder("");
//
//                    for (int i = 0; i < returnedAddress
//                            .getMaxAddressLineIndex(); i++) {
//                        strReturnedAddress.append(
//                                returnedAddress.getAddressLine(i)).append("\n");
//                    }
//                    address = strReturnedAddress.toString();
//                    // Log.w("My Current location address", "" +
//                    // strReturnedAddress.toString());
//                } else {
//                    // Log.w("My Current location address",
//                    // "No Address returned!");
//                    address = "Unavailable";
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                address = "Unavailable";
//            } finally {
//                if (list != null) {
//                    list.clear();
//                }
//            }
//        } else {
//            address = "Unavailable";
//        }
//
//        return address;
//    }
//
//    public static int mergeTrips(Trip tripA, Trip tripB, Activity activity, TripsDataSource datasource) {
//        int status = 1;
//        ArrayList<LatLng> latLngList = new ArrayList<LatLng>();
//
//        LatLng tripALastLoc;
//        LatLng TripBFirstLoc;
//        float[] gap = new float[3];
//
//        if (!Utils.isFileExists(tripA.getKml()) || !Utils.isFileExists(tripB.getKml())) {
//            return KML_NOT_FOUND;
//        }
//
//        KmlParser tripAParser = new KmlParser(tripA.getKml());
//        KmlParser tripBParser = new KmlParser(tripB.getKml());
//        tripAParser.openTripKml();
//        tripBParser.openTripKml();
//
//        latLngList.addAll(tripAParser.getTripLocations());
//        latLngList.addAll(tripBParser.getTripLocations());
//
//        tripALastLoc = tripAParser.getLastLocation();
//        TripBFirstLoc = tripBParser.getfirstLocation();
//        tripAParser.closeKml();
//        tripBParser.closeKml();
//
//        Location.distanceBetween(tripALastLoc.latitude,
//                tripALastLoc.longitude, TripBFirstLoc.latitude,
//                TripBFirstLoc.longitude, gap);
//
//        if (gap[0] > CONTINUE_TRIPS_GAP) {
//            return TripManager.UNABLE_TO_MERGE;
//        }
//
//
//        com.bartovapps.gpstriprec.core.kml.KmlManager kmlCreator = new com.bartovapps.gpstriprec.core.kml.KmlManager(activity);
//        kmlCreator.openRawDocument();
//
//        String mapFile = kmlCreator.updateTripLatLng(latLngList);
//
//        tripAParser.getLastLocation();
//        long tripsDuration = tripA.getDuration() + tripB.getDuration();
//        float tripsDistance = tripA.getDistance() + tripB.getDistance();
//        double averageSpeed = tripsDistance / (int) (tripsDuration / 1000); // m/sec
//        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm");
//        String date = sdf.format(new Date(System.currentTimeMillis()));
//
//        double maxSpeed = Math.max(tripA.getMaxSpeed(), tripB.getMaxSpeed());
//        double maxAlt = Math.max(tripA.getMaxAlt(), tripB.getMaxAlt());
//        long tripsStopTime = tripA.getStopTime() + tripB.getStopTime();
//        long tripsMoveTime = tripsDuration - tripsStopTime;
//        double averageMoveSpeed = tripsDistance / (tripsMoveTime / 1000); //m/Sec
//
//        Trip trip = new Trip(mapFile, date, tripsDistance, averageSpeed);
//        trip.setDuration(tripsDuration);
//        trip.setMaxSpeed(maxSpeed);
//        trip.setMaxAlt(maxAlt);
//        trip.setStopTime(tripsStopTime);
//        trip.setMoveTime(tripsMoveTime);
//        trip.setMove_average_speed(averageMoveSpeed);
//        ArrayList<ImageMarker> newTripMarkers = new ArrayList<>();
//
//
//        datasource.open();
//        trip = datasource.create(trip);
//        newTripMarkers.addAll(datasource.findAllMarkersForTrip(tripA.getId()));
//        newTripMarkers.addAll(datasource.findAllMarkersForTrip(tripB.getId()));
////        Log.i(LOG_TAG, "Got " + newTripMarkers.size() + " for new trip");
//
//
//        datasource.insertImageMarkers(newTripMarkers, trip.getId());
//        datasource.close();
//
//        return TripManager.MERGE_SUCCESS;
//
//    }
//
//    public int uploadTrip(Trip trip) {
//        int kml_status = KmlParser.KML_OPENED;
//        this.uploadedTrip = trip;
//        this.distance = this.uploadedTrip.getDistance();
//        this.maxSpeed = this.uploadedTrip.getMaxSpeed();
//        this.duration = this.uploadedTrip.getDuration();
//        this.movementTime = this.uploadedTrip.getMoveTime();
//        this.overallStopTime = this.uploadedTrip.getStopTime();
//        this.timer.setStartTime(this.uploadedTrip.getDuration());
//
////        KmlParser parser = new KmlParser(this.uploadedTrip.getKml());
////        kml_status = parser.openTripKml();
////        if(kml_status != KmlParser.KML_OPENED){
////            return kml_status;
////        }
//
//        this.latLngList.clear();
//        this.latLngList = KmlParser.getLocationsFromKml(trip.getKml());
//
//        if (this.latLngList == null) {
//            kml_status = KmlParser.FAIL_TO_OPEN_KML;
//            return kml_status;
//        }
//
//        datasource.open();
//        imageMarkers = datasource.findAllMarkersForTrip(trip.getId());
//        datasource.close();
//        mapHelper.clearEverything();
//        mapHelper.overlayRoute(latLngList, 10, Color.CYAN);
//        for (ImageMarker marker : imageMarkers) {
//            mapHelper.addImageMarker(marker, context);
//        }
//        mapHelper.viewRoute(latLngList);
//        return kml_status;
//    }
//
//    public void addImageMarker(Uri markerUri) throws Exception {
//        ImageMarker imageMarker = new ImageMarker(markerUri, currentLocation.getLatitude(), currentLocation.getLongitude());
//        imageMarkers.add(imageMarker);
//        mapHelper.addImageMarker(imageMarker, context);
////        Log.i(LOG_TAG, imageMarkers.size() + " ImageMarkers for this trip");
//    }
//
//    public void addImageMarker(Uri markerUri, Location location) {
//        ImageMarker imageMarker = new ImageMarker(markerUri, location.getLatitude(), location.getLongitude());
//        imageMarkers.add(imageMarker);
//        mapHelper.addImageMarker(imageMarker, context);
////        Log.i(LOG_TAG, imageMarkers.size() + " ImageMarkers for this trip");
//    }
//
//    public void setCurrentLocation(Location newLocation) {
//        currentLocation = newLocation;
//    }
//}
