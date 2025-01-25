package com.bartovapps.gpstriprec.presentation.screens.main_screen

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.bartovapps.gpstriprec.GpsRecLicense
import com.bartovapps.gpstriprec.GpsRecPrefs
import com.bartovapps.gpstriprec.GpsRecTripsList
import com.bartovapps.gpstriprec.R
import com.bartovapps.gpstriprec.core.timer.TripTimer
import com.bartovapps.gpstriprec.core.trip_manager.TripState
import com.bartovapps.gpstriprec.data.enums.AltitudeUnits
import com.bartovapps.gpstriprec.data.enums.RecordingState
import com.bartovapps.gpstriprec.data.enums.SaveStatus
import com.bartovapps.gpstriprec.data.enums.Units
import com.bartovapps.gpstriprec.displayers.FeetAltDisplayer
import com.bartovapps.gpstriprec.displayers.MetricDisplayer
import com.bartovapps.gpstriprec.displayers.MileageDisplayer
import com.bartovapps.gpstriprec.displayers.MphDisplayer
import com.bartovapps.gpstriprec.kmlhleper.KmlParserImpl
import com.bartovapps.gpstriprec.presentation.displayers.DataDisplayer
import com.bartovapps.gpstriprec.presentation.displayers.KmhDisplayer
import com.bartovapps.gpstriprec.presentation.displayers.MetricAltDisplayer
import com.bartovapps.gpstriprec.presentation.map.CustomSupportMapFragment
import com.bartovapps.gpstriprec.presentation.map.MapReadyCallback
import com.bartovapps.gpstriprec.services.GpsTripRecService
import com.bartovapps.gpstriprec.services.GpsTripRecService.LocalBinder
import com.bartovapps.gpstriprec.utils.Utils
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.floatingactionbutton.FloatingActionButton
import core.trip_manager.TripManager
import dagger.hilt.android.AndroidEntryPoint
import data.model.Trip
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class MainScreen : AppCompatActivity(), MapReadyCallback {
    private var cameraZoom = 15f
    private var lineWidth = 5f
    private lateinit var tvSpeed: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvAltitude: TextView

    //    private ToggleButton btStartStop;
    private lateinit var fabStartStop: FloatingActionButton
    private lateinit var fabStartCamera: FloatingActionButton

    @Inject
    lateinit var tripManager: TripManager

    @Inject
    lateinit var timerManager: TripTimer

    private lateinit var speedDisplayer: DataDisplayer
    private lateinit var distanceDisplayer: DataDisplayer
    private lateinit var altitudeDisplayer: DataDisplayer

    private lateinit var lm: LocationManager
    private lateinit var settings: SharedPreferences
    private var units = Units.Metric
    private var autoSave = AUTO_SAVE
    private var altUnits = AltitudeUnits.Feet
    private var lineColor = Color.RED
    private val mapType = GoogleMap.MAP_TYPE_NORMAL
    private var recordingMode = NEW_TRIP


    var gpsPd: ProgressDialog? = null
    var savingTripPd: ProgressDialog? = null
    var loadingTripPd: ProgressDialog? = null

    private var recordingState = RecordingState.Idle
    private var imRecording: ImageView? = null
    var handler: Handler = Handler(Looper.getMainLooper())

    // Bool to track whether the app is already resolving an error
    private var uploadedTrip: Trip? = null
    var recordingService: GpsTripRecService? = null
    var serviceBounded: Boolean = false
    var serviceIntent: Intent? = null

    lateinit var mapFrag: CustomSupportMapFragment

    private var toolbar: Toolbar? = null

    private var job: Job? = null

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gps_recorder_main_material)

        toolbar = findViewById(R.id.app_bar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setLogo(R.drawable.ic_launcher)
            setDisplayShowTitleEnabled(false)
        }

        settings = this.getSharedPreferences("GPS_TRIP_RECORDER", MODE_PRIVATE)
        settings.registerOnSharedPreferenceChangeListener(prefListener)
        handler = Handler(Looper.getMainLooper())
        lm = getSystemService(LOCATION_SERVICE) as LocationManager
        gpsPd = ProgressDialog(this)
        savingTripPd = ProgressDialog(this)
        serviceIntent = Intent(this, GpsTripRecService::class.java)

        // Create the interstitial.
        setUpMapIfNeeded()
        setUiComponents()
        subscribeTimerChanges()
    }

    private fun observeTripState() {
        job?.cancel()
        job = lifecycleScope.launch {
            tripManager.tripStateFlow.collect {
                Log.i(TAG, "onTripStateChanged: $it")
                processTripState(it)
            }
        }
    }

    private fun processTripState(state: TripState) {
        when (state) {
            TripState.Initiated -> mapFrag.clearEverything()
            is TripState.NewImageMarker -> mapFrag.addImageMarker(state.imageMarker)
            is TripState.NewLocation -> mapFrag.moveToLocationAndDrawLine(state.location)
            TripState.OnGoing -> mapFrag.mapCameraCloseup()
            is TripState.OverlayRoute -> TODO()
            TripState.Stopped -> mapFrag.mapCameraLongShot()
            is TripState.TripSaved -> mapFrag.fitCameraToRoute(state.locations)
            is TripState.StartLocation -> mapFrag.goToLocation(state.location)
        }
    }

    private fun subscribeTimerChanges() {
        timerManager.subscribeTimerChanges() //todo implement timer observer
    }


    // Invoke displayInterstitial() when you are ready to display an
    // interstitial.
    private fun updatePreferences() {
        val units = settings.getString(
            resources
                .getString(R.string.units), "1"
        )?.toInt()
        val color = settings.getString(
            resources
                .getString(R.string.LineColorPref), "1"
        )?.toInt()
        val zoom = settings.getString(
            resources
                .getString(R.string.ZoomPref), "15"
        )?.toFloat()
        val width = settings.getString(
            resources
                .getString(R.string.LineWidthPref), "5"
        )?.toFloat()
        this.autoSave = settings.getString(getString(R.string.AutoSavePrefKey), "0")!!
            .toInt()

        val altitudeUnits = settings.getString(
            resources
                .getString(R.string.altitudeUnitsKey), "1"
        )!!.toInt()

        if (units == 2) {
            this.units = Units.Millage
        } else {
            this.units = Units.Metric
        }

        if (altitudeUnits == 1) {
            this.altUnits = AltitudeUnits.Feet
        } else {
            this.altUnits = AltitudeUnits.Metric
        }

        setDisplayers()

        when (color) {
            2 -> this.lineColor = Color.GREEN
            3 -> this.lineColor = Color.YELLOW
            4 -> this.lineColor = Color.BLUE
            else -> this.lineColor = Color.RED
        }

        this.lineWidth = width!!
        this.cameraZoom = zoom!!

        mapFrag.let {
            it.lineWidth = width
            it.zoom = zoom
            it.lineColor = lineColor
        }
    }

    private fun setDisplayers() {
        if (units == Units.Millage) {
            speedDisplayer = MphDisplayer()
            distanceDisplayer = MileageDisplayer()
        } else {
            speedDisplayer = KmhDisplayer()
            distanceDisplayer = MetricDisplayer()
        }

        altitudeDisplayer = if (altUnits == AltitudeUnits.Feet) {
            FeetAltDisplayer()
        } else {
            MetricAltDisplayer()
        }
    }

    private fun setUiComponents() {
        tvSpeed = findViewById(R.id.tvSpeed)
        tvDistance = findViewById(R.id.tvDistance)
        tvTimer = findViewById(R.id.tvTimer)
        tvAltitude = findViewById(R.id.tvAltitude)


        //btStartStop = (ToggleButton) findViewById(R.id.btStartStop);
        fabStartStop = findViewById(R.id.fabStartStop)
        fabStartCamera = findViewById(R.id.fabCamera)


        // tvSpeed.setTypeface(typeFace);
        // tvDistance.setTypeface(typeFace);
        // tvTimer.setTypeface(typeFace);
        // tvAccuracy.setTypeface(typeFace);

//        btStartStop.setOnClickListener(btListener);
        fabStartStop.setOnClickListener(btListener)
        fabStartCamera.setOnClickListener(btListener)
        imRecording = findViewById<View>(R.id.imRecording) as ImageView
    }

    private var btListener: View.OnClickListener = View.OnClickListener { view: View ->
        if (view === fabStartStop) {
            if (recordingState == RecordingState.Idle) {
                if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    fabStartStop.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources, R.drawable.ic_action_new,
                            theme
                        )
                    )
                    buildAlertMessageNoGPS()
                } else {
                    if (uploadedTrip != null) {
                        if (recordingMode == LOADED_FROM_INTENT) {
                            startRecording()
                        } else {
                            loadedTripDialog()
                        }
                    } else {
                        recordingMode = NEW_TRIP
                        startRecording()
                    }
                }
            } else {
                if (autoSave == AUTO_SAVE) {
                    saveTrip()
                } else {
                    saveTripAlertDialog()
                }
            }
        }
        if (view === fabStartCamera) {
            //                Intent camIntent = new Intent(GpsRecMain.this, GpsTripRecCamera.class);
//                startActivityForResult(camIntent, getResources().getInteger(R.integer.GPS_CAMERA_ACTIVITY));

            takePhoto()
        }
    }


    private fun startRecording() {
        fabStartStop.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources, R.drawable.ic_action_stop,
                this.theme
            )
        )
        fabStartCamera.visibility = View.VISIBLE
        when (recordingMode) {
            CONTINUE_TRIP -> {}
            FOLLOW_TRIP -> tripManager.resetRoute(false)
            else -> tripManager.resetRoute(true)
        }

        enableLocationListener(fixListener)
        mapFrag.zoom = cameraZoom
        mapFrag.tilt = 0f
        updateDisplay()
        gpsPd?.apply {
            setCancelable(true)
            setOnCancelListener(pdOnCancelListener)
            setIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_launcher, theme))
            setTitle(getString(R.string.app_name))
            setMessage(getString(R.string.WaitForGPS))
            show()
        }

        recordingState = RecordingState.Recording
        imRecording?.visibility = View.VISIBLE
    }

    private fun stopRecording() {
        timerManager.stopTimer()
        disableLocationListener(fixListener)
        disableGpsLocationListener(trackLocationListener)
        recordingState = RecordingState.Idle
        imRecording?.visibility = View.GONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fabStartStop.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_action_new,
                    this.theme
                )
            )
        } else {
            fabStartStop.setImageDrawable(resources.getDrawable(R.drawable.ic_action_new))
        }
        fabStartCamera.visibility = View.INVISIBLE

        uploadedTrip = null
        stopService()
    }

    override fun onResume() {
        super.onResume()
        setUpMapIfNeeded()
        observeTripState()
    }


    override fun onStop() {
        super.onStop()
        if (recordingState == RecordingState.Recording) {
            Toast.makeText(this, getString(R.string.StillRecording), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private val fixListener = object : LocationListener{
        override fun onLocationChanged(location: Location) {
            disableGpsLocationListener(this)
            if (gpsPd?.isShowing == true) {
                gpsPd?.dismiss()
            }
            startService()
            enableGPSLocationListener(trackLocationListener)
            tripManager.setCurrentLocation(location)

            if (recordingState == RecordingState.Recording) {
                if (recordingMode == CONTINUE_TRIP) {
                    timerManager.resumeTimer()
                } else {
                    timerManager.startTimer()
                }
            }
            Toast.makeText(
                this@MainScreen,
                resources.getString(R.string.LocationFounded),
                Toast.LENGTH_LONG
            ).show()

        }

    }


    override fun onDestroy() {
        if (recordingState == RecordingState.Recording) {
            stopRecording()
            tripManager.saveTrip()
        }

        disableGpsLocationListener(trackLocationListener)
        disableLocationListener(fixListener)
        timerManager.pauseTimer()

        try {
            Utils.deleteCache(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            stopService(serviceIntent)
            unbindService(mConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(
            this,
            resources.getString(R.string.Goodbye), Toast.LENGTH_SHORT
        )
            .show()

        super.onDestroy()
    }


    override fun finish() {
        if (recordingState == RecordingState.Recording) {
            val setIntent = Intent(Intent.ACTION_MAIN)
            setIntent.addCategory(Intent.CATEGORY_HOME)
            startActivity(setIntent)
        } else {
            super.finish()
        }
    }


    private fun enableGPSLocationListener(listener: LocationListener) {
        lm.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, TIME_INTERVAL,
            0f, listener
        )
    }

    private fun disableGpsLocationListener(listener: LocationListener) {
        try {
            lm.removeUpdates(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enableLocationListener(
        listener: LocationListener
    ) {
        lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
    }

    private fun disableLocationListener(
        listener: LocationListener
    ) {
        try {
            lm.removeUpdates(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.gps_recorder_menus, menu)
        return true
    }

    @Suppress("deprecation")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_trips -> {
                val tripListIntent = Intent(this, GpsRecTripsList::class.java)
                startActivityForResult(
                    tripListIntent,
                    resources.getInteger(R.integer.GPS_TRIPS_LIST)
                )
            }

            R.id.action_settigns -> {
                val settings_intent = Intent(this, GpsRecPrefs::class.java)
                startActivity(settings_intent)
            }

            R.id.action_license -> {
                val license_intent = Intent(this, GpsRecLicense::class.java)
                startActivity(license_intent)
            }

            R.id.action_musicPlayer -> try {
                val intent = Intent(
                    MediaStore.INTENT_ACTION_MUSIC_PLAYER
                )
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    this,
                    resources.getString(R.string.noMusicPlayer),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        return true
    }

    var trackLocationListener: LocationListener =
        LocationListener { location: Location ->
            tripManager.updateLocation(
                location
            )
        }

    private fun updateDisplay() {
//        speedDisplayer.displayData(tvSpeed, routeManager.getSpeed());
//        distanceDisplayer.displayData(tvDistance, routeManager.getDistance());
//        altitudeDisplayer.displayData(tvAltitude, routeManager.getAltitude());
//
//        if (recordingService != null && serviceBounded) {
//            recordingService.updateService(routeManager.getDistance());
//        }
    }


    /**
     * Alert in case GPS is disabled!
     */
    private fun buildAlertMessageNoGPS() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.NoGPSHead))
        builder.setMessage(resources.getString(R.string.NoGPSBody))
            .setCancelable(false)
            .setPositiveButton(
                resources.getString(R.string.YES)
            ) { dialog: DialogInterface?, id: Int ->
                startActivity(
                    Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    )
                )
            }
            .setNegativeButton(
                resources.getString(R.string.NO)
            ) { dialog: DialogInterface, id: Int ->
                Toast.makeText(
                    this,
                    getString(R.string.GpsMustEnabled),
                    Toast.LENGTH_LONG
                ).show()
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }


    private fun setUpMapIfNeeded() {
        Log.i(TAG, "setUpMapIfNeeded: initializing map")
        if (!this::mapFrag.isInitialized) {
            mapFrag = supportFragmentManager.findFragmentById(R.id.map) as CustomSupportMapFragment
            mapFrag.lineWidth = lineWidth
            mapFrag.setMapReadyCallback(this)
        }
    }

    private fun saveTripAlertDialog() {
        val alertDialogBuilder = AlertDialog.Builder(
            this
        )

        // set title
        alertDialogBuilder.setTitle(
            resources
                .getString(R.string.SAVE_TRIP)
        )

        // set dialog message
        alertDialogBuilder
            .setMessage(resources.getString(R.string.SaveDialog))
            .setCancelable(true)
            .setIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_launcher, this.theme))
            .setPositiveButton(
                resources.getString(R.string.YES)
            ) { dialog: DialogInterface, id: Int ->
                dialog.dismiss()
                saveTrip()
            }
            .setNegativeButton(
                resources.getString(R.string.NO)
            ) { dialog: DialogInterface, id: Int ->
                stopRecording()
                dialog.dismiss()
            }

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        alertDialog.setOnCancelListener(saveDialogCancelListener)
        // show it
        alertDialog.show()
    }

    private fun saveTrip() {
        stopRecording()
        savingTripPd?.apply {
            setTitle(getString(R.string.app_name))
            setMessage(getString(R.string.SavingTrip))
            setCancelable(true)
            setIcon(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_launcher,
                    this@MainScreen.theme
                )
            )
            show()
        }
        SaveTripTask().execute("")
    }

    private var pdOnCancelListener: DialogInterface.OnCancelListener =
        DialogInterface.OnCancelListener { dialog: DialogInterface? ->
            Toast.makeText(this, getString(R.string.Canceled), Toast.LENGTH_SHORT).show()
            stopRecording()
        }

    private var saveDialogCancelListener: DialogInterface.OnCancelListener =
        DialogInterface.OnCancelListener { dialog: DialogInterface? ->
            fabStartStop.setBackgroundDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_action_new, theme
                )
            )
        }


    /* Called from ErrorDialogFragment when the dialog is dismissed. */


    override fun onMapReady() {
        Log.i(TAG, "onMapReady: ")
        val topPadding = findViewById<View>(R.id.llGauges).measuredHeight
        val bottomPadding = tvAltitude.measuredHeight
        mapFrag.setPadding(0, topPadding, 0, bottomPadding + 5)
        updatePreferences()
        moveMapToInitialPosition()
    }

    private fun moveMapToInitialPosition() {
        var path: String? = null

        try {
            path = intent.getStringExtra("kml_path")
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

        if (path != null) { //this means that app started by tap on kml file!
            //Todo refactor trip uploading
//            uploadedTrip = new Trip(path, null, 0, 0);
//            UploadTripTask uploadTripTask = new UploadTripTask();
//            uploadTripTask.execute(uploadedTrip);
//            recordingMode = LOADED_FROM_INTENT;
        } else {
            moveToLastKnownLocation()
        }
    }

    /* A fragment to display an error dialog */
    class ErrorDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle): Dialog? {
            // Get the error code and retrieve the appropriate dialog
            val errorCode = this.arguments.getInt(DIALOG_ERROR)
            return GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this.activity, REQUEST_RESOLVE_ERROR
            )
        }
    }

    private fun moveToLastKnownLocation() {
        val networkProvider = LocationManager.NETWORK_PROVIDER
        val lastKnownLocation = lm.getLastKnownLocation(networkProvider)
        lastKnownLocation?.let {
            mapFrag.zoom = cameraZoom
            mapFrag.goToLocation(lastKnownLocation)
        }
    }


    private fun removeLocationListener(listener: LocationListener) {
        lm.removeUpdates(listener)
    }

    private var prefListener: OnSharedPreferenceChangeListener =
        OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? -> updatePreferences() }

    private inner class SaveTripTask : AsyncTask<String?, Void?, String>() {
        override fun doInBackground(vararg params: String?): String {
            var result = ""
            val status = tripManager.saveTrip()
            when (status) {
                SaveStatus.PASSED -> result = resources.getString(R.string.TripSaved)
                SaveStatus.NOT_ENOUGH_DATA -> result = resources.getString(R.string.NotEnoughData)
                else -> {}
            }
            return result
        }

        override fun onPostExecute(result: String) {
            savingTripPd?.dismiss()
            Toast.makeText(this@MainScreen, result, Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        if (recordingState == RecordingState.Recording) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun backPressedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setIcon(resources.getDrawable(R.drawable.ic_launcher))
        builder.setTitle(resources.getString(R.string.app_name))
        builder.setMessage(resources.getString(R.string.StopAndExit))
            .setCancelable(false)
            .setPositiveButton(
                resources.getString(R.string.YES)
            ) { dialog: DialogInterface?, id: Int -> finish() }
            .setNegativeButton(
                resources.getString(R.string.NO)
            ) { dialog: DialogInterface, id: Int -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request we're responding to
        val hasExtras = data?.hasExtra("UploadedTrip") == true
        if (requestCode == TRIP_LIST_ACTIVITY && resultCode == RESULT_OK && hasExtras) {
            // uploadedTrip = (Trip) data.getSerializableExtra("UploadedTrip");
            if (uploadedTrip != null) {
                if (recordingState == RecordingState.Idle) {
                    UploadTripTask().execute(uploadedTrip)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.upload_while_recording),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


        if (requestCode == CAMERA_INTENT_ACTIVITY && resultCode == RESULT_OK) {
            val capturedImageUri = Uri.fromFile(mImageMarkerFileLocation)

            try {
                tripManager.addImageMarker(capturedImageUri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this,
                    "There was an error, please try again...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private inner class UploadTripTask : AsyncTask<Trip?, Void?, String>() {
        override fun onPreExecute() {
            loadingTripPd = ProgressDialog(this@MainScreen).apply {
                setIcon(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_launcher,
                        this@MainScreen.theme
                    )
                )
                setTitle(getString(R.string.app_name))
                setMessage(getString(R.string.displaying_trip))
                setCancelable(true)
                show()
            }
        }

        override fun doInBackground(vararg params: Trip?): String {
            val trip = params[0]
            trip?.let {
                if (Utils.isFileExists(it.kml)) {
                    val status = tripManager.uploadTrip(it)
                    if (status != KmlParserImpl.KML_OPENED) {
                        return FAILED
                    }
                } else {
                    return FAILED
                }
                return PASSED
            }
            return FAILED
        }

        override fun onPostExecute(result: String) {
            loadingTripPd?.dismiss()
            if (result == PASSED) {
                loadingTripPd = null
                Toast.makeText(
                    this@MainScreen, resources.getString(R.string.TripLoaded),
                    Toast.LENGTH_LONG
                ).show()
                updateDisplay()
            } else {
                uploadedTrip = null
                Toast.makeText(
                    this@MainScreen, R.string.unable_to_open_kml,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadedTripDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_launcher, theme))
        builder.setTitle(resources.getString(R.string.app_name))
        builder.setMessage(resources.getString(R.string.ContinueLoadedTrip))
            .setCancelable(false)
            .setPositiveButton(
                resources.getString(R.string.YES)
            ) { dialog: DialogInterface?, id: Int ->
                recordingMode = CONTINUE_TRIP
                startRecording()
            }
            .setNegativeButton(
                resources.getString(R.string.NO)
            ) { dialog: DialogInterface?, id: Int ->
                recordingMode = FOLLOW_TRIP
                mapFrag.clearMarkers()
                startRecording()
            }
        val alert = builder.create()
        alert.show()
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as LocalBinder
            recordingService = binder.service
            serviceBounded = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBounded = false
        }
    }

    fun startService() {
        startService(serviceIntent)
        bindService(serviceIntent!!, mConnection, BIND_AUTO_CREATE)
    }

    fun stopService() {
        try {
            stopService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            unbindService(mConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        serviceBounded = false
    }

    private fun takePhoto() {
        try {
            createImageFile()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this@MainScreen, "Couldn't create photo file...", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val imageUri = FileProvider.getUriForFile(
            this, applicationContext.packageName + ".provider",
            mImageMarkerFileLocation!!
        )
        val cameraIntent = Intent()
        cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, CAMERA_INTENT_ACTIVITY)
    }

    @Throws(IOException::class)
    private fun createImageFile() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "Image_" + timeStamp + "_"
        val storageDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        mImageMarkerFileLocation = File.createTempFile(imageFileName, ".jpg", storageDirectory)
    }

    companion object {
        private val TAG: String = MainScreen::class.java.simpleName
        private const val TRIP_LIST_ACTIVITY = 100
        private const val CAMERA_INTENT_ACTIVITY = 200
        private const val TIME_INTERVAL: Long = 2000

        private const val NEW_TRIP = 1
        private const val CONTINUE_TRIP = 2
        private const val FOLLOW_TRIP = 3
        private const val LOADED_FROM_INTENT = 4

        private const val AUTO_SAVE = 0


        // These values are for debug purposes
        // private static final float SPEED_FILTER = 0.0f; // 0.833 < 3km/h
        // private static final float ACCURACY = 1500.0f; // Accuracy of 10 meters
        // private static final int GPS_ERROR_DIALOG_REQUEST = 9001;
        const val CAM_INIT_ZOOM: Float = 0f
        const val PASSED: String = "PASSED"
        const val FAILED: String = "FAILED"

        // Request code to use when launching the resolution activity
        private const val REQUEST_RESOLVE_ERROR = 1001

        // Unique tag for the error dialog fragment
        private const val DIALOG_ERROR = "dialog_error"

        //File variable is static. This helps to prevent null pointer exceptions when return from camera intent.
        //This may occure if image was taken in landscape and phone was rotated back to portrait before returning to this Activity,
        //static helps to keeps the file variable with the value that was passed to the camera Activity.
        private var mImageMarkerFileLocation: File? = null
    }
}