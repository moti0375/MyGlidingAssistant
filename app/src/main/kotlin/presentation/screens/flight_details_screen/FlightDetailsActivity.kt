package presentation.screens.flight_details_screen

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
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistant.databinding.FlightDetailsActivityBinding
import com.dunihuliapps.myglidingassistnat.data.enums.AltitudeUnits
import com.dunihuliapps.myglidingassistnat.data.enums.DistanceUnits
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.HmsFormatter
import com.dunihuliapps.myglidingassistnat.domain.formatters.TimeFormatter
import com.dunihuliapps.myglidingassistnat.domain.formatters.UnitsFormatter
import com.dunihuliapps.myglidingassistnat.domain.map_helper.ImageMarker
import com.dunihuliapps.myglidingassistnat.presentation.map.CustomSupportMapFragment
import com.dunihuliapps.myglidingassistnat.presentation.map.InfoWindowClickListener
import com.dunihuliapps.myglidingassistnat.presentation.map.MapReadyListener
import com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen.FlightsListScreen
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.FeetFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.KmhFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MetricAltFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MetricFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MillageFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.MphFormatter
import com.dunihuliapps.myglidingassistnat.utils.Utils
import com.google.android.gms.maps.model.Marker
import dagger.hilt.android.AndroidEntryPoint
import data.model.Flight
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class FlightDetailsActivity : AppCompatActivity(), InfoWindowClickListener {
    private lateinit var prefs: SharedPreferences
    private var lineColor = Color.RED
    private var lineWidth = 5f
    private var flight: Flight? = null
    private var progressDialog: ProgressDialog? = null

    private lateinit var speedDisplayer: UnitsFormatter
    private lateinit var moveSpeedDisplayer: UnitsFormatter
    private lateinit var distanceDisplayer: UnitsFormatter
    private lateinit var altitudeDisplayer: UnitsFormatter
    private val timeFormatter: TimeFormatter = HmsFormatter()
    private lateinit var mapFragment: CustomSupportMapFragment
    private var distanceUnits: DistanceUnits = DistanceUnits.Metric
    private var altUnits: AltitudeUnits = AltitudeUnits.Feet

    private val detailsViewModel by viewModels<FlightDetailsViewModel>()

    private lateinit var binding: FlightDetailsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FlightDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setUpMapIfNeeded()
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

    private fun processDetailsLoading(flightDetailsState: FlightDetailsState) {
        when (flightDetailsState) {
            is FlightDetailsState.Initiated -> {}
            is FlightDetailsState.Loading -> showLoadingDialog()
            is FlightDetailsState.FlightLoaded -> processLoadedTrip(flightDetailsState)
            is FlightDetailsState.FailedToLoadFlight -> processLoadFailed(flightDetailsState)
            is FlightDetailsState.OpenGallery -> openGallery(flightDetailsState)
            is FlightDetailsState.MapImageFileReady -> takeMapHqSnapshot(
                flightDetailsState.file,
                flightDetailsState.tripTitle
            )
            is FlightDetailsState.FlightKmlReady -> shareKml(
                flightDetailsState.file,
                flightDetailsState.tripTitle
            )
        }
    }

    private fun openGallery(flightDetailsState: FlightDetailsState.OpenGallery) {
        val galleryIntent = Intent(this@FlightDetailsActivity, FlightsListScreen::class.java)
        galleryIntent.data = flightDetailsState.imageUri
        galleryIntent.putExtra("TripId", flightDetailsState.tripId);
        startActivityForResult(galleryIntent, GALLERY_ACTIVITY_REQ);
    }

    private fun showLoadingDialog() {
        Log.i(LOG_TAG, "showLoadingDialog")
        progressDialog = ProgressDialog(this).apply {
            setIcon(
                ResourcesCompat.getDrawable(
                    resources,
                    R.mipmap.ic_launcher,
                    this@FlightDetailsActivity.theme
                )
            )
            setTitle(getString(R.string.app_name))
            setMessage(getString(R.string.displaying_trip))
            show()
        }
    }

    private fun processLoadedTrip(state: FlightDetailsState.FlightLoaded) {
        Log.i(LOG_TAG, "processLoadedTrip: ")
        updateDisplay(state.flight)
        mapFragment.apply {
            clearEverything()
            overlayRoute(state.locations)
            addImageMarkers(state.markers)
        }
        hideLoadingDialog()
    }

    private fun processLoadFailed(flightDetailsState: FlightDetailsState.FailedToLoadFlight) {

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
                lineWidth = this@FlightDetailsActivity.lineWidth
                setInfoWindowClickListener(this@FlightDetailsActivity)
            }

            val tripId = intent.getLongExtra("trip_id", 0)
            detailsViewModel.addEvent(FlightDetailsEvent.LoadFlight(tripId))
        }
    }

    override fun onInfoWindowClicked(marker: Marker, imageMarker: ImageMarker?) {
        Toast.makeText(this, "InfoWindow was clicked", Toast.LENGTH_SHORT).show();
        Log.i(LOG_TAG, "Marker " + marker.id + " was clicked");

        imageMarker?.imageUri?.let {
            detailsViewModel.addEvent(FlightDetailsEvent.OnInfoWindowClicked(it))
        }

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

    private fun updateDisplay(flight: Flight) {

        binding.tripRawDetails.apply {
            tvWhen.text = flight.date
            tvDurationDetails.text = timeFormatter.formatTime(flight.duration)
            tvDistanceDetails.text = distanceDisplayer.formatUnits(flight.distance.toDouble())
            tvAveSpeed.text = speedDisplayer.formatUnits(flight.averageSpeed)
            tvMaxSpeed.text = speedDisplayer.formatUnits(flight.maxSpeed)
            tvMaxAltitude.text = altitudeDisplayer.formatUnits(flight.maxAlt)
            tvMoveTime.text = timeFormatter.formatTime(flight.moveTime)
            tvStopTime.text = timeFormatter.formatTime(flight.stopTime)
            tvTripDetailsHead.text = flight.tripName
            tvFrom.text = flight.startAddress ?: getString(R.string.unavailable_data)
            tvTo.text = flight.stopAddress ?: getString(R.string.unavailable_data)
            if (flight.averageSpeed == 0.0) {
                tvAverageMoveSpeed.text = getString(R.string.unavailable_data)
            } else {
                tvAverageMoveSpeed.text = moveSpeedDisplayer.formatUnits(flight.averageSpeed)
            }

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
                        this@FlightDetailsActivity,
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
                    SHARE_IMAGE -> detailsViewModel.addEvent(FlightDetailsEvent.ShareFlightMapImage)
                    SHARE_KML -> detailsViewModel.addEvent(FlightDetailsEvent.ShareFlightKml)
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
                    this@FlightDetailsActivity.theme
                )
            )
        }.show()
    }


    private fun takeMapHqSnapshot(imageFileName: String, tripTitle: String?) {
        mapFragment.takeMapSnapshot{ snapshot ->
            snapshot?.let {
                shareImage(snapshot, imageFileName, tripTitle)
            }
        }
    }


    private fun shareImage(b: Bitmap, imageFileName: String, tripTitle: String?) {
        val icon = BitmapFactory.decodeResource(this.resources, R.drawable.ic_launcher)
        val image = Utils.overlay(b, icon, getString(R.string.app_name))

        val share = Intent(Intent.ACTION_SEND).apply{
            type = "image/png"
        }

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
            this@FlightDetailsActivity.contentResolver.query(
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
                    image?.compress(Bitmap.CompressFormat.PNG, 100, os)
                    image?.recycle()
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
        private val LOG_TAG: String = FlightDetailsActivity::class.java.simpleName
        private const val CAM_ZOOM = 15f
    }
}