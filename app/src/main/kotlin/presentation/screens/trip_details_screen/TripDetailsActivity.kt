package com.bartovapps.gpstriprec.presentation.screens.trip_details_screen
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bartovapps.gpstriprec.GpsTripRecGallery
import com.bartovapps.gpstriprec.R
import com.bartovapps.gpstriprec.data.enums.AltitudeUnits
import com.bartovapps.gpstriprec.data.enums.DistanceUnits
import com.bartovapps.gpstriprec.presentation.units_formatters.HmsFormatter
import com.bartovapps.gpstriprec.domain.formatters.TimeFormatter
import com.bartovapps.gpstriprec.domain.formatters.UnitsFormatter
import com.bartovapps.gpstriprec.domain.map_helper.ImageMarker
import com.bartovapps.gpstriprec.presentation.map.CustomSupportMapFragment
import com.bartovapps.gpstriprec.presentation.map.InfoWindowClickListener
import com.bartovapps.gpstriprec.presentation.map.MapReadyListener
import com.bartovapps.gpstriprec.presentation.units_formatters.FeetFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.KmhFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.MetricAltFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.MetricFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.MillageFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.MphFormatter
import com.bartovapps.gpstriprec.utils.Utils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback
import com.google.android.gms.maps.model.Marker
import dagger.hilt.android.AndroidEntryPoint
import data.model.Trip
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class TripDetailsActivity : AppCompatActivity(), InfoWindowClickListener {
    private var mMap: GoogleMap? = null

    private lateinit var prefs: SharedPreferences
    private var lineColor = Color.RED
    private var lineWidth = 5f
    private var trip: Trip? = null
    private var progressDialog: ProgressDialog? = null
    private var toolbar: Toolbar? = null

    private lateinit var tvWhen: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvAvSpeed: TextView
    private lateinit var tvMaxSpeed: TextView
    private lateinit var tvFrom: TextView
    private lateinit var tvTo: TextView
    private lateinit var tvMaxAltitude: TextView
    private lateinit var tvAverageMoveSpeed: TextView
    private lateinit var tvMoveTime: TextView
    private lateinit var tvStopTime: TextView
    private lateinit var tvTripDetailsHead: TextView

    private lateinit var speedDisplayer: UnitsFormatter
    private lateinit var moveSpeedDisplayer: UnitsFormatter
    private lateinit var distanceDisplayer: UnitsFormatter
    private lateinit var altitudeDisplayer: UnitsFormatter
    private val timeFormatter: TimeFormatter = HmsFormatter()
    private lateinit var mapFragment: CustomSupportMapFragment
    private var distanceUnits: DistanceUnits = DistanceUnits.Metric
    private var altUnits: AltitudeUnits = AltitudeUnits.Feet

    private val detailsViewModel by viewModels<TripDetailsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.trip_details_activity)


        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setUpMapIfNeeded()
        initDisplayComponents()
        observeViewModelState()
        updatePreferences()
        setDisplayers()
    }

    private fun observeViewModelState() {
        lifecycleScope.launch {
            detailsViewModel.tripDetailsStateFlow.collect {
                processDetailsLoading(it)
            }
        }
    }

    private fun processDetailsLoading(tripDetailsState: TripDetailsState) {
        when (tripDetailsState) {
            is TripDetailsState.Initiated -> {}
            is TripDetailsState.Loading -> showLoadingDialog()
            is TripDetailsState.TripLoaded -> processLoadedTrip(tripDetailsState)
            is TripDetailsState.FailedToLoadTrip -> processLoadFailed(tripDetailsState)
            is TripDetailsState.OpenGallery -> openGallery(tripDetailsState)
            is TripDetailsState.MapImageFileReady -> takeMapHqSnapshot(
                tripDetailsState.file,
                tripDetailsState.tripTitle
            )
            is TripDetailsState.TripKmlReady -> shareKml(
                tripDetailsState.file,
                tripDetailsState.tripTitle
            )
        }
    }

    private fun openGallery(tripDetailsState: TripDetailsState.OpenGallery) {
        val galleryIntent = Intent(this@TripDetailsActivity, GpsTripRecGallery::class.java)
        galleryIntent.setData(tripDetailsState.imageUri)
        galleryIntent.putExtra("TripId", tripDetailsState.tripId);
        startActivityForResult(galleryIntent, GALLERY_ACTIVITY_REQ);
    }

    private fun showLoadingDialog() {
        Log.i(LOG_TAG, "showLoadingDialog")
        progressDialog = ProgressDialog(this).apply {
            setIcon(
                ResourcesCompat.getDrawable(
                    resources,
                    R.mipmap.ic_lanucher,
                    this@TripDetailsActivity.theme
                )
            )
            setTitle(getString(R.string.app_name))
            setMessage(getString(R.string.displaying_trip))
            show()
        }
    }

    private fun processLoadedTrip(state: TripDetailsState.TripLoaded) {
        Log.i(LOG_TAG, "processLoadedTrip: ")
        updateDisplay(state.trip)
        mapFragment.apply {
            clearEverything()
            overlayRoute(state.locations)
            addImageMarkers(state.markers)
        }
        hideLoadingDialog()
    }

    private fun processLoadFailed(tripDetailsState: TripDetailsState.FailedToLoadTrip) {

    }

    private fun addImageMarkers(markers: List<ImageMarker>?) {
        markers?.forEach {
            mapFragment.addImageMarker(it)
        }
    }

    private fun hideLoadingDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }

        if (item.itemId == R.id.action_googleErath) {
            startGoogleEarth()
        }

        if (item.itemId == R.id.action_share) {
            showSharingDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.trip_details_menu, menu)
        return true
    }

    private fun updatePreferences() {
        val color = prefs.getString(resources.getString(R.string.LineColorPref), "1")?.toInt()
        this.lineWidth = prefs.getString(resources.getString(R.string.LineWidthPref), "5.0f")?.toFloat() ?: 5f
        val distanceUnits = prefs.getString(resources.getString(R.string.distance_units_key), "1")?.toInt()
        val altUnits = prefs.getString(resources.getString(R.string.altitude_units_key), "1")?.toInt()
        when (distanceUnits) {
            1 -> this.distanceUnits = DistanceUnits.Metric
            2 -> this.distanceUnits = DistanceUnits.Millage
        }

        when (altUnits) {
            1 -> this.altUnits = AltitudeUnits.Feet
            2 -> this.altUnits = AltitudeUnits.Metric
        }

        when (color) {
            1 -> this.lineColor = Color.RED
            2 -> this.lineColor = Color.GREEN
            3 -> this.lineColor = Color.YELLOW
            4 -> this.lineColor = Color.BLUE
            else -> this.lineColor = Color.RED
        }
    }

//    private inner class TakeMapSnapshot : AsyncTask<String?, Void?, String>() {
//        override fun doInBackground(vararg params: String?): String {
//            mapHelper.saveMapAsImage(trip!!.id)
//            return "Executed"
//        }
//
//    }

    private fun setUpMapIfNeeded() {
        if (!this::mapFragment.isInitialized) {
            mapFragment = (supportFragmentManager
                .findFragmentById(R.id.savedMap) as CustomSupportMapFragment).apply {
                setMapReadyCallback(mapReadyListener)
            }
        }
    }

    private val mapReadyListener = object : MapReadyListener {
        override fun onMapReady() {
            Log.i(LOG_TAG, "onMapReady: ")
            mapFragment.apply {
                lineWidth = this@TripDetailsActivity.lineWidth
                setInfoWindowClickListener(this@TripDetailsActivity)
            }

            val tripId = intent.getLongExtra("trip_id", 0)
            detailsViewModel.addEvent(TripDetailsEvent.LoadTrip(tripId))
        }
    }

    override fun onInfoWindowClicked(marker: Marker, imageMarker: ImageMarker?) {
        Toast.makeText(this, "InfoWindow was clicked", Toast.LENGTH_SHORT).show();
        Log.i(LOG_TAG, "Marker " + marker.id + " was clicked");

        imageMarker?.imageUri?.let {
            detailsViewModel.addEvent(TripDetailsEvent.OnInfoWindowClicked(it))
        }

    }


    private fun initDisplayComponents() {
        tvWhen = findViewById(R.id.tvWhen)
        tvDuration = findViewById(R.id.tvDurationDetails)
        tvAvSpeed = findViewById(R.id.tvAveSpeed)
        tvMaxSpeed = findViewById(R.id.tvMaxSpeed)
        tvDistance = findViewById(R.id.tvDistanceDetails)
        tvFrom = findViewById(R.id.tvFrom)
        tvTo = findViewById(R.id.tvTo)
        tvMaxAltitude = findViewById(R.id.tvMaxAltitude)
        tvAverageMoveSpeed = findViewById(R.id.tvAverageMoveSpeed)
        tvMoveTime = findViewById(R.id.tvMoveTime)
        tvStopTime = findViewById(R.id.tvStopTime)
        tvTripDetailsHead = findViewById(R.id.tvTripDetailsHead)
    }

    private fun setDisplayers() {
        Log.i("TripDetailsActivity", "setDisplayers: units: ${this.distanceUnits}")
        if (this.distanceUnits == DistanceUnits.Millage) {
            speedDisplayer = MphFormatter()
            moveSpeedDisplayer = MphFormatter()
            distanceDisplayer = MillageFormatter()
        } else {
            speedDisplayer = KmhFormatter()
            moveSpeedDisplayer = KmhFormatter()
            distanceDisplayer = MetricFormatter()
        }

        altitudeDisplayer = if (this.altUnits == AltitudeUnits.Feet) {
            FeetFormatter()
        } else {
            MetricAltFormatter()
        }
    }

    private fun updateDisplay(trip: Trip) {
        tvWhen.text = trip.date
        tvDuration.text = timeFormatter.formatTime(trip.duration)
        tvDistance.text = distanceDisplayer.formatUnits(trip.distance.toDouble())
        tvAvSpeed.text = speedDisplayer.formatUnits(trip.averageSpeed)
        tvMaxSpeed.text = speedDisplayer.formatUnits(trip.maxSpeed)
        tvMaxAltitude.text = altitudeDisplayer.formatUnits(trip.maxAlt)
        tvMoveTime.text = timeFormatter.formatTime(trip.moveTime)
        tvStopTime.text = timeFormatter.formatTime(trip.stopTime)
        tvTripDetailsHead.text = trip.tripName
        tvFrom.text = trip.startAddress ?: getString(R.string.unavailable_data)
        tvTo.text = trip.stopAddress ?: getString(R.string.unavailable_data)
        if (trip.averageSpeed == 0.0) {
            tvAverageMoveSpeed.text = getString(R.string.unavailable_data)
        } else {
            tvAverageMoveSpeed.text = moveSpeedDisplayer.formatUnits(trip.averageSpeed)
        }
    }

    private fun startGoogleEarth() {
        val googleEarthInstalled = Utils.isPackageInstalled(
            GOOGLE_EARTH_PACKAGE, this
        )
        if (googleEarthInstalled) {
            if (!Utils.isFileExists(trip!!.kml)) {
                Toast.makeText(
                    this@TripDetailsActivity,
                    getString(R.string.MapUnavailable),
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val file = File(trip!!.kml)
            val earthURI = Uri.fromFile(file)

            val earthIntent = Intent(Intent.ACTION_VIEW, earthURI)

            //            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(Uri.fromFile(file), GOOGLE_EARTH_KML_ARG);
//            intent.putExtra("com.google.earth.EXTRA.tour_feature_id", "my_track");
            try {
                startActivity(earthIntent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                googleEarthInstallDialog()
            }
        } else {
            googleEarthInstallDialog()
        }
    }

    private fun googleEarthInstallDialog() {
        AlertDialog.Builder(this).apply {
            setMessage(resources.getString(R.string.NoGoogleEarth))
            setTitle(resources.getString(R.string.app_name))
            setIcon(resources.getDrawable(R.drawable.ic_launcher))
            setCancelable(false)
            setPositiveButton(
                resources.getString(R.string.YES)
            ) { dialog: DialogInterface, id: Int ->
                dialog.cancel()
                //Toast.makeText(context, "Google Earth is not installed on this device\nGoogle Erath is required for this action", Toast.LENGTH_LONG).show();
                val marketIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_EARTH_STORE_URI)
                )
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                try {
                    startActivity(marketIntent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@TripDetailsActivity,
                        getString(R.string.GooglePlayError),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            setNegativeButton(
                resources.getString(R.string.NO)
            ) { dialog: DialogInterface, id: Int -> dialog.cancel() }
        }.create().show()
    }


    private fun showSharingDialog() {
        val options = arrayOf<CharSequence>(
            getString(R.string.share_map_image),
            getString(R.string.share_kml)
        )

        AlertDialog.Builder(this).apply {
            setTitle(resources.getString(R.string.app_name))
            setCancelable(false)
            setSingleChoiceItems(options, 0, null)
            setPositiveButton(
                resources.getString(R.string.YES)
            ) { dialog, _ ->
                val selectedPosition = (dialog as AlertDialog).listView.checkedItemPosition
                when (selectedPosition) {
                    SHARE_IMAGE -> detailsViewModel.addEvent(TripDetailsEvent.ShareTripMapImage)
                    SHARE_KML -> detailsViewModel.addEvent(TripDetailsEvent.ShareTripKml)
                }
                dialog.cancel()
            }
            setNegativeButton(
                resources.getString(R.string.NO)
            ) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        }.create().apply {
            setIcon(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_launcher,
                    this@TripDetailsActivity.theme
                )
            )
        }.show()
    }


    private fun takeMapHqSnapshot(imageFileName: String, tripTitle: String?) {
        val callback = SnapshotReadyCallback { snapshot: Bitmap? ->
            shareImage(snapshot, imageFileName, tripTitle)
        }
        mapFragment.takeMapSnapshot(callback)

    }


    private fun shareImage(b: Bitmap?, imageFileName: String, tripTitle: String?) {
        val icon = BitmapFactory.decodeResource(this.resources, R.drawable.ic_launcher)
        val image = Utils.overlay(b, icon, getString(R.string.app_name))

        val share = Intent(Intent.ACTION_SEND)
        share.setType("image/png")
        val path = File(imageFileName).path

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, tripTitle ?: resources.getString(R.string.app_name))
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TripRecorder")
        }

        val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.apply {
            val filesUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.TITLE)
            val selection = MediaStore.MediaColumns.RELATIVE_PATH + " = ?"
            val args = arrayOf(path)
            this@TripDetailsActivity.contentResolver.query(
                filesUri,
                projection,
                selection,
                args,
                null
            ).use { c ->
                if (c?.moveToFirst() == true) {
                    val rowId = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    MediaStore.Files.getContentUri("external", rowId)
                }
            }
        }

        Log.i(LOG_TAG, "About to save the image in path: " + uri?.path)

        try {
            uri?.let {
                contentResolver.openOutputStream(it)?.use { os ->
                    image.compress(Bitmap.CompressFormat.PNG, 100, os)
                    image.recycle()
                }
            }
            share.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(share, getString(R.string.share_map_image)))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share map image", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareKml(kmlPath: String, tripTitle: String?) {
        Log.i(LOG_TAG, "About to share kml: $kmlPath")

        val values = ContentValues().apply {
            val displayName = if (!tripTitle.isNullOrEmpty()) "$tripTitle.kml" else getString(R.string.app_name).replace(" ", "") + ".kml"
            Log.i(LOG_TAG, "About to share displayName: $displayName")
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, getString(R.string.kml_mime_type))
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents/TripRecorder/kml")
        }

        var uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        if(uri == null){
            Log.i(LOG_TAG, "uri is null"); //It means that this file was already in the MediaStore, therefore it can't inserted again. Need to get it's Uri based on the title.
            val filesUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )
            val selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " = ?"
            val args = arrayOf(kmlPath)

            this.contentResolver.query(filesUri, projection, selection, args, null).use { c ->
                if (c?.moveToFirst() == true) {
                    Log.i(LOG_TAG, "item already exists! getting the already exists uri...");
                    val rowId = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    uri = MediaStore.Files.getContentUri("external", rowId)
                    Log.i(LOG_TAG, "refresh scan force uri=" + uri);
                }
            }
        }

        uri?.let {
            val yourByteArray = File(kmlPath).readBytes()
            contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(yourByteArray) // Write the file content to MediaStore
            }
            val share = Intent(Intent.ACTION_SEND).apply {
                setType(getString(R.string.kml_mime_type))
                putExtra(Intent.EXTRA_STREAM, it)
            }
            Log.i(LOG_TAG, "share uri : $uri")
            startActivity(Intent.createChooser(share, getString(R.string.share_kml)))
        }


    }


    companion object {
        private const val TAG = "TAG_TripDetailsActivity"
        private const val GOOGLE_EARTH_PACKAGE = "com.google.earth"
        private const val GOOGLE_EARTH_STORE_URI =
            "https://play.google.com/store/apps/details?id=com.google.earth"
        private const val GOOGLE_EARTH_KML_ARG = "application/vnd.googleearth.kml+xml"

        private const val GALLERY_ACTIVITY_REQ = 100
        private const val SHARE_IMAGE = 0
        private const val SHARE_KML = 1
        private val LOG_TAG: String = TripDetailsActivity::class.java.simpleName
        private const val CAM_ZOOM = 15f
    }
}