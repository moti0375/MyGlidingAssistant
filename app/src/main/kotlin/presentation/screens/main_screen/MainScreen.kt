package com.bartovapps.gpstriprec.presentation.screens.main_screen

import android.Manifest
import com.bartovapps.gpstriprec.presentation.units_formatters.MetricFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.MillageFormatter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.app.ProgressDialog
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
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableString
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bartovapps.gpstriprec.GpsRecLicense
import com.bartovapps.gpstriprec.GpsRecTripsList
import com.bartovapps.gpstriprec.R
import com.bartovapps.gpstriprec.data.enums.AltitudeUnits
import com.bartovapps.gpstriprec.data.enums.DistanceUnits
import com.bartovapps.gpstriprec.data.enums.data.enums.SpeedUnits
import com.bartovapps.gpstriprec.databinding.GpsRecorderMainMaterialBinding
import com.bartovapps.gpstriprec.presentation.units_formatters.FeetFormatter
import com.bartovapps.gpstriprec.domain.formatters.UnitsFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.HmsFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.KmhFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.MetricAltFormatter
import com.bartovapps.gpstriprec.presentation.map.CustomSupportMapFragment
import com.bartovapps.gpstriprec.presentation.map.MapReadyListener
import com.bartovapps.gpstriprec.presentation.screens.settings_screen.SettingsActivity
import com.bartovapps.gpstriprec.presentation.units_formatters.presentation.units_formatters.KnotsFormatter
import com.bartovapps.gpstriprec.services.GpsTripRecService
import com.bartovapps.gpstriprec.services.GpsTripRecService.LocalBinder
import com.google.android.gms.common.GooglePlayServicesUtil
import dagger.hilt.android.AndroidEntryPoint
import data.model.Trip
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

@AndroidEntryPoint
class MainScreen : AppCompatActivity(), MapReadyListener, OnSharedPreferenceChangeListener {
    private var cameraZoom = 15f
    private var lineWidth = 5f
    private lateinit var binding: GpsRecorderMainMaterialBinding

    private val tripManagerViewModel by viewModels<TripManagerViewModel>()

    private lateinit var speedFormatter: UnitsFormatter
    private lateinit var distanceFormatter: UnitsFormatter
    private lateinit var altitudeFormatter: UnitsFormatter
    private val timeFormatter = HmsFormatter()

    private lateinit var lm: LocationManager
    private lateinit var settings: SharedPreferences
    private var distanceUnits = DistanceUnits.Metric
    private var speedUnits = SpeedUnits.Metric
    private var altUnits = AltitudeUnits.Feet

    private var progressDialog: ProgressDialog? = null

    private var imRecording: ImageView? = null
    private var handler: Handler = Handler(Looper.getMainLooper())

    // Bool to track whether the app is already resolving an error
    private var recordingService: GpsTripRecService? = null
    private var serviceBounded: Boolean = false
    private lateinit var serviceIntent: Intent


    private lateinit var mapFrag: CustomSupportMapFragment


    private var job: Job? = null

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GpsRecorderMainMaterialBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.appBar)

        settings = PreferenceManager.getDefaultSharedPreferences(this)
        settings.registerOnSharedPreferenceChangeListener(this)
        handler = Handler(Looper.getMainLooper())
        lm = getSystemService(LOCATION_SERVICE) as LocationManager
        progressDialog = ProgressDialog(this)
        serviceIntent = Intent(this, GpsTripRecService::class.java)

        // Create the interstitial.
        setUpMapIfNeeded()
        subscribeTimerChanges()
    }

    private fun observeTripState() {
        job?.cancel()
        job = lifecycleScope.launch {
            tripManagerViewModel.tripStateFlow.collect {
                Log.i(TAG, "onTripStateChanged: $it")
                processTripState(it)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun processTripState(state: TripState) {
        when (state) {
            is TripState.Initiated -> mapFrag.clearEverything()
            is TripState.NewImageMarker -> mapFrag.addImageMarker(state.imageMarker)
            is TripState.TripUpdated -> processTripUpdate(state)
            is TripState.OnGoing -> mapFrag.mapCameraCloseup()
            is TripState.TripLoaded -> processTripLoaded(state.tripUploadedResult)
            is TripState.Stopped -> mapFrag.mapCameraLongShot()
            is TripState.TripSaved -> {
                processTripSaved(state)
            }

            is TripState.StartLocation -> mapFrag.goToLocation(state.location)
            is TripState.StartRecording -> startRecording()
            is TripState.StopAndSave -> saveTrip()
            is TripState.ShowSaveDialog -> saveTripAlertDialog()
            is TripState.ShowRecordingInBackground -> processRecordingInBackground()
        }
    }

    private fun processRecordingInBackground() {
        Toast.makeText(this, getString(R.string.StillRecording), Toast.LENGTH_SHORT).show()
    }

    private fun processTripLoaded(loadedResult: TripUploadedResult) {
        progressDialog?.dismiss()
        when (loadedResult) {
            is TripUploadedResult.Success -> {
                mapFrag.let {
                    it.overlayRoute(loadedResult.route)
                    it.zoom = loadedResult.zoom
                    it.lineColor = loadedResult.color
                    loadedResult.markers.forEach { marker ->
                        it.addImageMarker(marker)
                    }
                }
                Toast.makeText(this@MainScreen, getString(R.string.TripLoaded), Toast.LENGTH_LONG).show()
            }

            is TripUploadedResult.Failed -> {
                when (loadedResult.failures) {
                    TripLoadFailures.GenericUploadFailure -> {
                        Toast.makeText(this@MainScreen, R.string.unable_to_open_kml, Toast.LENGTH_LONG).show()
                    }
                    TripLoadFailures.UnableLoadingDuringRecording -> {
                        Toast.makeText(this, getString(R.string.upload_while_recording), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun processTripSaved(state: TripState.TripSaved) {
        when (state.saveStatus) {
            is SaveStatus.Success -> {
                mapFrag.fitCameraToRoute(state.saveStatus.locations)
                Toast.makeText(this@MainScreen, getString(R.string.TripSaved), Toast.LENGTH_LONG)
                    .show()
            }

            is SaveStatus.NotEnoughData -> {
                Toast.makeText(
                    this@MainScreen,
                    getString(R.string.NotEnoughData),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        progressDialog?.dismiss()
    }

    private fun processTripUpdate(state: TripState.TripUpdated) {
        mapFrag.moveToLocationAndDrawLine(state.location)
        updateDisplay(state)
    }

    private fun subscribeTimerChanges() {
        lifecycleScope.launch {
            tripManagerViewModel.timerStateFlow.collect {
                binding.tvTimer.text = SpannableString(timeFormatter.formatTime(it))
            }
        }
    }


    // Invoke displayInterstitial() when you are ready to display an
    // interstitial.
    private fun updatePreferences() {
        val distanceUnits = settings.getString(resources.getString(R.string.distance_units_key), "1")?.toInt()
        val speedUnits = settings.getString(resources.getString(R.string.speed_units_key), "1")?.toInt()
        val color = settings.getString(resources.getString(R.string.LineColorPref), "1")?.toInt()
        val zoom = settings.getString(
            resources
                .getString(R.string.ZoomPref), "15f"
        )?.toFloat() ?: 15f
        val width = settings.getString(resources.getString(R.string.LineWidthPref), "5f")?.toFloat() ?: 5f
        val altitudeUnits = settings.getString(
            resources
                .getString(R.string.altitude_units_key), "1"
        )?.toInt()

        if (distanceUnits == 2) {
            this.distanceUnits = DistanceUnits.Millage
        } else {
            this.distanceUnits = DistanceUnits.Metric
        }

        if (altitudeUnits == 1) {
            this.altUnits = AltitudeUnits.Feet
        } else {
            this.altUnits = AltitudeUnits.Metric
        }

        if (speedUnits == 1) {
            this.speedUnits = SpeedUnits.Metric
        } else {
            this.speedUnits = SpeedUnits.Knots
        }

        setFormatters()

        when (color) {
            2 -> mapFrag.lineColor = Color.GREEN
            3 -> mapFrag.lineColor = Color.YELLOW
            4 -> mapFrag.lineColor = Color.BLUE
            else -> mapFrag.lineColor = Color.RED
        }


        mapFrag.let {
            it.lineWidth = width
            it.zoom = zoom
        }
    }

    private fun setFormatters() {
        distanceFormatter = if(distanceUnits == DistanceUnits.Millage) {
            MillageFormatter()
        } else {
            MetricFormatter()
        }

        speedFormatter = if(speedUnits == SpeedUnits.Knots) {
            KnotsFormatter()
        } else {
            KmhFormatter()
        }

        altitudeFormatter = if (altUnits == AltitudeUnits.Feet) {
            FeetFormatter()
        } else {
            MetricAltFormatter()
        }
    }

    private val startStopClickListener = View.OnClickListener{_: View ->
        tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.StartStopButtonClicked)
    }

    private var camButtonClickListener: View.OnClickListener = View.OnClickListener { view: View ->
            takePhoto()
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startRecording() {
        binding.fabStartStop.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_action_stop, this.theme))
        binding.fabCamera.visibility = View.VISIBLE
        enableLocationListener(fixListener)
        mapFrag.zoom = cameraZoom
        mapFrag.tilt = 0f
        progressDialog?.apply {
            setCancelable(true)
            setOnCancelListener(pdOnCancelListener)
            setIcon(ResourcesCompat.getDrawable(resources, R.mipmap.ic_lanucher, theme))
            setTitle(getString(R.string.app_name))
            setMessage(getString(R.string.WaitForGPS))
            show()
        }

        imRecording?.visibility = View.VISIBLE
    }

    private fun stopRecording() {
        disableLocationListener(fixListener)
        disableGpsLocationListener(trackLocationListener)
        tripManagerViewModel.addTripEvent(mainScreenViewModelEvent = MainScreenViewModelEvent.StopTrip)
        imRecording?.visibility = View.GONE
        binding.fabStartStop.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_action_new, this.theme))
        binding.fabCamera.visibility = View.INVISIBLE
        stopService()
    }

    override fun onResume() {
        super.onResume()
        setUpMapIfNeeded()
        observeTripState()
    }


    private val fixListener = object : LocationListener {
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onLocationChanged(location: Location) {
            disableGpsLocationListener(this)
            progressDialog?.let {
                if (it.isShowing) {
                    it.dismiss()
                }
            }
            startService()
            enableGPSLocationListener(trackLocationListener)
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.LocationFounded(location))


            Toast.makeText(
                this@MainScreen,
                resources.getString(R.string.LocationFounded),
                Toast.LENGTH_LONG
            ).show()

        }

    }


    override fun onDestroy() {
        stopRecording()
        mapFrag.takeMapSnapshot {
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.TripEnded(it))
        }

        try {
            stopService(serviceIntent)
            unbindService(mConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(this, resources.getString(R.string.Goodbye), Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }

            R.id.action_license -> {
                val license_intent = Intent(this, GpsRecLicense::class.java)
                startActivity(license_intent)
            }
        }
        return true
    }

    var trackLocationListener: LocationListener =
        LocationListener { location: Location ->
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.OnNewLocation(location))
        }

    private fun updateDisplay(tripUpdate: TripState.TripUpdated) {
        binding.apply {
            tvSpeed.text = speedFormatter.formatUnits(tripUpdate.location.speed.toDouble())
            tvDistance.text = distanceFormatter.formatUnits(tripUpdate.distance.toDouble())
            tvAltitude.text = altitudeFormatter.formatUnits(tripUpdate.location.altitude)
        }

        if (recordingService != null && serviceBounded) {
            recordingService?.updateService(tripUpdate.distance)
        }
    }


    private fun setUpMapIfNeeded() {
        Log.i(TAG, "setUpMapIfNeeded: initializing map")
        if (!this::mapFrag.isInitialized) {
            mapFrag =
                (supportFragmentManager.findFragmentById(R.id.map) as CustomSupportMapFragment).apply {
                    lineWidth = this@MainScreen.lineWidth
                    setMapReadyCallback(this@MainScreen)
                }
        }
    }

    private fun saveTripAlertDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(resources.getString(R.string.SAVE_TRIP))
            setMessage(resources.getString(R.string.SaveDialog))
            setCancelable(true)
            setIcon(ResourcesCompat.getDrawable(resources, R.mipmap.ic_lanucher, this@MainScreen.theme))
            setPositiveButton(resources.getString(R.string.YES)) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                saveTrip()
            }
            setNegativeButton(resources.getString(R.string.NO)) { dialog: DialogInterface, _: Int ->
                stopRecording()
                dialog.dismiss()
            }
            setOnCancelListener(saveDialogCancelListener)
        }.show()
    }

    private fun saveTrip() {
        progressDialog?.apply {
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
        stopRecording()
        mapFrag.takeMapSnapshot {
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.TripEnded(it))
        }
    }

    private var pdOnCancelListener: DialogInterface.OnCancelListener =
        DialogInterface.OnCancelListener { _: DialogInterface? ->
            Toast.makeText(this, getString(R.string.Canceled), Toast.LENGTH_SHORT).show()
            stopRecording()
        }

    private var saveDialogCancelListener: DialogInterface.OnCancelListener =
        DialogInterface.OnCancelListener { _: DialogInterface? ->
            binding.fabStartStop.setBackgroundDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_action_new, theme))
        }


    /* Called from ErrorDialogFragment when the dialog is dismissed. */


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady() {
        Log.i(TAG, "onMapReady: ")
        val topPadding = findViewById<View>(R.id.llGauges).measuredHeight
        val bottomPadding = binding.tvAltitude.measuredHeight
        mapFrag.setPadding(0, topPadding, 0, bottomPadding + 5)
        updatePreferences()
        moveMapToInitialPosition()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun moveToLastKnownLocation() {
        val networkProvider = LocationManager.NETWORK_PROVIDER
        val lastKnownLocation = lm.getLastKnownLocation(networkProvider)
        lastKnownLocation?.let {
            mapFrag.zoom = cameraZoom
            mapFrag.goToLocation(lastKnownLocation)
        }
    }

    private var prefListener: OnSharedPreferenceChangeListener =
        OnSharedPreferenceChangeListener { _: SharedPreferences?, _: String? -> updatePreferences() }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    private fun backPressedDialog() {
        AlertDialog.Builder(this).apply {
            setIcon(ResourcesCompat.getDrawable(resources, R.mipmap.ic_lanucher, this@MainScreen.theme))
            setTitle(resources.getString(R.string.app_name))
            setMessage(resources.getString(R.string.StopAndExit))
            setCancelable(false)
            setPositiveButton(
                resources.getString(R.string.YES)
            ) { _: DialogInterface?, _: Int -> finish() }
            setNegativeButton(
                resources.getString(R.string.NO)
            ) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        }.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request we're responding to
        val hasExtras = data?.hasExtra("UploadedTrip") == true
        if (requestCode == TRIP_LIST_ACTIVITY && resultCode == RESULT_OK && hasExtras) {
            val uploadedTrip = data?.getSerializableExtra("UploadedTrip") as Trip
            showUploadTripDialog()
            tripManagerViewModel.addTripEvent(MainScreenViewModelEvent.UploadTrip(uploadedTrip))
        }


        if (requestCode == CAMERA_INTENT_ACTIVITY && resultCode == RESULT_OK) {
            val capturedImageUri = Uri.fromFile(mImageMarkerFileLocation)

            try {
                tripManagerViewModel.addTripEvent(
                    MainScreenViewModelEvent.PictureTaken(
                        capturedImageUri
                    )
                )
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

    private fun showUploadTripDialog() {
        progressDialog?.apply {
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

//    private inner class UploadTripTask : AsyncTask<Trip?, Void?, String>() {
//        override fun onPreExecute() {
//            progressDialog?.apply {
//                setIcon(
//                    ResourcesCompat.getDrawable(
//                        resources,
//                        R.drawable.ic_launcher,
//                        this@MainScreen.theme
//                    )
//                )
//                setTitle(getString(R.string.app_name))
//                setMessage(getString(R.string.displaying_trip))
//                setCancelable(true)
//                show()
//            }
//        }
//
//        override fun doInBackground(vararg params: Trip?): String {
//            val trip = params[0]
//            trip?.let {
//                if (Utils.isFileExists(it.kml)) {
//                    val status = tripManager.uploadTrip(it)
//                    if (status != KmlParserImpl.KML_OPENED) {
//                        return FAILED
//                    }
//                } else {
//                    return FAILED
//                }
//                return PASSED
//            }
//            return FAILED
//        }
//
//        override fun onPostExecute(result: String) {
//            progressDialog?.dismiss()
//            if (result == PASSED) {
//                progressDialog = null
//                Toast.makeText(
//                    this@MainScreen, resources.getString(R.string.TripLoaded),
//                    Toast.LENGTH_LONG
//                ).show()
//            } else {
//                uploadedTrip = null
//                Toast.makeText(
//                    this@MainScreen, R.string.unable_to_open_kml,
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }

//    private fun loadedTripDialog() {
//        AlertDialog.Builder(this).apply {
//            setIcon(ResourcesCompat.getDrawable(resources, R.mipmap.ic_lanucher, theme))
//            setTitle(resources.getString(R.string.app_name))
//            setMessage(resources.getString(R.string.ContinueLoadedTrip))
//                .setCancelable(false)
//                .setPositiveButton(
//                    resources.getString(R.string.YES)
//                ) { _: DialogInterface?, _: Int ->
//                    recordingMode = CONTINUE_TRIP
//                    startRecording()
//                }
//            setNegativeButton(
//                resources.getString(R.string.NO)
//            ) { _: DialogInterface?, _: Int ->
//                recordingMode = FOLLOW_TRIP
//                mapFrag.clearMarkers()
//                startRecording()
//            }
//        }.create().show()
//    }

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

    private fun startService() {
        if (this::serviceIntent.isInitialized) {
            serviceIntent.let {
                startService(it)
                bindService(it, mConnection, BIND_AUTO_CREATE)
            }
        }
    }

    private fun stopService() {
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePreferences()
    }

    companion object {
        private val TAG: String = MainScreen::class.java.simpleName
        private const val TRIP_LIST_ACTIVITY = 100
        private const val CAMERA_INTENT_ACTIVITY = 200
        private const val TIME_INTERVAL: Long = 2000


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