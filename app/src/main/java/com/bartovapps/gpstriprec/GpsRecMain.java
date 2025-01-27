//package com.bartovapps.gpstriprec;
//
//import static com.bartovapps.gpstriprec.kmlhleper.com.bartovapps.gpstriprec.core.trip_manager.com.bartovapps.gpstriprec.domain.trip_manager.KmlParserImpl.KML_OPENED;
//
//import android.annotation.SuppressLint;
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.app.DialogFragment;
//import android.app.ProgressDialog;
//import android.content.ActivityNotFoundException;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.DialogInterface.OnCancelListener;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.content.SharedPreferences;
//import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
//import android.graphics.Color;
//
//import android.location.Geocoder;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewTreeObserver;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.core.content.FileProvider;
//import androidx.core.content.res.ResourcesCompat;
//
//import com.bartovapps.gpstriprec.core.db.TripsDataSource;
//import com.bartovapps.gpstriprec.core.map_helper.MapHelper;
//import com.bartovapps.gpstriprec.core.timer.TripTimer;
//import com.bartovapps.gpstriprec.data.enums.AltitudeUnits;
//import com.bartovapps.gpstriprec.data.enums.RecordingState;
//import com.bartovapps.gpstriprec.data.enums.SaveStatus;
//import com.bartovapps.gpstriprec.data.enums.Units;
//import com.bartovapps.gpstriprec.displayers.FeetAltDisplayer;
//import com.bartovapps.gpstriprec.displayers.MetricDisplayer;
//import com.bartovapps.gpstriprec.displayers.MileageDisplayer;
//import com.bartovapps.gpstriprec.displayers.MphDisplayer;
//import com.bartovapps.gpstriprec.presentation.displayers.DataDisplayer;
//import com.bartovapps.gpstriprec.presentation.displayers.KmhDisplayer;
//import com.bartovapps.gpstriprec.presentation.displayers.MetricAltDisplayer;
//import com.bartovapps.gpstriprec.services.GpsTripRecService;
//import com.bartovapps.gpstriprec.utils.Utils;
//import com.google.android.gms.common.GooglePlayServicesUtil;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//
//import java.io.File;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import javax.inject.Inject;
//
//import core.trip_manager.TripManager;
//import core.trip_manager.TripManagerImpl;
//import dagger.hilt.android.AndroidEntryPoint;
//import data.model.Trip;
//
//@AndroidEntryPoint
//public class GpsRecMain extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {
//    private static final String TAG = GpsRecMain.class.getSimpleName();
//    private static final int TRIP_LIST_ACTIVITY = 100;
//    private static final int CAMERA_INTENT_ACTIVITY = 200;
//    private static final long TIME_INTERVAL = 2000;
//
//     private static final int NEW_TRIP = 1;
//    private static final int CONTINUE_TRIP = 2;
//    private static final int FOLLOW_TRIP = 3;
//    private static final int LOADED_FROM_INTENT = 4;
//
//    private static final int AUTO_SAVE = 0;
//
//
//    // These values are for debug purposes
//    // private static final float SPEED_FILTER = 0.0f; // 0.833 < 3km/h
//    // private static final float ACCURACY = 1500.0f; // Accuracy of 10 meters
//
//    // private static final int GPS_ERROR_DIALOG_REQUEST = 9001;
//
//    public static final float CAM_INIT_ZOOM = 0;
//    private float cameraZoom = 15;
//    private float lineWidth = 5;
//    public static final String PASSED = "PASSED";
//    public static final String FAILED = "FAILED";
//
//    private TextView tvSpeed;
//     private TextView tvDistance;
//    private TextView tvTimer;
//    private TextView tvAltitude;
//    //    private ToggleButton btStartStop;
//    private FloatingActionButton fabStartStop;
//    private FloatingActionButton fabStartCamera;
//
//    @Inject
//    public TripManager tripManager;
//    @Inject
//    public TripsDataSource datasource;
//    @Inject
//    TripTimer timerManager;
//    @Inject
//    MapHelper mapHelper;
//
//    private DataDisplayer speedDisplayer;
//    private DataDisplayer distanceDisplayer;
//    private DataDisplayer altitudeDisplayer;
//
//    private LocationManager lm;
//    private SharedPreferences settings;
//    private Units units = Units.Metric;
//    private int autoSave = AUTO_SAVE;
//    private AltitudeUnits altUnits = AltitudeUnits.Feet;
//    private int lineColor = Color.RED;
//    private int mapType = GoogleMap.MAP_TYPE_NORMAL;
//    private int recordingMode = NEW_TRIP;
//
//
//
//    GoogleMap mMap;
//    ProgressDialog gpsPd;
//    ProgressDialog savingTripPd;
//    ProgressDialog loadingTripPd;
//
//    Location lastKnownLocation;
//    private RecordingState recordingState = RecordingState.Idle;
//    private ImageView imRecording;
//    Handler handler = new Handler(Looper.getMainLooper());
//    // Request code to use when launching the resolution activity
//    private static final int REQUEST_RESOLVE_ERROR = 1001;
//    // Unique tag for the error dialog fragment
//    private static final String DIALOG_ERROR = "dialog_error";
//    // Bool to track whether the app is already resolving an error
//    private Trip uploadedTrip;
//    GpsTripRecService recordingService;
//    boolean serviceBounded = false;
//    Intent serviceIntent;
//
//    SupportMapFragment mapFrag;
//
//    //File variable is static. This helps to prevent null pointer exceptions when return from camera intent.
//    //This may occure if image was taken in landscape and phone was rotated back to portrait before returning to this Activity,
//    //static helps to keeps the file variable with the value that was passed to the camera Activity.
//    private static File mImageMarkerFileLocation = null;
//
//
//    private Toolbar toolbar;
//
//    @SuppressLint("SimpleDateFormat")
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.gps_recorder_main_material);
//
//        toolbar = findViewById(R.id.app_bar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setLogo(R.drawable.ic_launcher);
//        getSupportActionBar().setDisplayShowTitleEnabled(false);
//
//        settings = this.getSharedPreferences("GPS_TRIP_RECORDER", MODE_PRIVATE);
//        settings.registerOnSharedPreferenceChangeListener(prefListener);
//        handler = new Handler(Looper.getMainLooper());
//        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//        gpsPd = new ProgressDialog(GpsRecMain.this);
//        savingTripPd = new ProgressDialog(GpsRecMain.this);
//        serviceIntent = new Intent(GpsRecMain.this, GpsTripRecService.class);
//        // Create the interstitial.
//
//        setUpMapIfNeeded();
//        setUiComponents();
//        subscribeTimerChanges();
//
//    }
//
//    private void subscribeTimerChanges() {
//        timerManager.subscribeTimerChanges(); //todo implement timer observer
//    }
//
//    // Invoke displayInterstitial() when you are ready to display an
//    // interstitial.
//
//
//    private void updatePreferences() {
//        int units = Integer.parseInt(settings.getString(getResources()
//                .getString(R.string.units), "1"));
//        int color = Integer.parseInt(settings.getString(getResources()
//                .getString(R.string.LineColorPref), "1"));
//        float zoom = Float.parseFloat(settings.getString(getResources()
//                .getString(R.string.ZoomPref), "15"));
//        float width = Float.parseFloat(settings.getString(getResources()
//                .getString(R.string.LineWidthPref), "5"));
//        this.autoSave = Integer.parseInt(settings.getString(getString(R.string.AutoSavePrefKey), "0"));
//
//        int altitudeUnits = Integer.parseInt(settings.getString(getResources()
//                .getString(R.string.altitudeUnitsKey), "1"));
//
//        if (units == 2) {
//            this.units = Units.Millage;
//        } else {
//            this.units = Units.Metric;
//        }
//
//        if (altitudeUnits == 1) {
//            this.altUnits = AltitudeUnits.Feet;
//        } else {
//            this.altUnits = AltitudeUnits.Metric;
//        }
//
//        setDisplayers();
//
//        switch (color) {
//            case 2:
//                this.lineColor = Color.GREEN;
//                break;
//            case 3:
//                this.lineColor = Color.YELLOW;
//                break;
//            case 4:
//                this.lineColor = Color.BLUE;
//                break;
//            default:
//                this.lineColor = Color.RED;
//                break;
//        }
//
//        this.lineWidth = width;
//        this.cameraZoom = zoom;
//
//        if (mapHelper != null) {
//            mapHelper.setLineColor(lineColor);
//            mapHelper.setZoom(zoom);
//            mapHelper.setLineWidth(width);
//        }
//    }
//
//    private void setDisplayers() {
//        if (units == Units.Millage) {
//            speedDisplayer = new MphDisplayer();
//            distanceDisplayer = new MileageDisplayer();
//        } else {
//            speedDisplayer = new KmhDisplayer();
//            distanceDisplayer = new MetricDisplayer();
//        }
//
//        if (altUnits == AltitudeUnits.Feet) {
//            altitudeDisplayer = new FeetAltDisplayer();
//        } else {
//            altitudeDisplayer = new MetricAltDisplayer();
//        }
//    }
//
//    private void setUiComponents() {
//
//        tvSpeed = findViewById(R.id.tvSpeed);
//        tvDistance = findViewById(R.id.tvDistance);
//        tvTimer = findViewById(R.id.tvTimer);
//        tvAltitude = findViewById(R.id.tvAltitude);
//
//
//        //btStartStop = (ToggleButton) findViewById(R.id.btStartStop);
//        fabStartStop = findViewById(R.id.fabStartStop);
//        fabStartCamera = findViewById(R.id.fabCamera);
//
//
//        // tvSpeed.setTypeface(typeFace);
//        // tvDistance.setTypeface(typeFace);
//        // tvTimer.setTypeface(typeFace);
//        // tvAccuracy.setTypeface(typeFace);
//
////        btStartStop.setOnClickListener(btListener);
//        fabStartStop.setOnClickListener(btListener);
//        fabStartCamera.setOnClickListener(btListener);
//        imRecording = (ImageView) findViewById(R.id.imRecording);
//
//        ViewTreeObserver vto = tvAltitude.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//
//            @Override
//            public void onGlobalLayout() {
//                int topPadding = findViewById(R.id.llGauges).getMeasuredHeight();
//                int bottomPadding = tvAltitude.getMeasuredHeight();
////                Log.i(LOG_TAG, "topPadding: " + topPadding + "\nbottomPadding: " + bottomPadding);
//                if (mMap != null) { //map shouldn't be null by now..
//                    mMap.setPadding(0, topPadding, 0, bottomPadding + 5);
//                }
//
//                ViewTreeObserver obs = tvAltitude.getViewTreeObserver();
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                    obs.removeOnGlobalLayoutListener(this);
//                } else {
//                    obs.removeGlobalOnLayoutListener(this);
//                }
//            }
//
//        });
//
//    }
//
//    OnClickListener btListener = view -> {
//        if (view == fabStartStop) {
//            if (recordingState == RecordingState.Idle) {
//                if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//                    fabStartStop.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_new, getTheme()));
//                    buildAlertMessageNoGPS();
//                } else {
//                    if (uploadedTrip != null) {
//                        if (recordingMode == LOADED_FROM_INTENT) {
//                            startRecording();
//                        } else {
//                            loadedTripDialog();
//                        }
//                    } else {
//                        recordingMode = NEW_TRIP;
//                        startRecording();
//                    }
//                }
//            } else {
//                if (autoSave == AUTO_SAVE) {
//                    saveTrip();
//                } else {
//                    saveTripAlertDialog();
//                }
//            }
//        }
//
//        if (view == fabStartCamera) {
//
////                Intent camIntent = new Intent(GpsRecMain.this, GpsTripRecCamera.class);
////                startActivityForResult(camIntent, getResources().getInteger(R.integer.GPS_CAMERA_ACTIVITY));
//            takePhoto();
//        }
//    };
//
//
//    private void startRecording() {
//        fabStartStop.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_stop, this.getTheme()));
//        fabStartCamera.setVisibility(View.VISIBLE);
//        switch (recordingMode) {
//            case CONTINUE_TRIP:
//                break;
//            case FOLLOW_TRIP:
//                tripManager.resetRoute(false);
//                break;
//            default:
//                tripManager.resetRoute(true);
//                break;
//        }
//
//        enableLocationListener(fixListener);
//        mapHelper.setZoom(cameraZoom);
//        mapHelper.setCameraTilt(0);
//        updateDisplay();
//        gpsPd.setCancelable(true);
//        gpsPd.setOnCancelListener(pdOnCancelListener);
//        gpsPd.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, getTheme()));
//        gpsPd.setTitle(getString(R.string.app_name));
//        gpsPd.setMessage(getString(R.string.WaitForGPS));
//        gpsPd.show();
//
//        recordingState = RecordingState.Recording;
//        imRecording.setVisibility(View.VISIBLE);
//    }
//
//    private void stopRecording() {
//        timerManager.stopTimer();
//        disableLocationListener(fixListener);
//        disableGpsLocationListener(trackLocationListener);
//        recordingState = RecordingState.Idle;
//        imRecording.setVisibility(View.GONE);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            fabStartStop.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_new, this.getTheme()));
//        } else {
//            fabStartStop.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_new));
//        }
//        fabStartCamera.setVisibility(View.INVISIBLE);
//
//        uploadedTrip = null;
//        stopService();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        setUpMapIfNeeded();
//    }
//
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (recordingState == RecordingState.Recording) {
//            Toast.makeText(GpsRecMain.this, getString(R.string.StillRecording), Toast.LENGTH_SHORT)
//                    .show();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        if (recordingState == RecordingState.Recording) {
//            stopRecording();
//            tripManager.saveTrip();
//        }
//
//        disableGpsLocationListener(trackLocationListener);
//        disableLocationListener(fixListener);
//        timerManager.pauseTimer();
//
//        try {
//            Utils.deleteCache(GpsRecMain.this);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//            stopService(serviceIntent);
//            unbindService(mConnection);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        Toast.makeText(GpsRecMain.this,
//                        getResources().getString(R.string.Goodbye), Toast.LENGTH_SHORT)
//                .show();
//
//        super.onDestroy();
//
//
//    }
//
//
//    @Override
//    public void finish() {
//        if (recordingState == RecordingState.Recording) {
//            Intent setIntent = new Intent(Intent.ACTION_MAIN);
//            setIntent.addCategory(Intent.CATEGORY_HOME);
//            startActivity(setIntent);
//        } else {
//            super.finish();
//        }
//    }
//
//
//    private void enableGPSLocationListener(LocationListener listener) {
//        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIME_INTERVAL,
//                0, listener);
//    }
//
//    private void disableGpsLocationListener(LocationListener listener) {
//        try {
//            lm.removeUpdates(listener);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void enableLocationListener(
//            LocationListener listener) {
//        lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null);
//    }
//
//    private void disableLocationListener(
//            LocationListener listener) {
//        try {
//            lm.removeUpdates(listener);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.gps_recorder_menus, menu);
//        return true;
//    }
//
//    @SuppressWarnings("deprecation")
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_trips:
//                Intent tripListIntent = new Intent(this, GpsRecTripsList.class);
//                startActivityForResult(tripListIntent,
//                        getResources().getInteger(R.integer.GPS_TRIPS_LIST));
//                break;
//            case R.id.action_settigns:
//                Intent settings_intent = new Intent(this, GpsRecPrefs.class);
//                startActivity(settings_intent);
//                break;
//            case R.id.action_license:
//                Intent license_intent = new Intent(this, GpsRecLicense.class);
//                startActivity(license_intent);
//                break;
//            case R.id.action_musicPlayer:
//                try {
//                    Intent intent = new Intent(
//                            MediaStore.INTENT_ACTION_MUSIC_PLAYER);
//                    startActivity(intent);
//                } catch (ActivityNotFoundException e) {
//                    Toast.makeText(GpsRecMain.this,
//                            getResources().getString(R.string.noMusicPlayer),
//                            Toast.LENGTH_LONG).show();
//                }
//                break;
//        }
//        return true;
//    }
//
//    LocationListener trackLocationListener = location -> tripManager.updateLocation(location);
//
//    protected void updateDisplay() {
////        speedDisplayer.displayData(tvSpeed, routeManager.getSpeed());
////        distanceDisplayer.displayData(tvDistance, routeManager.getDistance());
////        altitudeDisplayer.displayData(tvAltitude, routeManager.getAltitude());
////
////        if (recordingService != null && serviceBounded) {
////            recordingService.updateService(routeManager.getDistance());
////        }
//
//    }
//
//
//    /**
//     * Alert in case GPS is disabled!
//     */
//    private void buildAlertMessageNoGPS() {
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(getResources().getString(R.string.NoGPSHead));
//        builder.setMessage(getResources().getString(R.string.NoGPSBody))
//                .setCancelable(false)
//                .setPositiveButton(getResources().getString(R.string.YES),
//                        (dialog, id) -> startActivity(new Intent(
//                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
//                .setNegativeButton(getResources().getString(R.string.NO),
//                        (dialog, id) -> {
//                            Toast.makeText(
//                                    GpsRecMain.this,
//                                    getString(R.string.GpsMustEnabled),
//                                    Toast.LENGTH_LONG).show();
//                            dialog.cancel();
//                        });
//        final AlertDialog alert = builder.create();
//        alert.show();
//    }
//
//
//    private void setUpMapIfNeeded() {
//        // Do a null check to confirm that we have not already instantiated the
//        // map.
//        if (mapFrag == null) {
//            Log.i(TAG, "setUpMapIfNeeded: initializing map");
//            mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//            // Check if we were successful in obtaining the map.
//            if (mapFrag != null) {
//                mapFrag.getMapAsync(this);
//            }
//        }
//    }
//
//    public void saveTripAlertDialog() {
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
//                GpsRecMain.this);
//
//        // set title
//        alertDialogBuilder.setTitle(getResources()
//                .getString(R.string.SAVE_TRIP));
//
//        // set dialog message
//        alertDialogBuilder
//                .setMessage(getResources().getString(R.string.SaveDialog))
//                .setCancelable(true)
//                .setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, this.getTheme()))
//                .setPositiveButton(getResources().getString(R.string.YES),
//                        (dialog, id) -> {
//                            dialog.dismiss();
//                            saveTrip();
//                        })
//                .setNegativeButton(getResources().getString(R.string.NO),
//                        (dialog, id) -> {
//                            stopRecording();
//                            dialog.dismiss();
//                        });
//
//        // create alert dialog
//        AlertDialog alertDialog = alertDialogBuilder.create();
//
//        alertDialog.setOnCancelListener(saveDialogCancelListener);
//        // show it
//        alertDialog.show();
//    }
//
//    private void saveTrip() {
//        stopRecording();
//        savingTripPd.setTitle(getString(R.string.app_name));
//        savingTripPd.setMessage(getString(R.string.SavingTrip));
//        savingTripPd.setCancelable(true);
//        savingTripPd.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, this.getTheme()));
//        savingTripPd.show();
//        new SaveTripTask().execute("");
//    }
//
//    OnCancelListener pdOnCancelListener = dialog -> {
//        Toast.makeText(GpsRecMain.this, getString(R.string.Canceled), Toast.LENGTH_SHORT).show();
//        stopRecording();
//    };
//
//    OnCancelListener saveDialogCancelListener = dialog ->
//        fabStartStop.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_new, getTheme()));
//
//
//    /* Called from ErrorDialogFragment when the dialog is dismissed. */
//
//    @Override
//    public void onMapLoaded() {
//        //Nothing to do in this method
//    }
//
//    @Override
//    public void onMapReady(GoogleMap googleMap) {
//        Log.i(TAG, "onMapReady: ");
//        mMap = googleMap;
//        mapHelper.initMap(mMap);
//        mapHelper.setLineWidth(lineWidth);
//        updatePreferences();
//        moveMapToInitialPosition();
//    }
//
//    private void moveMapToInitialPosition() {
//        String path = null;
//
//        try {
//            path = getIntent().getStringExtra("kml_path");
//        } catch (NullPointerException e) {
//            e.printStackTrace();
//        }
//
//        if (path != null) { //this means that app started by tap on kml file!
//            //Todo refactor trip uploading
////            uploadedTrip = new Trip(path, null, 0, 0);
////            UploadTripTask uploadTripTask = new UploadTripTask();
////            uploadTripTask.execute(uploadedTrip);
////            recordingMode = LOADED_FROM_INTENT;
//        } else {
//            moveToLastKnownLocation();
//        }
//    }
//
//    /* A fragment to display an error dialog */
//    public static class ErrorDialogFragment extends DialogFragment {
//        public ErrorDialogFragment() {
//        }
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//            // Get the error code and retrieve the appropriate dialog
//            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
//            return GooglePlayServicesUtil.getErrorDialog(errorCode,
//                    this.getActivity(), REQUEST_RESOLVE_ERROR);
//        }
//
//        @Override
//        public void onDismiss(DialogInterface dialog) {
//        }
//    }
//
//    private void moveToLastKnownLocation() {
//        String networkProvider = LocationManager.NETWORK_PROVIDER;
//        lastKnownLocation = lm.getLastKnownLocation(networkProvider);
//        if (lastKnownLocation != null) {
//            mapHelper.setZoom(cameraZoom);
//            mapHelper.setLocation(lastKnownLocation);
//        }
//    }
//
//    LocationListener fixListener = new LocationListener() {
//        @Override
//        public void onLocationChanged(@NonNull Location location) {
//
//            lm.removeUpdates(this);
//            if (gpsPd.isShowing()) {
//                gpsPd.dismiss();
//            }
//            startService();
//
//            enableGPSLocationListener(trackLocationListener);
//            tripManager.setCurrentLocation(location);
//            mapHelper.setLocation(location);
//
//            if (recordingState == RecordingState.Recording) {
//                if (recordingMode == CONTINUE_TRIP) {
//                    timerManager.resumeTimer();
//                } else {
//                    timerManager.startTimer();
//                }
//            }
//            Toast.makeText(GpsRecMain.this,
//                    getResources().getString(R.string.LocationFounded),
//                    Toast.LENGTH_LONG).show();
//        }
//    };
//
//    OnSharedPreferenceChangeListener prefListener = (sharedPreferences, key) -> updatePreferences();
//
//    private class SaveTripTask extends AsyncTask<String, Void, String> {
//
//        @Override
//        protected String doInBackground(String... params) {
//            String result = new String();
//            SaveStatus status = tripManager.saveTrip();
//            switch (status) {
//                case PASSED:
//                    result = getResources().getString(R.string.TripSaved);
//                    break;
//                case NOT_ENOUGH_DATA:
//                    result = getResources().getString(R.string.NotEnoughData);
//                    break;
//                default:
//                    break;
//            }
//            return result;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            savingTripPd.dismiss();
//            Toast.makeText(GpsRecMain.this, result, Toast.LENGTH_LONG).show();
//        }
//
//        @Override
//        protected void onPreExecute() {
//        }
//
//        @Override
//        protected void onProgressUpdate(Void... values) {
//        }
//    }
//
//
//    @Override
//    public void onBackPressed() {
//        if (recordingState == RecordingState.Recording) {
//            finish();
//        } else {
//            super.onBackPressed();
//        }
//    }
//
//
//    private void backPressedDialog() {
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
//        builder.setTitle(getResources().getString(R.string.app_name));
//        builder.setMessage(getResources().getString(R.string.StopAndExit))
//                .setCancelable(false)
//                .setPositiveButton(getResources().getString(R.string.YES),
//                        (dialog, id) -> finish())
//                .setNegativeButton(getResources().getString(R.string.NO),
//                        (dialog, id) -> dialog.cancel());
//        final AlertDialog alert = builder.create();
//        alert.show();
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        // Check which request we're responding to
//        if (requestCode == TRIP_LIST_ACTIVITY && resultCode == RESULT_OK && data.hasExtra("UploadedTrip")) {
//               // uploadedTrip = (Trip) data.getSerializableExtra("UploadedTrip");
//                if (uploadedTrip != null) {
//                    if (recordingState == RecordingState.Idle) {
//                        new UploadTripTask().execute(uploadedTrip);
//                    } else {
//                        Toast.makeText(this, getString(R.string.upload_while_recording), Toast.LENGTH_LONG).show();
//                    }
//                }
//            }
//
//
//        if (requestCode == CAMERA_INTENT_ACTIVITY && resultCode == RESULT_OK) {
//                      Uri capturedImageUri = Uri.fromFile(mImageMarkerFileLocation);
//
//            try {
//                tripManager.addImageMarker(capturedImageUri);
//            } catch (Exception e) {
//                e.printStackTrace();
//                Toast.makeText(GpsRecMain.this, "There was an error, please try again...", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    private class UploadTripTask extends AsyncTask<Trip, Void, String> {
//        @Override
//        protected void onPreExecute() {
//            loadingTripPd = new ProgressDialog(GpsRecMain.this);
//            loadingTripPd.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
//            loadingTripPd.setTitle(getString(R.string.app_name));
//            loadingTripPd.setMessage(getString(R.string.displaying_trip));
//            loadingTripPd.setCancelable(true);
//            loadingTripPd.show();
//        }
//
//        @Override
//        protected String doInBackground(Trip... params) {
//            Trip trip = params[0];
//            if (Utils.isFileExists(trip.getKml())) {
//
//                int status = tripManager.uploadTrip(trip);
//                if (status != KML_OPENED) {
//                    return FAILED;
//                }
//            } else {
//                return FAILED;
//            }
//            return PASSED;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            loadingTripPd.dismiss();
//            if (result.equals(PASSED)) {
//                loadingTripPd = null;
//                Toast.makeText(GpsRecMain.this, getResources().getString(R.string.TripLoaded),
//                        Toast.LENGTH_LONG).show();
//                updateDisplay();
//            } else {
//                uploadedTrip = null;
//                Toast.makeText(GpsRecMain.this, R.string.unable_to_open_kml,
//                        Toast.LENGTH_LONG).show();
//
//            }
//        }
//
//        @Override
//        protected void onProgressUpdate(Void... values) {
//            //TODO - Remove all AsyncTasks
//        }
//    }
//
//    private void loadedTripDialog() {
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, getTheme()));
//        builder.setTitle(getResources().getString(R.string.app_name));
//        builder.setMessage(getResources().getString(R.string.ContinueLoadedTrip))
//                .setCancelable(false)
//                .setPositiveButton(getResources().getString(R.string.YES),
//                        (dialog, id) -> {
//                            recordingMode = CONTINUE_TRIP;
//                            startRecording();
//                        })
//                .setNegativeButton(getResources().getString(R.string.NO),
//                        (dialog, id) -> {
//                            recordingMode = FOLLOW_TRIP;
//                            mapHelper.clearMarkers();
//                            startRecording();
//                        });
//        final AlertDialog alert = builder.create();
//        alert.show();
//    }
//
//    private ServiceConnection mConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName className,
//                                       IBinder service) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance
//            GpsTripRecService.LocalBinder binder = (GpsTripRecService.LocalBinder) service;
//            recordingService = binder.getService();
//            serviceBounded = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            serviceBounded = false;
//        }
//    };
//
//    public void startService() {
//        startService(serviceIntent);
//        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
//    }
//
//    public void stopService() {
//
//        try {
//            stopService(serviceIntent);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//            unbindService(mConnection);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        serviceBounded = false;
//    }
//
//    private void takePhoto() {
//
//        try {
//            createImageFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//            Toast.makeText(GpsRecMain.this, "Couldn't create photo file...", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        Uri imageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", mImageMarkerFileLocation);
//        Intent cameraIntent = new Intent();
//        cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
//        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//        startActivityForResult(cameraIntent, CAMERA_INTENT_ACTIVITY);
//
//    }
//
//    private void createImageFile() throws IOException {
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "Image_" + timeStamp + "_";
//        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//
//        mImageMarkerFileLocation = File.createTempFile(imageFileName, ".jpg", storageDirectory);
//    }
//
//}
