package com.bartovapps.gpstriprec.domain.db
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class TripsDBOpenHelper @Inject constructor(@ApplicationContext context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_TRIPS"
        db.execSQL(DROP_TABLE)

        //        Log.i(LOG_TAG, "Any previous table dropped");
        db.execSQL(TABLE_CREATE)
        //        Log.i(LOG_TAG, "Tours table has been created");
        db.execSQL(MARKERS_TABLE_CREATE) //Create markers table
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {

            val ADD_MAX_SPEED =
                "ALTER TABLE " + TABLE_TRIPS +
                        " ADD COLUMN " + COLUMN_MAX_SPEED + " NUMERIC "

            val ADD_MAX_ALT =
                "ALTER TABLE " + TABLE_TRIPS +
                        " ADD COLUMN " + COLUMN_MAX_ALT + " NUMERIC "

            val ADD_NAME =
                "ALTER TABLE " + TABLE_TRIPS +
                        " ADD COLUMN " + COLUMN_NAME + " NUMERIC "

            db.execSQL(ADD_MAX_SPEED)
            db.execSQL(ADD_MAX_ALT)
            db.execSQL(ADD_NAME)
        }

        if (oldVersion < 3) {
            val ADD_MAP_IMAGE =
                "ALTER TABLE " + TABLE_TRIPS +
                        " ADD COLUMN " + COLUMN_MAP_IMAGE + " TEXT "
            db.execSQL(ADD_MAP_IMAGE)


//			Log.i(LOG_TAG, "Database has been upgrade to version " + DATABASE_VERSION);
        }

        if (oldVersion < 4) {
            val ADD_COLUMN =
                "ALTER TABLE " + TABLE_TRIPS +
                        " ADD COLUMN " + COLUMN_MOVE_SPEED + " NUMERIC "
            db.execSQL(ADD_COLUMN)

        }

        if (oldVersion < 5) {
            val ADD_MOVE_TIME_COLUMN =
                "ALTER TABLE " + TABLE_TRIPS +
                        " ADD COLUMN " + COLUMN_MOVE_TIME + " NUMERIC "

            val ADD_STOP_TIME_COLUMN =
                "ALTER TABLE " + TABLE_TRIPS +
                        " ADD COLUMN " + COLUMN_STOP_TIME + " NUMERIC "

            db.execSQL(ADD_MOVE_TIME_COLUMN)
            db.execSQL(ADD_STOP_TIME_COLUMN)

        }


        if (oldVersion < 6) {
            db.execSQL(MARKERS_TABLE_CREATE) //Create markers table on older versions
            Log.i(LOG_TAG, "Markers table has been created")
        }
    }


    companion object {
        private val LOG_TAG: String = TripsDBOpenHelper::class.java.simpleName
        private const val DATABASE_NAME = "trips.db"
        private const val DATABASE_VERSION = 6

        const val TABLE_TRIPS: String = "trips"
        const val COLUMN_ID: String = "tripid"
        const val COLUMN_DATE: String = "DATE"
        const val COLUMN_DIST: String = "distance"
        const val COLUMN_SPEED: String = "ave_speed"
        const val COLUMN_DURATION: String = "duration"
        const val COLUMN_FROM: String = "start_address"
        const val COLUMN_TO: String = "stop_address"
        const val COLUMN_MAP: String = "map"
        const val COLUMN_MAX_SPEED: String = "max_speed"
        const val COLUMN_MAX_ALT: String = "max_altitude"
        const val COLUMN_NAME: String = "name"
        const val COLUMN_MAP_IMAGE: String = "map_image_file"
        const val COLUMN_MOVE_SPEED: String = "movement_speed"
        const val COLUMN_MOVE_TIME: String = "movement_time"
        const val COLUMN_STOP_TIME: String = "stop_time"


        const val TABLE_MARKERS: String = "trips_markers"
        const val COLUMN_MARKER_ID: String = "marker_id"
        const val COLUMN_MARKER_TRIP_ID: String = "trip_id"
        const val COLUMN_MARKER_LATITUDE: String = "marker_latitude"
        const val COLUMN_MARKER_LONGITUDE: String = "marker_longitude"
        const val COLUMN_MARKER_URI: String = "marker_uri"
        const val COLUMN_MARKER_SNIPPET: String = "marker_snippet"
        const val COLUMN_MARKER_PLACEHOLDER1: String = "marker_place_holder1"
        const val COLUMN_MARKER_PLACEHOLDER2: String = "marker_place_holder2"


        private const val TABLE_CREATE = "CREATE TABLE " + TABLE_TRIPS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_DURATION + " NUMERIC, " +
                COLUMN_DIST + " NUMERIC, " +
                COLUMN_SPEED + " NUMERIC, " +
                COLUMN_FROM + " TEXT, " +
                COLUMN_TO + " TEXT, " +
                COLUMN_MAP + " TEXT, " +
                COLUMN_MAX_SPEED + " NUMERIC, " +
                COLUMN_MAX_ALT + " NUMERIC, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_MAP_IMAGE + " TEXT, " +
                COLUMN_MOVE_SPEED + " NUMERIC, " +
                COLUMN_MOVE_TIME + " NUMERIC, " +
                COLUMN_STOP_TIME + " NUMERIC " +
                ")"


        private const val MARKERS_TABLE_CREATE = "CREATE TABLE " + TABLE_MARKERS + " (" +
                COLUMN_MARKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MARKER_TRIP_ID + " NUMERIC, " +
                COLUMN_MARKER_LATITUDE + " NUMERIC, " +
                COLUMN_MARKER_LONGITUDE + " NUMERIC, " +
                COLUMN_MARKER_URI + " TEXT, " +
                COLUMN_MARKER_SNIPPET + " TEXT, " +
                COLUMN_MARKER_PLACEHOLDER1 + " TEXT, " +
                COLUMN_MARKER_PLACEHOLDER2 + " TEXT " +
                ")"
    }
}