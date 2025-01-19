package com.bartovapps.gpstriprec.db;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.bartovapps.gpstriprec.maphelper.ImageMarker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import data.model.Trip;


public class TripsDataSource {
    public static final String LOG_TAG = TripsDataSource.class.getSimpleName();

    SQLiteOpenHelper dbhelper;
    SQLiteDatabase database;

    private static final String[] allColumns = {
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
            TripsDBOpenHelper.COLUMN_STOP_TIME};


    private static final String[] markersColumns = {
            TripsDBOpenHelper.COLUMN_MARKER_ID,
            TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID,
            TripsDBOpenHelper.COLUMN_MARKER_LATITUDE,
            TripsDBOpenHelper.COLUMN_MARKER_LONGITUDE,
            TripsDBOpenHelper.COLUMN_MARKER_URI,
            TripsDBOpenHelper.COLUMN_MARKER_SNIPPET
    };


    public TripsDataSource(Activity activity) {
        dbhelper = new TripsDBOpenHelper(activity.getApplicationContext());
    }

    public void open() {
//		Log.i(LOG_TAG, "Database opened");
        database = dbhelper.getWritableDatabase();
    }

    public void close() {
//		Log.i(LOG_TAG, "Database closed");		
        dbhelper.close();
    }

    public long create(Trip trip) {
        ContentValues values = new ContentValues();
        values.put(TripsDBOpenHelper.COLUMN_DATE, trip.getDate());
        values.put(TripsDBOpenHelper.COLUMN_DURATION, trip.getDuration());
        values.put(TripsDBOpenHelper.COLUMN_DIST, trip.getDistance());
        values.put(TripsDBOpenHelper.COLUMN_SPEED, trip.getAverageSpeed());
        values.put(TripsDBOpenHelper.COLUMN_FROM, trip.getStartAddress());
        values.put(TripsDBOpenHelper.COLUMN_TO, trip.getStopAddress());
        values.put(TripsDBOpenHelper.COLUMN_MAP, trip.getKml());
        values.put(TripsDBOpenHelper.COLUMN_MAX_SPEED, trip.getMaxSpeed());
        values.put(TripsDBOpenHelper.COLUMN_MAX_ALT, trip.getMaxAlt());
        values.put(TripsDBOpenHelper.COLUMN_NAME, trip.getTripName());
        values.put(TripsDBOpenHelper.COLUMN_MAP_IMAGE, trip.getImageFileName());
        values.put(TripsDBOpenHelper.COLUMN_MOVE_SPEED, trip.getMoveAverageSpeed());
        values.put(TripsDBOpenHelper.COLUMN_MOVE_TIME, trip.getMoveTime());
        values.put(TripsDBOpenHelper.COLUMN_STOP_TIME, trip.getStopTime());
        long insertId = database.insert(TripsDBOpenHelper.TABLE_TRIPS, null, values);

        return insertId;
    }

    public List<Trip> findAll() {
        List<Trip> trips = new ArrayList<>();
        try (Cursor cursor = database.query(TripsDBOpenHelper.TABLE_TRIPS, allColumns,
                null, null, null, null, TripsDBOpenHelper.COLUMN_ID + " DESC");
        ) {
            if (cursor.getCount() > 0) {

                while (cursor.moveToNext()) {
                    Trip trip = new Trip(cursor.getLong(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_ID)),
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
                    );
                    trips.add(trip);
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            return trips;
        }

        return trips;
    }

    public boolean removeSavedTrip(Trip trip) {
        String where = TripsDBOpenHelper.COLUMN_ID + "=" + trip.getId();
        int result = database.delete(TripsDBOpenHelper.TABLE_TRIPS, where, null);

        if (trip.getKml() != null) {
            File mapFile = new File(trip.getKml());
            if (mapFile.exists()) {
                mapFile.delete();
            }
        }

        return (result == 1);
    }

    public boolean updateTripTitle(Trip trip, String title) {
        ContentValues args = new ContentValues();
        String where = TripsDBOpenHelper.COLUMN_ID + "=" + trip.getId();
        args.put(TripsDBOpenHelper.COLUMN_NAME, title);
        return database.update(TripsDBOpenHelper.TABLE_TRIPS, args, where, null) > 0;
    }

    public boolean updateTripData(long tripId, String column, String data) {
        ContentValues args = new ContentValues();
        String where = TripsDBOpenHelper.COLUMN_ID + "=" + tripId;
        args.put(column, data);
        return database.update(TripsDBOpenHelper.TABLE_TRIPS, args, where, null) > 0;
    }

    private void recoverDb() {
        dbhelper.onCreate(database);
    }

    public List<ImageMarker> insertImageMarkers(List<ImageMarker> markers, double tripId) {
        ContentValues values = new ContentValues();

        for (ImageMarker marker : markers) {
            values.put(TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID, tripId);
            values.put(TripsDBOpenHelper.COLUMN_MARKER_LATITUDE, marker.getLatitude());
            values.put(TripsDBOpenHelper.COLUMN_MARKER_LONGITUDE, marker.getLongitude());
            values.put(TripsDBOpenHelper.COLUMN_MARKER_URI, marker.getImageUri().toString());
            long insertId = database.insert(TripsDBOpenHelper.TABLE_MARKERS, null, values);
            values.clear();
        }
        Log.i(LOG_TAG, markers.size() + " marker was inserted to database");

        return markers;
    }


    public List<ImageMarker> findAllMarkersForTrip(long tripId) {
        List<ImageMarker> markers = new ArrayList<>();

        try (Cursor cursor = database.query(TripsDBOpenHelper.TABLE_MARKERS, markersColumns,
                TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID + " = ?", new String[]{"" + tripId}, null, null, TripsDBOpenHelper.COLUMN_MARKER_ID);
        ) {
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    ImageMarker marker = new ImageMarker();
                    marker.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MARKER_LATITUDE)));
                    marker.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MARKER_LONGITUDE)));
                    marker.setImageUri(Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MARKER_URI))));
                    markers.add(marker);
//				Log.i(LOG_TAG, "data.model.Trip add: " + trip.toString());
                }
            }
        } catch (SQLiteException e) {
            return markers;
        }


        return markers;
    }

    public List<Uri> findAllMarkersUrisForTrip(long tripId) {
        List<Uri> uris = new ArrayList<>();

        try (Cursor cursor = database.query(TripsDBOpenHelper.TABLE_MARKERS, markersColumns,
                TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID + " = ?", new String[]{"" + tripId}, null, null, TripsDBOpenHelper.COLUMN_MARKER_ID)) {
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(TripsDBOpenHelper.COLUMN_MARKER_URI)));
                    uris.add(uri);
                }
            }
        } catch (SQLiteException e) {
            return uris;
        }
        return uris;
    }

    public int deleteMarkersForTrip(double tripId) {
        String where = TripsDBOpenHelper.COLUMN_MARKER_TRIP_ID + "=" + tripId;
        return database.delete(TripsDBOpenHelper.TABLE_MARKERS, where, null);
    }
}
