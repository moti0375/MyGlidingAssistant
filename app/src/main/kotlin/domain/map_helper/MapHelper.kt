package com.dunihuliapps.myglidingassistnat.domain.map_helper
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.location.Location
import android.media.ExifInterface
import android.os.Handler
import android.util.Log
import com.dunihuliapps.myglidingassistant.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.min


import com.dunihuliapps.myglidingassistnat.domain.db.TripsDBOpenHelper
import com.dunihuliapps.myglidingassistnat.domain.db.TripsDataSource
import com.dunihuliapps.myglidingassistnat.domain.di.QMainThread
import com.dunihuliapps.myglidingassistnat.domain.di.QTripsImagesDir

@Deprecated("All this logic moved to CustomMapFragment")
class MapHelper @Inject constructor(
    @QMainThread private val handler: Handler,
    @ApplicationContext private val context: Context,
    @QTripsImagesDir private val tripImagesDir: String,
    private val tripsDataSource: TripsDataSource,
) {
    // private static final String LOG_TAG = "MAP_HELPER";
    private var lineWidth = 5f
    private lateinit var mMap: GoogleMap
    private var zoom = 5f
    private var marker: Marker? = null
    private var startMarker: Marker? = null
    private var lastMarker: Marker? = null
    private var line: Polyline? = null
    private var lineColor = Color.RED
    private var mapType = GoogleMap.MAP_TYPE_NORMAL
    private var tilt = 0f
    private var latLng: LatLng? = null
    private var bearing = 0f

    private val markersIdsMap: MutableMap<String, ImageMarker?> = LinkedHashMap()

    fun initMap(googleMap: GoogleMap) {
        //Todo refactor this with a com.bartovapps.gpstriprec.presentation.map.CustomMapFragment
        Log.i(LOG_TAG, "initMap: $this")
        this.mMap = googleMap
        clearEverything()
        val update = CameraUpdateFactory.zoomBy(this.zoom)
        mMap.moveCamera(update)
//        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(LayoutInflater.from(context)))
    }

    fun setLocation(location: Location) {
        // this.zoom = zoom;
        latLng = LatLng(location.latitude, location.longitude)
        moveCamera(latLng!!, NORTH)
    }

    fun goToLocation(location: Location) {
        this.bearing = location.bearing
        LatLng(location.latitude, location.longitude).also {
            moveCamera(it, this.bearing)
            addMarker(it)
            drawLine()
        }
    }


    private fun drawLine() {
        if (lastMarker != null) {
            val options = PolylineOptions()
                .add(lastMarker?.position).add(marker?.position)
                .width(lineWidth).color(lineColor)

            line = mMap.addPolyline(options)
        }
    }

    private fun addMarker(ll: LatLng) {
        marker?.let {
            lastMarker = it
            it.remove()
            marker = null
        }

        if (startMarker == null) {
            val startOptions = MarkerOptions()
                .position(ll)
                .draggable(false)
                .title("Start Point")
                .snippet(ll.latitude.toString() + "," + ll.longitude)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            startMarker = mMap?.addMarker(startOptions)
        }
        val options = MarkerOptions()
            .position(ll)
            .draggable(false)
            .title("End Point")
            .snippet(ll.latitude.toString() + "," + ll.longitude)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        marker = mMap.addMarker(options)
        System.gc()
    }


    fun addImageMarker(imageMarker: ImageMarker, context: Context) {
        handler.post {
            val ll = LatLng(imageMarker.latitude, imageMarker.longitude)
            mMap.addMarker(
                MarkerOptions().position(ll)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .snippet("" + ll.latitude + "," + ll.longitude)
                    .title(context.getString(R.string.app_name))
            )?.apply {
                markersIdsMap[id] = imageMarker
            }

        }
    }

    fun clearMarkers() {
        this.lastMarker = null
        this.marker = null
        this.line = null
        markersIdsMap.clear()
    }

    private fun moveCamera(ll: LatLng, bearing: Float) {
        // CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll,
        // this.zoom);

        val cameraPosition = CameraPosition.Builder().target(ll)
            .zoom(this.zoom).tilt(this.tilt) // Sets the zoom
            .bearing(bearing) // Sets the tilt of the camera to 30 degrees
            .build()

        // CameraUpdate update = CameraUpdateFactory.newCameraPosition(new
        // CameraPosition(ll, this.zoom, 0, bearing));
        mMap.animateCamera(
            CameraUpdateFactory
                .newCameraPosition(cameraPosition)
        )
    }

    fun clearEverything() {
        if (this::mMap.isInitialized) {
            handler.post {
                marker?.remove()
                startMarker?.remove()
                startMarker = null

                lastMarker?.remove()
                lastMarker = null

                line?.remove()
                line = null

                markersIdsMap.clear()
                mMap.clear()
            }
        }
    }

    //This method is used when uploading a trip in the details Activity
    fun overlayRoute(list: List<LatLng>) {
        val options = PolylineOptions()
            .width(lineWidth).color(this@MapHelper.lineColor)
        val builder = LatLngBounds.Builder()
        for (i in 0 until list.size - 1) {
            Log.i(LOG_TAG, "Add polyline");
            options.add(list[i]).add(list[i + 1])
            builder.include(list[i])
        }

        val tmpBounds = builder.build()
        val update = CameraUpdateFactory.newLatLngBounds(
            tmpBounds, MAP_PADDING
        )

        Log.i(LOG_TAG, "About to overlay route.. list: $list")
        handler.post {
            addMarker(list[0])
            addMarker(list[list.size - 1])
            line = mMap.addPolyline(options)
            mMap.moveCamera(update)
        }
    }

    //This method is called when upload a trip to main screen, color is given as
    fun overlayRoute(list: List<LatLng>, zoom: Float, color: Int) {
        handler.post {
            val options = PolylineOptions()
                .width(lineWidth).color(color)
            addMarker(list[0])
            val builder = LatLngBounds.Builder()

            for (i in 0 until list.size - 1) {
                options.add(list[i]).add(list[i + 1])
                builder.include(list[i])

                // Log.i(LOG_TAG, "Add polyline");
            }
            line = mMap.addPolyline(options)
            addMarker(list[list.size - 1])
            val tmpBounds = builder.build()

            val update = CameraUpdateFactory.newLatLngBounds(
                tmpBounds, 100
            )
            mMap.moveCamera(update)
        }
    }


    fun setLineColor(color: Int) {
        this.lineColor = color
    }

    fun setZoom(zoom: Float) {
        this.zoom = zoom
        val update = CameraUpdateFactory.zoomTo(zoom)
        mMap.animateCamera(update)
    }

    fun getZoom(): Float {
        return this.zoom
    }

    fun setCameraTilt(tilt: Float) {
        this.tilt = tilt
    }

    fun setMapType(type: Int) {
        this.mapType = type
        mMap.mapType = mapType
    }

    fun setLineWidth(width: Float) {
        lineWidth = width
    }

    fun mapCameraLongshot() {
        setCameraTilt(CAMERA_LONGSHOT_TILT)
        setZoom(this.zoom - CAMERA_LONGSHOT_RAT)
    }

    fun mapCameraCloseup() {
        setCameraTilt(0f)
        setZoom(this.zoom + CAMERA_LONGSHOT_RAT)
    }

    fun viewRoute(list: List<LatLng>) {
        val builder = LatLngBounds.Builder()

        for (location in list) {
            builder.include(location)
        }
        val tmpBounds = builder.build()
        val update = CameraUpdateFactory.newLatLngBounds(
            tmpBounds, 100
        )

        handler.post { mMap.moveCamera(update) }

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            Thread.interrupted()
        }
    }

    fun saveMapAsImage( tripId: Long) {
        Log.i("com.bartovapps.gpstriprec.domain.map_helper.MapHelper", "saveMapAsImage: ")
        val timestamp = System.currentTimeMillis()

        val fileName = "$tripImagesDir/trip_$timestamp.jpeg"

        val callback = SnapshotReadyCallback { s: Bitmap? ->
            s?.let {
                var snapshot = it
                try {
                    val out = FileOutputStream(fileName)
                    snapshot = Bitmap.createScaledBitmap(
                        snapshot, 500,
                        500, false
                    )
                    snapshot.compress(Bitmap.CompressFormat.JPEG, 50, out)
                    snapshot.recycle()

                    tripsDataSource.updateTripData(
                        tripId,
                        TripsDBOpenHelper.COLUMN_MAP_IMAGE, fileName
                    )
                    out.flush()
                    out.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(
                        "com.bartovapps.gpstriprec.core.map_helper.com.bartovapps.gpstriprec.domain.map_helper.MapHelper",
                        "There was an exception: " + e.message
                    )
                }
            }
        }

        mMap.snapshot(callback)
    }

    fun saveMapAsImage(mapFileName: String) {
        // final File fileDir = new File(root +
        // context.getResources().getString(com.bartovapps.gpstriprec.R.string.projectRootDir)
        // + "/mapsImages");
        Log.i(LOG_TAG, "About to save map image");
        val fileName = File(mapFileName).apply {
            parentFile?.let { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
        }

        // final String fileName = fileDir + "/" + mapFileName;
        val callback =
            SnapshotReadyCallback { s ->
                var snapshot = s
                var out: FileOutputStream? = null
                try {
                    out = FileOutputStream(fileName)
                    snapshot = Bitmap.createScaledBitmap(
                        snapshot!!, 500,
                        500, false
                    )
                    snapshot.compress(Bitmap.CompressFormat.JPEG, 50, out)
                    snapshot.recycle()
                    Log.i(LOG_TAG, "trip image saved to $mapFileName")
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        out?.apply {
                            close()
                            flush()
                        }
                    } catch (finallyException: Exception) {
                        finallyException.printStackTrace()
                    }
                }
            }
        mMap.snapshot(callback)
    }

    // private class LongOperation extends AsyncTask<String, Void, String> {
    //
    // @Override
    // protected String doInBackground(String... params) {
    // for (int i = 0; i < 5; i++) {
    // try {
    // Thread.sleep(1000);
    // } catch (InterruptedException e) {
    // Thread.interrupted();
    // }
    // }
    // return "Executed";
    // }
    //
    // @Override
    // protected void onPostExecute(String result) {
    // // might want to change "executed" for the returned string passed
    // // into onPostExecute() but that is upto you
    // }
    //
    // @Override
    // protected void onPreExecute() {}
    //
    // @Override
    // protected void onProgressUpdate(Void... values) {}
    // }
    private fun writeTextOnDrawable(drawableId: Int, text: String, context: Context): Bitmap {
        val bm = BitmapFactory.decodeResource(context.resources, drawableId)
            .copy(Bitmap.Config.ARGB_8888, true)

        val tf = Typeface.create("Helvetica", Typeface.BOLD)

        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.setTypeface(tf)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = convertToPixels(context, 11).toFloat()

        val textRect = Rect()
        paint.getTextBounds(text, 0, text.length, textRect)

        val canvas = Canvas(bm)

        //If the text is bigger than the canvas , reduce the font size
        if (textRect.width() >= (canvas.width - 4))  //the padding on either sides is considered as 4, so as to appropriately fit in the text
            paint.textSize =
                convertToPixels(context, 7).toFloat() //Scaling needs to be used for different dpi's


        //Calculate the positions
        val xPos = (canvas.width / 2) - 2 //-2 is for regulating the x position offset

        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
        val yPos = ((canvas.height / 2) - ((paint.descent() + paint.ascent()) / 2)).toInt()

        canvas.drawText(text, xPos.toFloat(), yPos.toFloat(), paint)

        return bm
    }




    fun getReducedImage(imageFileLocation: String?): Bitmap {
        Log.i(LOG_TAG, "image file path: $imageFileLocation")
        val targetImageWidth = context.resources.getDimension(R.dimen.image_marker_width).toInt()
        val targetImageHeight = context.resources.getDimension(R.dimen.image_marker_height).toInt()

        Log.i(
            LOG_TAG,
            "image view sizes: $targetImageWidth, $targetImageHeight"
        )

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFileLocation, options)
        val cameraImageWidth = options.outWidth
        val cameraImageHeight = options.outHeight

        val scaleFactor = min(
            (cameraImageWidth / targetImageWidth).toDouble(),
            (cameraImageHeight / targetImageHeight).toDouble()
        )
            .toInt()
        options.inSampleSize = scaleFactor
        options.inJustDecodeBounds = false

        val reducedSizeImage = BitmapFactory.decodeFile(imageFileLocation, options)
        return reducedSizeImage
    }

    private fun rotateImage(bitmap: Bitmap, imageFileLocation: String): Bitmap {
        val matrix = Matrix()

        val rotation = getImageRotation(imageFileLocation)
        matrix.setRotate(rotation.toFloat())

        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return rotatedBitmap
    }

    private fun getImageRotation(imageFileLocation: String): Int {
        var exifInterface: ExifInterface? = null

        try {
            exifInterface = ExifInterface(imageFileLocation)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val orientation = exifInterface?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        Log.i(LOG_TAG, "image orientation: $orientation")


        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0

        }
    }

    fun getImageMarkerUri(markerId: String?): ImageMarker? {
        if (markerId == null) {
            return null
        }
        return markersIdsMap[markerId]
    }

    companion object {
        private const val EARTHRADIUS = 6366198.0
        private const val CAMERA_LONGSHOT_RAT = 2f
        private const val CAMERA_LONGSHOT_TILT = 65f
        private const val LOG_TAG = "com.bartovapps.gpstriprec.domain.map_helper.MapHelper"
        private const val MAP_PADDING = 30

        private const val NORTH = 360f

        /**
         * Create a new LatLng which lies toNorth meters north and toEast meters
         * east of startLL
         */
        fun move(startLL: LatLng, toNorth: Double, toEast: Double): LatLng {
            val lonDiff = meterToLongitude(toEast, startLL.latitude)
            val latDiff = meterToLatitude(toNorth)
            return LatLng(
                startLL.latitude + latDiff, startLL.longitude
                        + lonDiff
            )
        }

        private fun meterToLongitude(meterToEast: Double, latitude: Double): Double {
            val latArc = Math.toRadians(latitude)
            val radius = cos(latArc) * EARTHRADIUS
            val rad = meterToEast / radius
            return Math.toDegrees(rad)
        }

        private fun meterToLatitude(meterToNorth: Double): Double {
            val rad = meterToNorth / EARTHRADIUS
            return Math.toDegrees(rad)
        }

        fun convertToPixels(context: Context, nDP: Int): Int {
            val conversionScale = context.resources.displayMetrics.density

            return ((nDP * conversionScale) + 0.5f).toInt()
        }
    }
}