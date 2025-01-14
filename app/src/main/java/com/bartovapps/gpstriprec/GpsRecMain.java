package com.bartovapps.gpstriprec;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.bartovapps.gpstriprec.db.TripsDataSource;
import com.bartovapps.gpstriprec.displayers.DataDisplayer;
import com.bartovapps.gpstriprec.displayers.FeetAltDisplayer;
import com.bartovapps.gpstriprec.displayers.KmhDisplayer;
import com.bartovapps.gpstriprec.displayers.MetricAltDisplayer;
import com.bartovapps.gpstriprec.displayers.MetricDisplayer;
import com.bartovapps.gpstriprec.displayers.MileageDisplayer;
import com.bartovapps.gpstriprec.displayers.MphDisplayer;
import com.bartovapps.gpstriprec.enums.AltUnits;
import com.bartovapps.gpstriprec.enums.RecordingState;
import com.bartovapps.gpstriprec.enums.SaveStatus;
import com.bartovapps.gpstriprec.enums.Units;
import com.bartovapps.gpstriprec.kmlhleper.KmlParser;
import com.bartovapps.gpstriprec.maphelper.MapHelper;
import com.bartovapps.gpstriprec.services.GpsTripRecService;
import com.bartovapps.gpstriprec.timer.TimerManager;
import com.bartovapps.gpstriprec.trip.Trip;
import com.bartovapps.gpstriprec.trip.TripManager;
import com.bartovapps.gpstriprec.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.bartovapps.gpstriprec.R;

public class GpsRecMain extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {
    private static final String TAG = GpsRecMain.class.getSimpleName();
    private static final int TRIP_LIST_ACTIVITY = 100;
    private static final int CAMERA_INTENT_ACTIVITY = 200;
    private static final long TIME_INTERVAL = (long) 2000;

    private static final float SPEED_FILTER = 0.832f; // 0.833 < 3km/h
    private static final float ACCURACY = 25.0f; // Accuracy of 25 meters
    private static final int NEW_TRIP = 1;
    private static final int CONTINUE_TRIP = 2;
    private static final int FOLLOW_TRIP = 3;
    private static final int LOADED_FROM_INTENT = 4;

    private static final int AUTO_SAVE = 0;


    // These values are for debug purposes
    // private static final float SPEED_FILTER = 0.0f; // 0.833 < 3km/h
    // private static final float ACCURACY = 1500.0f; // Accuracy of 10 meters

    // private static final int GPS_ERROR_DIALOG_REQUEST = 9001;

    public static final float CAM_INIT_ZOOM = 0;
    private float cameraZoom = 15;
    private float lineWidth = 5;
    public static final String PASSED = "PASSED";
    public static final String FAILED = "FAILED";

    private TextView tvSpeed;
    // private TextView tvLatitude;
    // private TextView tvLongitude;
    private TextView tvDistance;
    private TextView tvTimer;
    private TextView tvAltitude;
    //    private ToggleButton btStartStop;
    private FloatingActionButton fabStartStop;
    private FloatingActionButton fabStartCamera;

    private TripManager routeManager;
    private TripsDataSource datasource;
    private DataDisplayer speedDisplayer;
    private DataDisplayer distanceDisplayer;
    private DataDisplayer altitudeDisplayer;

    private LocationManager lm;
    //	private LocationClient locationClient;
    private GoogleApiClient locationClient;
    private SharedPreferences settings;
    private Units units = Units.Metric;
    private int autoSave = AUTO_SAVE;
    private AltUnits altUnits = AltUnits.Feet;
    private int lineColor = Color.RED;
    private int mapType = GoogleMap.MAP_TYPE_NORMAL;
    private int recordingMode = NEW_TRIP;

    // private boolean gpsFixed = false;
    private boolean isServicesOk = true;
    private boolean isActivityVisible = true;
    TimerManager timerManager;
    GoogleMap mMap;

    MapHelper mapHelper;
    ProgressDialog gpsPd;
    ProgressDialog savingTripPd;
    ProgressDialog loadingTripPd;

    Location lastKnownLocation;
    private RecordingState recordingState = RecordingState.Idle;
    private ImageView imRecording;
    Handler handler = new Handler(Looper.getMainLooper());
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private Trip uploadedTrip;
    GpsTripRecService recordingService;
    boolean serviceBounded = false;
    Intent serviceIntent;

    SupportMapFragment mapFrag;

    //File variable is static. This helps to prevent null pointer exceptions when return from camera intent.
    //This may occure if image was taken in landscape and phone was rotated back to portrait before returning to this Activity,
    //static helps to keeps the file variable with the value that was passed to the camera Activity.
    private static File mImageMarkerFileLocation = null;


    private Toolbar toolbar;

    @SuppressLint("SimpleDateFormat")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Log.i(LOG_TAG, "onCreate");
        setContentView(R.layout.gps_recorder_main_material);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
            // WTF SAMSUNG!
        }

        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        settings = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        settings.registerOnSharedPreferenceChangeListener(prefListener);
        updatePreferences();
        handler = new Handler(Looper.getMainLooper());
        isServicesOk = Utils.servicesOk(GpsRecMain.this);
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        datasource = new TripsDataSource(GpsRecMain.this);
        gpsPd = new ProgressDialog(GpsRecMain.this);
        savingTripPd = new ProgressDialog(GpsRecMain.this);
        serviceIntent = new Intent(GpsRecMain.this, GpsTripRecService.class);
        // Create the interstitial.


        if (isServicesOk) {

            Log.i(TAG, "onCreate: services OK");
            setUpMapIfNeeded();
            setUiComponents();
            // setDisplayers();

            locationClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            timerManager = new TimerManager(getApplicationContext(), tvTimer);
        }

    }


    // Invoke displayInterstitial() when you are ready to display an
    // interstitial.


    private void updatePreferences() {
        int units = Integer.parseInt(settings.getString(getResources()
                .getString(R.string.units), "1"));
        int color = Integer.parseInt(settings.getString(getResources()
                .getString(R.string.LineColorPref), "1"));
        float zoom = Float.parseFloat(settings.getString(getResources()
                .getString(R.string.ZoomPref), "15"));
        float width = Float.parseFloat(settings.getString(getResources()
                .getString(R.string.LineWidthPref), "5"));
        this.autoSave = Integer.parseInt(settings.getString(getString(R.string.AutoSavePrefKey), "0"));

        int altitudeUnits = Integer.parseInt(settings.getString(getResources()
                .getString(R.string.altitudeUnitsKey), "1"));

        // Log.i(LOG_TAG, "Selected units: " + units);

        switch (units) {
            case 1:
                this.units = Units.Metric;
                break;
            case 2:
                this.units = Units.Mileage;
                break;
        }

        switch (altitudeUnits) {
            case 1:
                this.altUnits = AltUnits.Feet;
                break;
            case 2:
                this.altUnits = AltUnits.Metric;
                break;
        }

        setDisplayers();

        switch (color) {
            case 1:
                this.lineColor = Color.RED;
                break;
            case 2:
                this.lineColor = Color.GREEN;
                break;
            case 3:
                this.lineColor = Color.YELLOW;
                break;
            case 4:
                this.lineColor = Color.BLUE;
                break;
            default:
                this.lineColor = Color.RED;
                break;
        }

        this.lineWidth = width;
        this.cameraZoom = zoom;

        if (mapHelper != null) {
            mapHelper.setLineColor(lineColor);
            mapHelper.setZoom(zoom);
            mapHelper.setLineWidth(width);
        }
    }

    private void setDisplayers() {
        if (units == Units.Mileage) {
            // Log.i(LOG_TAG, "Settings Mileage displayer");
            speedDisplayer = new MphDisplayer();
            distanceDisplayer = new MileageDisplayer();
        } else {
            // Log.i(LOG_TAG, "Settings Metric displayer");
            speedDisplayer = new KmhDisplayer();
            distanceDisplayer = new MetricDisplayer();
        }

        if (altUnits == AltUnits.Feet) {
            altitudeDisplayer = new FeetAltDisplayer();
        } else {
            altitudeDisplayer = new MetricAltDisplayer();
        }
    }

    private void setUiComponents() {

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // Typeface typeFace = Typeface.createFromAsset(getAssets(),
        // "fonts/Digit01.ttf");

        tvSpeed =  findViewById(R.id.tvSpeed);
        tvDistance =  findViewById(R.id.tvDistance);
        tvTimer =  findViewById(R.id.tvTimer);
        tvAltitude =  findViewById(R.id.tvAltitude);


        //btStartStop = (ToggleButton) findViewById(R.id.btStartStop);
        fabStartStop = findViewById(R.id.fabStartStop);
        fabStartCamera =  findViewById(R.id.fabCamera);


        // tvSpeed.setTypeface(typeFace);
        // tvDistance.setTypeface(typeFace);
        // tvTimer.setTypeface(typeFace);
        // tvAccuracy.setTypeface(typeFace);

//        btStartStop.setOnClickListener(btListener);
        fabStartStop.setOnClickListener(btListener);
        fabStartCamera.setOnClickListener(btListener);
        imRecording = (ImageView) findViewById(R.id.imRecording);

        ViewTreeObserver vto = tvAltitude.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                int topPadding = findViewById(R.id.llGauges).getMeasuredHeight();
                int bottomPadding = tvAltitude.getMeasuredHeight();
//                Log.i(LOG_TAG, "topPadding: " + topPadding + "\nbottomPadding: " + bottomPadding);
                if(mMap != null){ //map shouldn't be null by now..
                    mMap.setPadding(0,  topPadding , 0, bottomPadding + 5);
                }

                ViewTreeObserver obs = tvAltitude.getViewTreeObserver();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }
            }

        });

    }

    OnClickListener btListener = new OnClickListener() {

        @Override
        public void onClick(View view) {


            if (view == fabStartStop) {
                if (recordingState == RecordingState.Idle) {
                    if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        //  stopRecording();
                        // fabStartStop.setChecked(false);
                        // fabStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_action_stop));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            fabStartStop.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_new, GpsRecMain.this.getTheme()));
                        } else {
                            fabStartStop.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_new));
                        }
                        buildAlertMessageNoGPS();
                    } else {
                        connectServicesIfNeeded();
                        if (locationClient.isConnected()) {
                            if (uploadedTrip != null) {
                                if (recordingMode == LOADED_FROM_INTENT) {
                                    startRecording();
                                } else {
                                    loadedTripDialog();
                                }
                            } else {
                                recordingMode = NEW_TRIP;
                                startRecording();
                            }
                        } else {
                            Toast.makeText(
                                    GpsRecMain.this,
                                    getResources().getString(
                                            R.string.ServicesDisconnected),
                                    Toast.LENGTH_LONG).show();
//                            Log.i(LOG_TAG, "location client disconnected!");
                            stopRecording();
                            connectServicesIfNeeded();
                        }
                    }
                } else {
                    if (autoSave == AUTO_SAVE) {
                        saveTrip();
                    } else {
                        saveTripAlertDialog();
                    }
                }
            }

            if (view == fabStartCamera) {

//                Intent camIntent = new Intent(GpsRecMain.this, GpsTripRecCamera.class);
//                startActivityForResult(camIntent, getResources().getInteger(R.integer.GPS_CAMERA_ACTIVITY));
                takePhoto();
            }
        }

    };


    private void startRecording() {

        // fabStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_action_stop));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fabStartStop.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_stop, this.getTheme()));
        } else {
            fabStartStop.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_stop));
        }

        fabStartCamera.setVisibility(View.VISIBLE);
        switch (recordingMode) {
            case NEW_TRIP:
                routeManager.resetRoute(true);
                break;
            case FOLLOW_TRIP:
                routeManager.resetRoute(false);
                break;
            case CONTINUE_TRIP:
                break;
            case LOADED_FROM_INTENT:
                break;
            default:
                routeManager.resetRoute(true);
                break;
        }

        enableLocationListener(fixListener, 1);
        mapHelper.setZoom(cameraZoom);
        mapHelper.setCameraTilt(0);
        updateDisplay();
        gpsPd.setCancelable(true);
        gpsPd.setOnCancelListener(pdOnCancelListener);
        gpsPd.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        gpsPd.setTitle(getString(R.string.app_name));
        gpsPd.setMessage(getString(R.string.WaitForGPS));
        gpsPd.show();

        recordingState = RecordingState.Recording;
        imRecording.setVisibility(View.VISIBLE);
    }

    private void stopRecording() {
        timerManager.stopTimer();
//            disableLocationListener(locListener);
        disableLocationListener(fixListener);
        disableGpsLocationListener(locationListener);
        recordingState = RecordingState.Idle;
        imRecording.setVisibility(View.GONE);
        // btStartStop.setChecked(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fabStartStop.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_new, this.getTheme()));
        } else {
            fabStartStop.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_new));
        }
        fabStartCamera.setVisibility(View.INVISIBLE);

        uploadedTrip = null;
        stopService();

        //locationClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;

        if (isServicesOk) {
            setUpMapIfNeeded();
            connectServicesIfNeeded();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityVisible = false;

        if (recordingState == RecordingState.Recording) {

            Toast.makeText(GpsRecMain.this, getString(R.string.StillRecording), Toast.LENGTH_SHORT)
                    .show();
            //backPressedDialog();
        }
    }

    @Override
    protected void onDestroy() {
        // Log.i(LOG_TAG, "onDestroy was called");
        if (isServicesOk) { // If Google Services OK
            if (recordingState == RecordingState.Recording) {
                stopRecording();
                routeManager.saveTrip();
            }

            disableGpsLocationListener(locationListener);
            disableLocationListener(fixListener);
            timerManager.pauseTimer();
            locationClient.disconnect();
        }

        try {
            Utils.deleteCache(GpsRecMain.this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            stopService(serviceIntent);
            unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(GpsRecMain.this,
                getResources().getString(R.string.Goodbye), Toast.LENGTH_SHORT)
                .show();

        super.onDestroy();


    }


    @Override
    public void finish() {
        if (recordingState == RecordingState.Recording) {

            Intent setIntent = new Intent(Intent.ACTION_MAIN);
            setIntent.addCategory(Intent.CATEGORY_HOME);
            //setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(setIntent);
        } else {
            super.finish();
        }
    }

//    @Override
//    public void finish() {
//        super.finish();
//        if (isServicesOk) { // If Services OK
//            if (recordingState == RecordingState.Recording) {
//                stopRecording();
//                routeManager.saveTrip();
//            }
//
////            disableLocationListener(locListener);
//            disableLocationListener(fixListener);
//            disableGpsLocationListener(locationListener);
//            timerManager.pauseTimer();
//            locationClient.disconnect();
//        }
//        Toast.makeText(GpsRecMain.this,
//                getResources().getString(R.string.Goodbye), Toast.LENGTH_SHORT)
//                .show();
//    }

    private void enableGPSLocationListener(LocationListener listener) {
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIME_INTERVAL,
                0, listener);
    }

    private void disableGpsLocationListener(LocationListener listener) {
        try {
            lm.removeUpdates(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enableLocationListener(
            com.google.android.gms.location.LocationListener listener,
            int numOfUpdates) {

        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(TIME_INTERVAL);
        request.setFastestInterval(1000);
        if (numOfUpdates != -1) {
            request.setNumUpdates(numOfUpdates);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                locationClient, request, listener);

//		locationClient.requestLocationUpdates(request, listener);
    }

    private void disableLocationListener(
            com.google.android.gms.location.LocationListener listener) {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClient, listener);
//			locationClient.removeLocationUpdates(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gps_recorder_menus, menu);
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_trips:
                Intent tripListIntent = new Intent(this, GpsRecTripsList.class);
                startActivityForResult(tripListIntent,
                        getResources().getInteger(R.integer.GPS_TRIPS_LIST));
                break;
            case R.id.action_settigns:
                Intent settings_intent = new Intent(this, GpsRecPrefs.class);
                startActivity(settings_intent);
                break;
            case R.id.action_license:
                Intent license_intent = new Intent(this, GpsRecLicense.class);
                startActivity(license_intent);
                break;
            case R.id.action_musicPlayer:
                try {
                    Intent intent = new Intent(
                            MediaStore.INTENT_ACTION_MUSIC_PLAYER);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(GpsRecMain.this,
                            getResources().getString(R.string.noMusicPlayer),
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
        return true;
    }

    LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Log.i(LOG_TAG, provider + " provider status changed");

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS is disabled!!",
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLocationChanged(Location location) {
            // Log.i(LOG_TAG, "got new location");
            routeManager.updateLocation(location);
            updateDisplay();
        }
    };

    // LocationListener fixListener = new LocationListener() {
    //
    // @Override
    // public void onStatusChanged(String provider, int status, Bundle extras) {
    //
    // }
    //
    // @Override
    // public void onProviderEnabled(String provider) {
    //
    // }
    //
    // @Override
    // public void onProviderDisabled(String provider) {
    //
    // }
    //
    // @Override
    // public void onLocationChanged(Location location) {
    // Toast.makeText(context,
    // "Gps location fixed!! You may start walking or running",
    // Toast.LENGTH_SHORT).show();
    //
    // pd.dismiss();
    // enableLocationListener(locationListener);
    // disableLocationListener(fixListener);
    // timerManager.startTimer();
    // mapHelper.setLocation(location, CAM_FIX_ZOOM);
    // }
    // };

    protected void updateDisplay() {
        speedDisplayer.displayData(tvSpeed, routeManager.getSpeed());
        distanceDisplayer.displayData(tvDistance, routeManager.getDistance());
        altitudeDisplayer.displayData(tvAltitude, routeManager.getAltitude());

        if (recordingService != null && serviceBounded) {
            recordingService.updateService(routeManager.getDistance());
        }
//        if(!isActivityVisible){
//            updateNotification(routeManager.getDistance());
//        }
    }


    /**
     * Alert in case GPS is disabled!
     */
    private void buildAlertMessageNoGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.NoGPSHead));
        builder.setMessage(getResources().getString(R.string.NoGPSBody))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                Toast.makeText(
                                        GpsRecMain.this,
                                        getString(R.string.GpsMustEnabled),
                                        Toast.LENGTH_LONG).show();
                                dialog.cancel();
                                // finish();
                            }
                        });
        final AlertDialog alert = builder.create();
        alert.show();
    }



    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (mMap == null && isServicesOk) {
            Log.i(TAG, "setUpMapIfNeeded: initializing map");

            mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            // Check if we were successful in obtaining the map.
            mapFrag.getMapAsync(this);
        }
    }

    private void setUpMap() {
    }


    private void connectServicesIfNeeded() {
        Log.i(TAG, "Trying to connect location client");
        if (locationClient.isConnected() == false) {
            locationClient.connect();
        }
    }

    public void saveTripAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                GpsRecMain.this);

        // set title
        alertDialogBuilder.setTitle(getResources()
                .getString(R.string.SAVE_TRIP));

        // set dialog message
        alertDialogBuilder
                .setMessage(getResources().getString(R.string.SaveDialog))
                .setCancelable(true)
                .setIcon(getResources().getDrawable(R.drawable.ic_launcher))
                .setPositiveButton(getResources().getString(R.string.YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                saveTrip();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                stopRecording();
                                dialog.dismiss();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.setOnCancelListener(saveDialogCancelListener);
        // show it
        alertDialog.show();
    }

    private void saveTrip() {
        stopRecording();
        savingTripPd.setTitle(getString(R.string.app_name));
        savingTripPd.setMessage(getString(R.string.SavingTrip));
        savingTripPd.setCancelable(true);
        savingTripPd.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        savingTripPd.show();
        new SaveTripTask().execute("");
    }

    OnCancelListener pdOnCancelListener = new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
            Toast.makeText(GpsRecMain.this, getString(R.string.Canceled), Toast.LENGTH_SHORT).show();
            stopRecording();
        }
    };

    OnCancelListener saveDialogCancelListener = new OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            //btStartStop.setChecked(true);
            fabStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_action_new));


        }
    };

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        //Log.i(LOG_TAG, "Connection Failed..");

        Log.e(TAG, "onConnectionFailed: " + result);
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (SendIntentException e) {
                // There was an error with the resolution intent. Try again.
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }


    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    @Override
    public void onMapLoaded() {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady: ");
        mMap = googleMap;
        mapHelper = new MapHelper(mMap, CAM_INIT_ZOOM, lineColor, mapType,
                handler, this);
        mapHelper.setLineWidth(lineWidth);
        routeManager = new TripManager(GpsRecMain.this, ACCURACY,
                SPEED_FILTER, mapHelper, datasource, timerManager);
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((GpsRecMain) getActivity()).onDialogDismissed();
        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.i(TAG, "onConnected was called");

        String NetworkProvider = LocationManager.NETWORK_PROVIDER;
        lastKnownLocation = lm.getLastKnownLocation(NetworkProvider);

        String path = null;

        try {
            path = getIntent().getStringExtra("kml_path");
//            Log.i(LOG_TAG, "Got path: " + path);
        } catch (NullPointerException e) {
//            Log.i(LOG_TAG, "path is null");
            e.printStackTrace();
        }

        if (path != null) { //this means that app started by tap on kml file!
            uploadedTrip = new Trip(path, null, 0, 0);
            UploadTripTask uploadTripTask = new UploadTripTask();
            uploadTripTask.execute(uploadedTrip);
            recordingMode = LOADED_FROM_INTENT;
        } else {
            if (lastKnownLocation != null) {
//            Log.i(LOG_TAG, "Got last known location!");
                mapHelper.setZoom(cameraZoom);
                mapHelper.setLocation(lastKnownLocation);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        connectServicesIfNeeded();
//        Log.i(LOG_TAG, "Connection Suspended");
    }


    com.google.android.gms.location.LocationListener fixListener = new com.google.android.gms.location.LocationListener() {

        @Override
        public void onLocationChanged(Location location) {

            if (gpsPd.isShowing()) {
                gpsPd.dismiss();
            }
            startService();

//            recordingService.updateService(getResources().getString(R.string.LocationFounded));

            // enableLocationListener(locListener, -1);
            enableGPSLocationListener(locationListener);
            routeManager.setCurrentLocation(location);
            mapHelper.setLocation(location);

            if (recordingState == RecordingState.Recording) {
                if (recordingMode == CONTINUE_TRIP) {
                    timerManager.resumeTimer();
                } else {
                    timerManager.startTimer();
                }
            }
            Toast.makeText(GpsRecMain.this,
                    getResources().getString(R.string.LocationFounded),
                    Toast.LENGTH_LONG).show();
        }
    };


    com.google.android.gms.location.LocationListener locListener = new com.google.android.gms.location.LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            routeManager.updateLocation(location);
            updateDisplay();
        }

    };

    OnSharedPreferenceChangeListener prefListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            updatePreferences();
            //   updateDisplay();

        }
    };

    private class SaveTripTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String result = new String();
            SaveStatus status = routeManager.saveTrip();
            switch (status) {
                case PASSED:
                    result = getResources().getString(R.string.TripSaved);
                    break;
                case NOT_ENOUGH_DATA:
                    result = getResources().getString(R.string.NotEnoughData);
                    break;
                default:
                    break;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            savingTripPd.dismiss();
            Toast.makeText(GpsRecMain.this, result, Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    @Override
    public void onBackPressed() {
        if (recordingState == RecordingState.Recording) {
            finish();
        } else {
            super.onBackPressed();
        }
    }


    private void backPressedDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        builder.setTitle(getResources().getString(R.string.app_name));
        builder.setMessage(getResources().getString(R.string.StopAndExit))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                finish();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                dialog.cancel();
                            }
                        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == TRIP_LIST_ACTIVITY) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && data.hasExtra("UploadedTrip")) {
                uploadedTrip = (Trip) data.getSerializableExtra("UploadedTrip");
                if (uploadedTrip != null) {
                    if (recordingState == RecordingState.Idle) {
                        new UploadTripTask().execute(uploadedTrip);
                    } else {
                        Toast.makeText(this, getString(R.string.upload_while_recording), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        if (requestCode == CAMERA_INTENT_ACTIVITY && resultCode == RESULT_OK) {
 //        Uri returnedUri = Uri.parse(data.getStringExtra(MediaStore.EXTRA_OUTPUT));
 //           Toast.makeText(GpsRecMain.this,"Returned from camera intent, uri: " + returnedUri.getPath(), Toast.LENGTH_SHORT).show();
            Uri capturedImageUri = Uri.fromFile(mImageMarkerFileLocation);
//            Log.i(LOG_TAG, "Image Uri: " + capturedImageUri.toString());

            try {
                routeManager.addImageMarker(capturedImageUri);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(GpsRecMain.this, "There was an error, please try again...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UploadTripTask extends AsyncTask<Trip, Void, String> {

        @Override
        protected void onPreExecute() {
            loadingTripPd = new ProgressDialog(GpsRecMain.this);
            loadingTripPd.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
            loadingTripPd.setTitle(getString(R.string.app_name));
            loadingTripPd.setMessage(getString(R.string.displaying_trip));
            loadingTripPd.setCancelable(true);
            loadingTripPd.show();
        }

        @Override
        protected String doInBackground(Trip... params) {
            Trip trip = params[0];
            if (Utils.isFileExists(trip.getKml())) {

                int status = routeManager.uploadTrip(trip);
                if (status != KmlParser.KML_OPENED) {
                    return FAILED;
                }
            } else {
                return FAILED;
            }
            return PASSED;
        }

        @Override
        protected void onPostExecute(String result) {
            loadingTripPd.dismiss();
            if (result.equals(PASSED)) {
                loadingTripPd = null;
                Toast.makeText(GpsRecMain.this, getResources().getString(R.string.TripLoaded),
                        Toast.LENGTH_LONG).show();
                updateDisplay();
            } else {
                uploadedTrip = null;
                Toast.makeText(GpsRecMain.this, R.string.unable_to_open_kml,
                        Toast.LENGTH_LONG).show();

            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private void loadedTripDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        builder.setTitle(getResources().getString(R.string.app_name));
        builder.setMessage(getResources().getString(R.string.ContinueLoadedTrip))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                recordingMode = CONTINUE_TRIP;
                                startRecording();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                recordingMode = FOLLOW_TRIP;
                                mapHelper.clearMarkers();
                                startRecording();
                            }
                        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // Toast.makeText(GpsRecMain.this, "onServiceConnected called", Toast.LENGTH_SHORT).show();
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            GpsTripRecService.LocalBinder binder = (GpsTripRecService.LocalBinder) service;
            recordingService = binder.getService();
            serviceBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Toast.makeText(GpsRecMain.this, "onServiceDisconnected called", Toast.LENGTH_SHORT).show();
            serviceBounded = false;
        }
    };

    public void startService() {
        startService(serviceIntent);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void stopService() {

//        Log.i(LOG_TAG, "stopService was called");
        // stopService(serviceIntent);
        try {
            stopService(serviceIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        serviceBounded = false;
    }

    private void takePhoto() {

        try {
            createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(GpsRecMain.this, "Couldn't create photo file...", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri imageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", mImageMarkerFileLocation);
        Intent cameraIntent = new Intent();
        cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
//        cameraIntent.setData(imageUri);
//        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, CAMERA_INTENT_ACTIVITY);

    }

    private void createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        Log.i(LOG_TAG, "Timestamp: " + timeStamp);
        String imageFileName = "Image_" + timeStamp + "_";
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        mImageMarkerFileLocation = File.createTempFile(imageFileName, ".jpg", storageDirectory);
    }

}
