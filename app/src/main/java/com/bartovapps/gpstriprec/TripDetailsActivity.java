package com.bartovapps.gpstriprec;

import static com.bartovapps.gpstriprec.core.db.TripsDBOpenHelper.COLUMN_FROM;
import static com.bartovapps.gpstriprec.core.db.TripsDBOpenHelper.COLUMN_TO;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bartovapps.gpstriprec.core.db.TripsDataSource;
import com.bartovapps.gpstriprec.core.di.QMainThread;
import com.bartovapps.gpstriprec.core.map_helper.ImageMarker;
import com.bartovapps.gpstriprec.core.map_helper.MapHelper;
import com.bartovapps.gpstriprec.displayers.DataDisplayer;
import com.bartovapps.gpstriprec.displayers.FeetAltDisplayer;
import com.bartovapps.gpstriprec.displayers.HmsDisplayer;
import com.bartovapps.gpstriprec.displayers.KmhDisplayer;
import com.bartovapps.gpstriprec.displayers.MetricAltDisplayer;
import com.bartovapps.gpstriprec.displayers.MetricDisplayer;
import com.bartovapps.gpstriprec.displayers.MileageDisplayer;
import com.bartovapps.gpstriprec.displayers.MphDisplayer;
import com.bartovapps.gpstriprec.displayers.TimeDisplayer;
import com.bartovapps.gpstriprec.enums.AltUnits;
import com.bartovapps.gpstriprec.enums.Units;
import com.bartovapps.gpstriprec.kmlhleper.KmlParser;
import com.bartovapps.gpstriprec.utils.Utils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import data.model.Trip;

@AndroidEntryPoint
public class TripDetailsActivity extends AppCompatActivity implements GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener, OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback {
    private static final String TAG = "TAG_TripDetailsActivity";
    private static final String GOOGLE_EARTH_PACKAGE = "com.google.earth";
    private static final String GOOGLE_EARTH_STORE_URI = "https://play.google.com/store/apps/details?id=com.google.earth";
    private static final String GOOGLE_EARTH_KML_ARG = "application/vnd.googleearth.kml+xml";

    private static final int GALLERY_ACTIVITY_REQ = 100;
    private static final int SHARE_IMAGE = 0;
    private static final int SHARE_KML = 1;
    private static final String LOG_TAG = TripDetailsActivity.class.getSimpleName();
    private static final float CAM_ZOOM = 15f;
    private GoogleMap mMap;
    Context context = this;
    @Inject
    MapHelper mapHelper;

    @Inject
    KmlParser parser;
    private List<LatLng> locations;
    private SharedPreferences settings;
    private int lineColor = Color.RED;
    private float lineWidth = 5;
    private int mapType = GoogleMap.MAP_TYPE_NORMAL;
    String mapKmlFileName;
    private Trip trip;
    Bundle mB;
    ProgressDialog progressDialog;
    Toolbar toolbar;

    String date;
    float distance;
    double averageSpeed;
    double averageMoveSpeed;
    double maxSpeed;
    double maxAltitude;
    long duration;
    long moveTime;
    long stopTime;
    String startAddress;
    String stopAddress;
    String mapFileName;
    String tripTitle;

    TextView tvWhen;
    TextView tvDuration;
    TextView tvDistance;
    TextView tvAvSpeed;
    TextView tvMaxSpeed;
    TextView tvFrom;
    TextView tvTo;
    TextView tvMaxAltitude;
    TextView tvAverageMoveSpeed;
    TextView tvMoveTime;
    TextView tvStopTime;
    TextView tvTripDetailsHead;

    DataDisplayer speedDisplayer;
    DataDisplayer moveSpeedDisplayer;
    DataDisplayer distanceDisplayer;
    DataDisplayer altitudeDisplayer;
    TimeDisplayer timeDisplayer;

    Units units = Units.Metric;
    AltUnits altUnits = AltUnits.Feet;

    @Inject
    @QMainThread
    Handler handler;

    @Inject
    TripsDataSource tripsDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		Log.i(LOG_TAG, "onCreate called");
        setContentView(R.layout.trip_details_activity);
        toolbar = findViewById(R.id.app_bar);
        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
            // WTF SAMSUNG!
        }
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_back);
        locations = new ArrayList<>();
        settings = PreferenceManager.getDefaultSharedPreferences(context);

        updatePreferences();
        setDisplayers();

        handler = new Handler();
        //trip =  getIntent().getSerializableExtra("trip", Trip.serializer());


        if (trip != null) {
            mapKmlFileName = trip.getKml();
//            Log.i(LOG_TAG, "MAP FileName: " + mapKmlFileName);
            initDisplayComponents();
            getTripDetails();
        } else {
//            Log.i(LOG_TAG, "Cannot find map file: " + mapKmlFileName);
            Toast.makeText(context, getResources().getString(R.string.MapUnavailable), Toast.LENGTH_LONG).show();
        }

//	    AdBuddiz.showAd(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        if (item.getItemId() == R.id.action_googleErath) {
            startGoogleEarth();
        }

        if (item.getItemId() == R.id.action_share) {
            String filePath = new File(trip.getKml()).getParent() + "/sharing";
            File parent = new File(filePath);
            final String sharedImage = parent.toString() + "/sharedMap.png";
            if (!parent.exists()) {
                parent.mkdirs();
            }
            shareMapDialog(sharedImage);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume:");

        if (Utils.isFileExists(mapKmlFileName)) {
            setUpMapIfNeeded();
        } else {
//            Log.i(LOG_TAG, "Cannot find map file: " + mapKmlFileName);
            Toast.makeText(context, getResources().getString(R.string.MapUnavailable), Toast.LENGTH_LONG).show();
        }
        if (trip != null) {
            updateDisplay();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.trip_details_menu, menu);
        return true;
    }

    private void updatePreferences() {
        int color = Integer.parseInt(settings.getString(getResources().getString(R.string.LineColorPref), "1"));
        this.lineWidth = Float.parseFloat(settings.getString(getResources().getString(R.string.LineWidthPref), "5"));

        int speedUnits = Integer.parseInt(settings.getString(getResources()
                .getString(R.string.units), "1"));
        int altUnits = Integer.parseInt(settings.getString(getResources().getString(R.string.altitudeUnitsKey), "1"));

        switch (speedUnits) {
            case 1:
                this.units = Units.Metric;
                break;
            case 2:
                this.units = Units.Mileage;
                break;
        }

        switch (altUnits) {
            case 1:
                this.altUnits = AltUnits.Feet;
                break;
            case 2:
                this.altUnits = AltUnits.Metric;
                break;
        }

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
    }

    @SuppressLint("NewApi")
    private boolean initMap() {
        if (mMap == null) {
            MapFragment mapFrag = (MapFragment) getFragmentManager()
                    .findFragmentById(R.id.savedMap);
            mapFrag.getMapAsync(this);
        }

        return (mMap != null);

    }

    OnMapLoadedCallback mapLoaded = new OnMapLoadedCallback() {
        FileInputStream fis;

        @Override
        public void onMapLoaded() {

//				parser.openTripKml();
//				locations = parser.getTripLocations();
//				mapHelper.overlayRoute(locations, CAM_ZOOM);
//				new LongOperation().execute("");

/* New api using Google Map Utility library!
            try {
                fis = new FileInputStream(mapKmlFileName);
                KmlLayer layer = new KmlLayer(mMap, fis, getApplicationContext());
                layer.addLayerToMap();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }catch (XmlPullParserException e){
                e.printStackTrace();
            }
*/


//				if(trip.getImageFileName() == null || !(new File(trip.getImageFileName()).exists()) ){ //this is for previews versions, map thumb nail image will be created when first watched
////					Toast.makeText(GpsRecMapTab.this, "Can't find trip map image on database.. creating new snapshot..", Toast.LENGTH_SHORT).show();
//					new LongOperation().execute("");
//				}
        }
//		Log.i(LOG_TAG, "Received locations from file..");
    };

    @Override
    public void onMapLoaded() {
        Log.i(TAG, "onMapLoaded: ");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady: ");
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(mapLoaded);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mapHelper.setLineWidth(lineWidth);
        MapOverlayTask task = new MapOverlayTask();
        task.execute(mapKmlFileName);

    }


    private class TakeMapSnapshot extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mapHelper.saveMapAsImage(TripDetailsActivity.this, trip.getId());
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class MapOverlayTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
//            Log.i(TripDetailsActivity.LOG_TAG, "About to load trip #: " + trip.getId());
            progressDialog = new ProgressDialog(TripDetailsActivity.this);
            progressDialog.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
            progressDialog.setTitle(getString(R.string.app_name));
            progressDialog.setMessage(getString(R.string.displaying_trip));
            progressDialog.setCancelable(true);
            progressDialog.show();
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... params) {
            // locations = parser.getTripLocations();
            locations = parser.parsKmlString(params[0]);
//            mapHelper.overlayRoute(locations, CAM_ZOOM);
            mapHelper.overlayRoute(locations);
            tripsDataSource.open();
            List<ImageMarker> imageMarkers = tripsDataSource.findAllMarkersForTrip(trip.getId());
            tripsDataSource.close();

//            Log.i(TripDetailsActivity.LOG_TAG, "There are " + imageMarkers.size() + " imageMarkers for this trip");

            if (imageMarkers != null) {
                for (ImageMarker imageMarker : imageMarkers) {
                    mapHelper.addImageMarker(imageMarker, context);

                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (progressDialog.isShowing()) {
                progressDialog.setProgress(100);
                progressDialog.dismiss();
            }

            displayAddresses();

            if (trip.getImageFileName() == null || !(new File(trip.getImageFileName()).exists())) { //this is for previews versions, map thumb nail image will be created when first watched
//					Toast.makeText(GpsRecMapTab.this, "Can't find trip map image on database.. creating new snapshot..", Toast.LENGTH_SHORT).show();
                new TakeMapSnapshot().execute("");
            }
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (mMap == null && Utils.servicesOk(TripDetailsActivity.this)){
            MapFragment mapFrag = (MapFragment) getFragmentManager()
                    .findFragmentById(R.id.savedMap);
            mapFrag.getMapAsync(this);
            // Check if we were successful in obtaining the map.
        }else {
            Toast.makeText(TripDetailsActivity.this,
                    getResources().getString(R.string.UnableInitMap),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setUpMap() {
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
//        Toast.makeText(TripDetailsActivity.this, "Marker was clicked", Toast.LENGTH_SHORT).show();
//        Log.i(LOG_TAG, "Marker " + marker.getId() + " was clicked");
        System.gc();
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
//        Toast.makeText(TripDetailsActivity.this, "InfoWindow was clicked", Toast.LENGTH_SHORT).show();
//        Log.i(LOG_TAG, "Marker " + marker.getId() + " was clicked");
        ImageMarker imageMarker = mapHelper.getImageMarkerUri(marker.getId());

        if (imageMarker != null) {
            Intent galleryIntent = new Intent(TripDetailsActivity.this, GpsTripRecGallery.class);
            galleryIntent.setData(imageMarker.getImageUri());
            galleryIntent.putExtra("TripId", trip.getId());
            startActivityForResult(galleryIntent, GALLERY_ACTIVITY_REQ);
        }
        System.gc();
    }


    private void initDisplayComponents() {
        tvWhen =  findViewById(R.id.tvWhen);
        tvDuration =  findViewById(R.id.tvDurationDetails);
        tvAvSpeed =  findViewById(R.id.tvAveSpeed);
        tvMaxSpeed =  findViewById(R.id.tvMaxSpeed);
        tvDistance =  findViewById(R.id.tvDistanceDetails);
        tvFrom =  findViewById(R.id.tvFrom);
        tvTo =  findViewById(R.id.tvTo);
        tvMaxAltitude =  findViewById(R.id.tvMaxAltitude);
        tvAverageMoveSpeed =  findViewById(R.id.tvAverageMoveSpeed);
        tvMoveTime =  findViewById(R.id.tvMoveTime);
        tvStopTime =  findViewById(R.id.tvStopTime);
        tvTripDetailsHead =  findViewById(R.id.tvTripDetailsHead);
    }

    private void getTripDetails() {
        timeDisplayer = new HmsDisplayer();

        date = trip.getDate();
        distance = trip.getDistance();
        averageSpeed = trip.getAverageSpeed();
        averageMoveSpeed = trip.getMoveAverageSpeed();
        duration = trip.getDuration();
        moveTime = trip.getMoveTime();
        stopTime = trip.getStopTime();
        startAddress = trip.getStartAddress();
        stopAddress = trip.getStopAddress();
//        Log.i(LOG_TAG, "Start Address: " + startAddress + "\nStop Address: " + stopAddress);

        maxSpeed = trip.getMaxSpeed();
        mapFileName = trip.getKml();

        maxAltitude = trip.getMaxAlt();
        tripTitle = trip.getTripName();
//        Log.i(LOG_TAG, "data.model.Trip Title: " + tripTitle);

        if (tripTitle == null || "".equals(tripTitle)) {
            tripTitle = getString(R.string.RecordedDetailedTitle);
        }

    }

    private void setDisplayers() {
        if (this.units == Units.Mileage) {
            speedDisplayer = new MphDisplayer();
            moveSpeedDisplayer = new MphDisplayer();
            distanceDisplayer = new MileageDisplayer();
        } else {
            speedDisplayer = new KmhDisplayer();
            moveSpeedDisplayer = new KmhDisplayer();
            distanceDisplayer = new MetricDisplayer();
        }

        if (this.altUnits == AltUnits.Feet) {
            altitudeDisplayer = new FeetAltDisplayer();
        } else {
            altitudeDisplayer = new MetricAltDisplayer();
        }
    }

    private void updateDisplay() {
        tvWhen.setText("" + date);
        timeDisplayer.displayTime(tvDuration, duration);
        distanceDisplayer.displayData(tvDistance, distance);
        speedDisplayer.displayData(tvAvSpeed,  averageSpeed);
        speedDisplayer.displayData(tvMaxSpeed,  maxSpeed);
        altitudeDisplayer.displayData(tvMaxAltitude, maxAltitude);
        timeDisplayer.displayTime(tvMoveTime, moveTime);
        timeDisplayer.displayTime(tvStopTime, stopTime);
        tvTripDetailsHead.setText(tripTitle);

        if (averageMoveSpeed == 0) {
            tvAverageMoveSpeed.setText(getString(R.string.UnavailableData));
        } else {
            moveSpeedDisplayer.displayData(tvAverageMoveSpeed, averageMoveSpeed);
        }
    }

    private void displayAddresses() {
        if (startAddress == null || startAddress.contains("Unavailable") || startAddress.contains("none")) { // trying to get address if wasn't
            // Refactor after make TripManager singleton
           // startAddress = TripManager.getAddress(locations.get(0)).trim(); //getting first location address
//            Log.i(LOG_TAG, "Got start address: " + startAddress);

            if (startAddress.contains("Unavailable") || startAddress == null) { //if it still unavailable, might be location without address at all or no Internet at this time..
                startAddress = getString(R.string.UnavailableData);
            } else {
                updateDb(COLUMN_FROM, startAddress); //updating the database as address has been acquired!
            }
        } else {
//            Log.i(LOG_TAG, "Start Address from DataBase: " + startAddress);
        }


        if (stopAddress == null || stopAddress.contains("Unavailable") || stopAddress.contains("none")) { // trying to get address if wasn't
            // Todo, refactor after making Trip
           // stopAddress = TripManager.getAddress(locations.get(locations.size() - 1), context).trim(); //getting first location address
//            Log.i(LOG_TAG, "Got stop address: " + stopAddress);

            if (stopAddress.contains("Unavailable") || stopAddress == null) { //if it still unavailable, might be location without address at all or no Internet at this time..
                stopAddress = getString(R.string.UnavailableData);
            } else {
                updateDb(COLUMN_TO, stopAddress); //updating the database as address has been acquired!
            }
        } else {
//            Log.i(LOG_TAG, "Stop Address from DataBase: " + stopAddress);
        }

        tvFrom.setText(startAddress);
        tvTo.setText(stopAddress);

    }

    private void updateDb(String column, String data) { //if addresses succeed to acquire, db will be update accordingly
        tripsDataSource.open();
        tripsDataSource.updateTripData(trip.getId(), column, data);
        tripsDataSource.close();
    }


    private void startGoogleEarth() {
        boolean googleEarthInstalled = Utils.isPackageInstalled(GOOGLE_EARTH_PACKAGE, context);
        if (googleEarthInstalled) {
            if (!Utils.isFileExists(trip.getKml())) {
                Toast.makeText(TripDetailsActivity.this, getString(R.string.MapUnavailable), Toast.LENGTH_LONG).show();
                return;
            }

            File file = new File(trip.getKml());
            Uri earthURI = Uri.fromFile(file);

            Intent earthIntent = new Intent(android.content.Intent.ACTION_VIEW, earthURI);

//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(Uri.fromFile(file), GOOGLE_EARTH_KML_ARG);
//            intent.putExtra("com.google.earth.EXTRA.tour_feature_id", "my_track");
            try {
                startActivity(earthIntent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                googleEarthInstallDialog();
            }
        } else {
            googleEarthInstallDialog();
        }
    }

    private void googleEarthInstallDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set dialog message
        alertDialogBuilder
                .setMessage(getResources().getString(R.string.NoGoogleEarth))
                .setTitle(getResources().getString(R.string.app_name))
                .setIcon(getResources().getDrawable(R.drawable.ic_launcher))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                //Toast.makeText(context, "Google Earth is not installed on this device\nGoogle Erath is required for this action", Toast.LENGTH_LONG).show();
                                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_EARTH_STORE_URI));
                                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                try {
                                    startActivity(marketIntent);
                                } catch (ActivityNotFoundException e) {
                                    e.printStackTrace();
                                    Toast.makeText(TripDetailsActivity.this, getString(R.string.GooglePlayError), Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }


    private void shareMapDialog(final String mapFileName) {
        final CharSequence[] options = {getString(R.string.share_map_image), getString(R.string.share_kml)};

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set dialog message
        alertDialogBuilder
//                .setMessage(getString())
                .setTitle(getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setSingleChoiceItems(options, 0, null)
                .setPositiveButton(getResources().getString(R.string.YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
//                                Log.i(LOG_TAG, "Option selected: " + selectedPosition);
                                switch (selectedPosition) {
                                    case SHARE_IMAGE:
                                        takeMapHqSnapshot(mapFileName);
                                        break;
                                    case SHARE_KML:
                                        shareKml(trip.getKml());
                                        break;
                                }
                                dialog.cancel();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.NO),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        alertDialog.show();

    }


    private void takeMapHqSnapshot(final String imageFileName) {


        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                // TODO Auto-generated method stub
                shareImage(snapshot);

            }
        };
        mMap.snapshot(callback);

    }


    public void shareImage(Bitmap b) {
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_launcher);
        Bitmap image = Utils.overlay(b, icon, getString(R.string.app_name));

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "sharedTrip");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, trip.getTripName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        String path = new File(new File(trip.getKml()).getParent()) + "/sharing/sharedTrip.png";
        values.put(MediaStore.Images.Media.DATA, path);
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values);

        if (uri == null) {
//            Log.i(LOG_TAG, "uri is null");
            Uri filesUri = MediaStore.Files.getContentUri("external");
            String[] projection = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.TITLE};
            String selection = MediaStore.MediaColumns.DATA + " = ?";
            String[] args = {path};
            Cursor c = context.getContentResolver().query(filesUri, projection, selection, args, null);
            if (c != null && c.getCount() == 1) {
//                Log.i(LOG_TAG, "item already exists! getting the already exists uri...");
                c.moveToFirst();
                long rowId = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                String title = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE));
//                Log.i(LOG_TAG, "Title is: " + title);
                c.close();
                uri = MediaStore.Files.getContentUri("external", rowId);
//                Log.i(LOG_TAG, "refresh scan force uri=" + uri);
            } else {
//                Log.i(LOG_TAG, "Keep trying...");
            }

        }

//        Log.i(LOG_TAG, "About to save the image in path: " + uri.getPath());
        OutputStream outStream;
        try {
            outStream = getContentResolver().openOutputStream(uri);
            image.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
            image.recycle();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        share.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(share, getString(R.string.share_map_image)));

    }

    public void shareKml(String kmlPath) {
        Log.i(LOG_TAG, "About to share kml: " + kmlPath);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(getString(R.string.kml_mime_type));

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, getString(R.string.share_kml));
        values.put(MediaStore.Images.Media.DISPLAY_NAME, tripTitle.length() > 0 ? tripTitle + ".kml" : getString(R.string.app_name) + ".kml");
        values.put(MediaStore.Images.Media.MIME_TYPE, getString(R.string.kml_mime_type));
        //String path = new File(new File(trip.getKml()).getParent()) + "/sharing/sharedTrip.png";
        values.put(MediaStore.Images.Media.DATA, kmlPath);
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values);

        if (uri == null) {
//            Log.i(LOG_TAG, "uri is null"); //It means that this file was already in the MediaStore, therefore it can't inserted again. Need to get it's Uri based on the title.
            Uri filesUri = MediaStore.Files.getContentUri("external");
            String[] projection = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.TITLE, MediaStore.MediaColumns.DISPLAY_NAME};
            String selection = MediaStore.MediaColumns.DATA + " = ?";
            String[] args = {kmlPath};

            //Updating the display name every share (maybe the user changed the name of the trip)
            ContentValues updateContentValues = new ContentValues();
            updateContentValues.put(MediaStore.Images.Media.DISPLAY_NAME, tripTitle.length() > 0 ? tripTitle + ".kml" : getString(R.string.app_name) + ".kml");

            Cursor c = context.getContentResolver().query(filesUri, projection, selection, args, null);
            if (c != null && c.getCount() == 1) {
//                Log.i(LOG_TAG, "item already exists! getting the already exists uri...");
                c.moveToFirst();
                long rowId = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                c.close();
                uri = MediaStore.Files.getContentUri("external", rowId);
//                Log.i(LOG_TAG, "refresh scan force uri=" + uri);
            } else {
//                Log.i(LOG_TAG, "Keep trying...");
            }

        }

//        Log.i(LOG_TAG, "About to save the image in path: " + uri.getPath());


        share.putExtra(Intent.EXTRA_STREAM, uri);
        // share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + ", " + tripTitle);
        startActivity(Intent.createChooser(share, getString(R.string.share_kml)));

    }


}
