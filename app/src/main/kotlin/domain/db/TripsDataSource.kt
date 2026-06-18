package com.dunihuliapps.myglidingassistnat.domain.db
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log
import com.dunihuliapps.myglidingassistnat.domain.map_helper.ImageMarker
import data.model.Flight
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripsDataSource @Inject constructor(dbhelper: TripsDBOpenHelper) {
    private val database: SQLiteDatabase = dbhelper.writableDatabase

    fun create(flight: Flight): Long {
        val values = ContentValues()
        values.put(TripsDBOpenHelper.COLUMN_DATE, flight.date)
        values.put(TripsDBOpenHelper.COLUMN_DURATION, flight.duration)
        values.put(TripsDBOpenHelper.COLUMN_DIST, flight.distance)
        values.put(TripsDBOpenHelper.COLUMN_SPEED, flight.averageSpeed)
        values.put(TripsDBOpenHelper.COLUMN_FROM, flight.startAddress)
        values.put(TripsDBOpenHelper.COLUMN_TO, flight.stopAddress)
        values.put(TripsDBOpenHelper.COLUMN_MAP, flight.kml)
        values.put(TripsDBOpenHelper.COLUMN_MAX_SPEED, flight.maxSpeed)
        values.put(TripsDBOpenHelper.COLUMN_MAX_ALT, flight.maxAlt)
        values.put(TripsDBOpenHelper.COLUMN_NAME, flight.tripName)
        values.put(TripsDBOpenHelper.COLUMN_MAP_IMAGE, flight.imageFileName)
        values.put(TripsDBOpenHelper.COLUMN_MOVE_SPEED, flight.moveAverageSpeed)
        values.put(TripsDBOpenHelper.COLUMN_MOVE_TIME, flight.moveTime)
        values.put(TripsDBOpenHelper.COLUMN_STOP_TIME, flight.stopTime)
        val insertId = database.insert(TripsDBOpenHelper.TABLE_TRIPS, null, values)

        return insertId
    }

    fun findTripById(tripId: Long): Flight? {
        return database.query(
            TripsDBOpenHelper.TABLE_TRIPS,
            allColumns,
            TripsDBOpenHelper.COLUMN_ID + " = ? ",
            arrayOf("$tripId"),
            null,
            null,
            TripsDBOpenHelper.COLUMN_ID + " DESC",
            "1"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                Flight(
                    cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_DATE)),
                    cursor.getFloat(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_DIST)),
                    cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MAP)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_DURATION)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MOVE_TIME)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_STOP_TIME)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_SPEED)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MOVE_SPEED)),
                    cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_FROM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_TO)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MAX_SPEED)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MAX_ALT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MAP_IMAGE))
                )
            } else {
                null
            }
        }
    }

    fun findAll(): List<Flight> {
        val flights: MutableList<Flight> = ArrayList()
        try {
            database.query(
                TripsDBOpenHelper.TABLE_TRIPS, allColumns,
                null, null, null, null, TripsDBOpenHelper.COLUMN_ID + " DESC"
            ).use { cursor ->
                if (cursor.count > 0) {
                    while (cursor.moveToNext()) {
                        val flight = Flight(
                            cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_DATE)),
                            cursor.getFloat(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_DIST)),
                            cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MAP)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_DURATION)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MOVE_TIME)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_STOP_TIME)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_SPEED)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MOVE_SPEED)),
                            cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_FROM)),
                            cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_TO)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MAX_SPEED)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MAX_ALT)),
                            cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MAP_IMAGE))
                        )
                        flights.add(flight)
                    }
                }
            }
        } catch (e: SQLiteException) {
            e.printStackTrace()
            return flights
        }

        return flights
    }

    fun removeSavedTrip(flight: Flight): Boolean {
        val where = TripsDBOpenHelper.COLUMN_ID + "=" + flight.id
        val result = database.delete(TripsDBOpenHelper.TABLE_TRIPS, where, null)

        if (flight.kml != null) {
            val mapFile = File(flight.kml)
            if (mapFile.exists()) {
                mapFile.delete()
            }
        }

        return (result == 1)
    }

    fun updateTripTitle(flight: Flight, title: String?): Boolean {
        val args = ContentValues()
        val where = TripsDBOpenHelper.COLUMN_ID + "=" + flight.id
        args.put(TripsDBOpenHelper.COLUMN_NAME, title)
        return database.update(TripsDBOpenHelper.TABLE_TRIPS, args, where, null) > 0
    }

    fun updateTripData(tripId: Long, column: String?, data: String?): Boolean {
        val args = ContentValues()
        val where = TripsDBOpenHelper.COLUMN_ID + "=" + tripId
        args.put(column, data)
        return database.update(TripsDBOpenHelper.TABLE_TRIPS, args, where, null) > 0
    }

    fun insertImageMarkers(markers: List<ImageMarker>, tripId: Double): List<ImageMarker> {
        val values = ContentValues()

        for (marker in markers) {
            values.put(TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID, tripId)
            values.put(TripsDBOpenHelper.COLUMN_MARKER_LATITUDE, marker.latitude)
            values.put(TripsDBOpenHelper.COLUMN_MARKER_LONGITUDE, marker.longitude)
            values.put(TripsDBOpenHelper.COLUMN_MARKER_URI, marker.imageUri.toString())
            val insertId = database.insert(TripsDBOpenHelper.TABLE_MARKERS, null, values)
            values.clear()
        }
        Log.i(LOG_TAG, markers.size.toString() + " marker was inserted to database")

        return markers
    }


    fun findAllMarkersForTrip(tripId: Long): List<ImageMarker> {
        val markers: MutableList<ImageMarker> = ArrayList()

        try {
            database.query(
                TripsDBOpenHelper.TABLE_MARKERS,
                markersColumns,
                TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID + " = ?",
                arrayOf("" + tripId),
                null,
                null,
                TripsDBOpenHelper.COLUMN_MARKER_ID
            ).use { cursor ->
                if (cursor.count > 0) {
                    while (cursor.moveToNext()) {
                        val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(
                            TripsDBOpenHelper.COLUMN_MARKER_LATITUDE))
                        val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(
                            TripsDBOpenHelper.COLUMN_MARKER_LONGITUDE))
                        val imageUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(
                            TripsDBOpenHelper.COLUMN_MARKER_URI)))
                        val marker = ImageMarker(latitude = latitude, longitude = longitude, imageUri = imageUri)
                        markers.add(marker)
                    }
                }
            }
        } catch (e: SQLiteException) {
            return markers
        }


        return markers
    }

    fun findAllMarkersUrisForTrip(tripId: Long): List<Uri> {
        val uris: MutableList<Uri> = ArrayList()

        try {
            database.query(
                TripsDBOpenHelper.TABLE_MARKERS,
                markersColumns,
                TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID + " = ?",
                arrayOf("" + tripId),
                null,
                null,
                TripsDBOpenHelper.COLUMN_MARKER_ID
            ).use { cursor ->
                if (cursor.count > 0) {
                    while (cursor.moveToNext()) {
                        val uri = Uri.parse(
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MARKER_URI)
                            )
                        )
                        uris.add(uri)
                    }
                }
            }
        } catch (e: SQLiteException) {
            return uris
        }
        return uris
    }

    fun deleteMarkersForTrip(tripId: Double): Int {
        val where = TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID + "=" + tripId
        return database.delete(TripsDBOpenHelper.TABLE_MARKERS, where, null)
    }

    companion object {
        val LOG_TAG: String = TripsDataSource::class.java.simpleName

        private val allColumns = arrayOf(
            TripsDBOpenHelper.COLUMN_ID,
            TripsDBOpenHelper.COLUMN_DATE,
            TripsDBOpenHelper.COLUMN_DURATION,
            TripsDBOpenHelper.COLUMN_DIST,
            TripsDBOpenHelper.COLUMN_SPEED,
            TripsDBOpenHelper.COLUMN_FROM,
            TripsDBOpenHelper.COLUMN_TO,
            TripsDBOpenHelper.COLUMN_MAP,
            TripsDBOpenHelper.COLUMN_MAX_SPEED,
            TripsDBOpenHelper.COLUMN_MAX_ALT,
            TripsDBOpenHelper.COLUMN_NAME,
            TripsDBOpenHelper.COLUMN_MAP_IMAGE,
            TripsDBOpenHelper.COLUMN_MOVE_SPEED,
            TripsDBOpenHelper.COLUMN_MOVE_TIME,
            TripsDBOpenHelper.COLUMN_STOP_TIME
        )


        private val markersColumns = arrayOf(
            TripsDBOpenHelper.COLUMN_MARKER_ID,
            TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID,
            TripsDBOpenHelper.COLUMN_MARKER_LATITUDE,
            TripsDBOpenHelper.COLUMN_MARKER_LONGITUDE,
            TripsDBOpenHelper.COLUMN_MARKER_URI,
            TripsDBOpenHelper.COLUMN_MARKER_SNIPPET
        )
    }
}