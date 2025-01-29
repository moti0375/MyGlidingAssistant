//package com.bartovapps.gpstriprec.db;
//
//import android.content.Context;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//import android.util.Log;
//
//public class com.bartovapps.gpstriprec.domain.db.TripsDBOpenHelper extends SQLiteOpenHelper{
//
//	private static final String LOG_TAG = com.bartovapps.gpstriprec.domain.db.TripsDBOpenHelper.class.getSimpleName();
//	private static final String DATABASE_NAME = "trips.db";
//	private static final int DATABASE_VERSION = 6;
//
//	public static final String TABLE_TRIPS = "trips";
//	public static final String COLUMN_ID = "tripid";
//	public static final String COLUMN_DATE = "DATE";
//	public static final String COLUMN_DIST = "distance";
//	public static final String COLUMN_SPEED = "ave_speed";
//	public static final String COLUMN_DURATION = "duration";
//	public static final String COLUMN_FROM = "start_address";
//	public static final String COLUMN_TO = "stop_address";
//	public static final String COLUMN_MAP = "map";
//	public static final String COLUMN_MAX_SPEED = "max_speed";
//	public static final String COLUMN_MAX_ALT = "max_altitude";
//	public static final String COLUMN_NAME = "name";
//	public static final String COLUMN_MAP_IMAGE = "map_image_file";
//	public static final String COLUMN_MOVE_SPEED = "movement_speed";
//    public static final String COLUMN_MOVE_TIME = "movement_time";
//    public static final String COLUMN_STOP_TIME = "stop_time";
//
//
//
//	public static final String TABLE_MARKERS = "trips_markers";
//	public static final String COLUMN_MARKER_ID = "marker_id";
//	public static final String COLUMN_MARKER_TRIP_ID = "trip_id";
//	public static final String COLUMN_MARKER_LATITUDE = "marker_latitude";
//	public static final String COLUMN_MARKER_LONGITUDE = "marker_longitude";
//	public static final String COLUMN_MARKER_URI = "marker_uri";
//	public static final String COLUMN_MARKER_SNIPPET = "marker_snippet";
//	public static final String COLUMN_MARKER_PLACEHOLDER1 = "marker_place_holder1";
//	public static final String COLUMN_MARKER_PLACEHOLDER2 = "marker_place_holder2";
//
//
//
//	private static final String TABLE_CREATE =
//			"CREATE TABLE " + TABLE_TRIPS + " (" +
//			COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//			COLUMN_DATE + " TEXT, " +
//			COLUMN_DURATION + " NUMERIC, " +
//			COLUMN_DIST + " NUMERIC, " +
//			COLUMN_SPEED + " NUMERIC, " +
//			COLUMN_FROM + " TEXT, " +
//			COLUMN_TO + " TEXT, " +
//			COLUMN_MAP + " TEXT, " +
//			COLUMN_MAX_SPEED + " NUMERIC, " +
//			COLUMN_MAX_ALT   + " NUMERIC, " +
//			COLUMN_NAME      + " TEXT, " +
//			COLUMN_MAP_IMAGE      + " TEXT, " +
//            COLUMN_MOVE_SPEED  + " NUMERIC, " +
//            COLUMN_MOVE_TIME  + " NUMERIC, " +
//            COLUMN_STOP_TIME  + " NUMERIC " +
//			")";
//
//
//	private static final String MARKERS_TABLE_CREATE =
//			"CREATE TABLE " + TABLE_MARKERS + " (" +
//					COLUMN_MARKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//					COLUMN_MARKER_TRIP_ID + " NUMERIC, " +
//					COLUMN_MARKER_LATITUDE + " NUMERIC, " +
//					COLUMN_MARKER_LONGITUDE + " NUMERIC, " +
//					COLUMN_MARKER_URI + " TEXT, " +
//					COLUMN_MARKER_SNIPPET + " TEXT, " +
//					COLUMN_MARKER_PLACEHOLDER1 + " TEXT, " +
//					COLUMN_MARKER_PLACEHOLDER2 + " TEXT " +
//					")";
//
//	public com.bartovapps.gpstriprec.domain.db.TripsDBOpenHelper(Context context) {
//		super(context, DATABASE_NAME, null, DATABASE_VERSION);
//	}
//
//	@Override
//	public void onCreate(SQLiteDatabase db) {
//		String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_TRIPS;
//		db.execSQL(DROP_TABLE);
////        Log.i(LOG_TAG, "Any previous table dropped");
//
//		db.execSQL(TABLE_CREATE);
////        Log.i(LOG_TAG, "Tours table has been created");
//		db.execSQL(MARKERS_TABLE_CREATE); //Create markers table
//	}
//
//	@Override
//	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		if(oldVersion < 2){
////			Log.i(LOG_TAG, "About to upgrade database to version " + DATABASE_VERSION);
//
//			String ADD_MAX_SPEED =
//					"ALTER TABLE " + TABLE_TRIPS +
//					" ADD COLUMN " + COLUMN_MAX_SPEED + " NUMERIC ";
//
//			String ADD_MAX_ALT =
//					"ALTER TABLE " + TABLE_TRIPS +
//					" ADD COLUMN " + COLUMN_MAX_ALT + " NUMERIC ";
//
//			String ADD_NAME =
//					"ALTER TABLE " + TABLE_TRIPS +
//					" ADD COLUMN " + COLUMN_NAME + " NUMERIC ";
//
//			db.execSQL(ADD_MAX_SPEED);
//			db.execSQL(ADD_MAX_ALT);
//			db.execSQL(ADD_NAME);
//
////			Log.i(LOG_TAG, "Database has been upgrade to version " + DATABASE_VERSION);
//		}
//
//		if(oldVersion < 3){
//			String ADD_MAP_IMAGE =
//					"ALTER TABLE " + TABLE_TRIPS +
//					" ADD COLUMN " + COLUMN_MAP_IMAGE + " TEXT ";
//			db.execSQL(ADD_MAP_IMAGE);
//
////			Log.i(LOG_TAG, "Database has been upgrade to version " + DATABASE_VERSION);
//		}
//
//        if(oldVersion < 4){
//            String ADD_COLUMN =
//                    "ALTER TABLE " + TABLE_TRIPS +
//                    " ADD COLUMN " + COLUMN_MOVE_SPEED + " NUMERIC ";
//            db.execSQL(ADD_COLUMN);
//
////            Log.i(LOG_TAG, "Database has been upgrade to version " + DATABASE_VERSION);
//        }
//
//        if(oldVersion < 5){
//            String ADD_MOVE_TIME_COLUMN =
//                    "ALTER TABLE " + TABLE_TRIPS +
//                    " ADD COLUMN " + COLUMN_MOVE_TIME + " NUMERIC ";
//
//            String ADD_STOP_TIME_COLUMN =
//                    "ALTER TABLE " + TABLE_TRIPS +
//                            " ADD COLUMN " + COLUMN_STOP_TIME + " NUMERIC ";
//
//            db.execSQL(ADD_MOVE_TIME_COLUMN);
//            db.execSQL(ADD_STOP_TIME_COLUMN);
//
////            Log.i(LOG_TAG, "Database has been upgrade to version " + DATABASE_VERSION);
//        }
//
//
//		if(oldVersion < 6){
//			db.execSQL(MARKERS_TABLE_CREATE); //Create markers table on older versions
//			Log.i(LOG_TAG, "Markers table has been created");
//		}
//
//
//		//db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
//		//onCreate(db);
//	}
//
//
//
//}
