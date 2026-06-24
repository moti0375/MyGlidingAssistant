package presentation.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.domain.map_helper.ImageMarker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class CustomSupportMapFragment : SupportMapFragment(), OnMapReadyCallback, OnMarkerClickListener, OnInfoWindowClickListener {

    lateinit var map: GoogleMap
    val isMapReady: Boolean get() = this::map.isInitialized
    var lineWidth = 5f
    var zoom = 5f
    var lineColor = Color.RED
    var tilt = 0f

    private var marker: Marker? = null
    private var startMarker: Marker? = null
    private var lastMarker: Marker? = null
    private var line: Polyline? = null
    private var mapType = GoogleMap.MAP_TYPE_NORMAL
    private var bearing = 0f
    private val markersIdsMap = mutableMapOf<String, ImageMarker>()
    private var mapReadyListener : MapReadyListener? = null
    private var mapInfoWindowClickedListener : InfoWindowClickListener? = null

    private var gliderMarker : BitmapDescriptor? = null

    init {
        getMapAsync(this)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(map: GoogleMap) {
        // BitmapDescriptorFactory requires the Maps SDK to be initialized,
        // which is only guaranteed once onMapReady fires.
        gliderMarker = bitmapDescriptorFromVector(R.drawable.glider_marker, 100)
        setupMap(map)
        mapReadyListener?.onMapReady()
        map.setOnMarkerClickListener(this)
        map.setOnInfoWindowClickListener(this)
    }

    fun setMapReadyCallback(mapReadyListener: MapReadyListener) {
        this.mapReadyListener = mapReadyListener
    }

    fun setInfoWindowClickListener(listener: InfoWindowClickListener){
        this.mapInfoWindowClickedListener = listener
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        if(this::map.isInitialized){
            map.setPadding(left, top, right, bottom)
        }
    }

    fun goToLocation(location: Location) {
        // this.zoom = zoom;
        val latLng = LatLng(location.latitude, location.longitude)
        moveCamera(latLng, NORTH)
    }

    fun moveToLocationAndDrawLine(location: Location) {
        this.bearing = location.bearing
        LatLng(location.latitude, location.longitude).also {
            moveCamera(it, this.bearing)
            addMarker(it)
            drawLine()
        }
    }

    fun mapCameraLongShot() {
        tilt = CAMERA_LONGSHOT_TILT
        this.zoom -= CAMERA_LONGSHOT_RAT
    }

    fun mapCameraCloseup() {
        tilt = 0f
        this.zoom += CAMERA_LONGSHOT_RAT
    }

    fun overlayRoute(list: List<LatLng>) {
        val options = PolylineOptions()
            .width(lineWidth).color(lineColor)
        val builder = LatLngBounds.Builder()
        for (i in 0 until list.size - 1) {
            Log.i(LOG_TAG, "Add polyline");
            options.add(list[i]).add(list[i + 1])
            builder.include(list[i])
        }

        Log.i(LOG_TAG, "About to overlay route.. list: $list")
        addMarker(list[0])
        addMarker(list[list.size - 1])
        line = map.addPolyline(options)
        fitCameraToRoute(list)
    }


    fun fitCameraToRoute(list: List<LatLng>) {
        val builder = LatLngBounds.Builder()
        for (location in list) {
            builder.include(location)
        }
        val tmpBounds = builder.build()
        val update = CameraUpdateFactory.newLatLngBounds(
            tmpBounds, MAP_PADDING
        )

        this.map.moveCamera(update)
    }

    fun takeMapSnapshot(callback: GoogleMap.SnapshotReadyCallback) {
        this.map.snapshot(callback)
    }


    @SuppressLint("PotentialBehaviorOverride")
    private fun setupMap(map: GoogleMap) {
        this.map = map
        val update = CameraUpdateFactory.zoomBy(this.zoom)
        this.map.apply {
            moveCamera(update)
            setInfoWindowAdapter(
                CustomInfoWindowAdapter(
                    LayoutInflater.from(context),
                    markersIdsMap
                )
            )
        }
    }

    private fun moveCamera(ll: LatLng, bearing: Float) {
        val cameraPosition = CameraPosition.Builder().target(ll)
            .zoom(this.zoom).tilt(this.tilt) // Sets the zoom
            .bearing(bearing) // Sets the tilt of the camera to 30 degrees
            .build()

        this.map.animateCamera(
            CameraUpdateFactory
                .newCameraPosition(cameraPosition)
        )
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
            startMarker = this.map.addMarker(startOptions)
        }
        val options = MarkerOptions()
            .position(ll)
            .draggable(false)
            .title("End Point")
            .snippet(ll.latitude.toString() + "," + ll.longitude)
            .anchor(0.5f, 0.5f)
            .icon(gliderMarker)
        marker = this.map.addMarker(options)
    }

    private fun drawLine() {
        lastMarker?.let {
            val options = PolylineOptions()
                .add(it.position).add(marker?.position)
                .width(lineWidth).color(lineColor)
            line = this.map.addPolyline(options)
        }
    }

    fun clearEverything() {
        if (this::map.isInitialized) {
            marker?.remove()
            startMarker?.remove()
            startMarker = null

            lastMarker?.remove()
            lastMarker = null

            line?.remove()
            line = null

            markersIdsMap.clear()
            this.map.clear()
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        return false
    }


    override fun onInfoWindowClick(marker: Marker) {
        mapInfoWindowClickedListener?.onInfoWindowClicked(marker, markersIdsMap[marker.id])
    }

    fun addImageMarker(imageMarker: ImageMarker) {
        val ll = LatLng(imageMarker.latitude, imageMarker.longitude)
        this.map.addMarker(
            MarkerOptions().position(ll)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .snippet("" + ll.latitude + "," + ll.longitude)
                .title(getString(R.string.app_name))
        )?.apply {
            markersIdsMap[id] = imageMarker
        }
    }

    fun clearMarkers() {
        this.lastMarker = null
        this.marker = null
        this.line = null
        markersIdsMap.clear()
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int, sizeDp: Int): BitmapDescriptor? {
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()

        val vectorDrawable = ContextCompat.getDrawable(requireContext(), vectorResId) ?: return null
        val bitmap = Bitmap.createBitmap(
            sizePx,
            sizePx,
            Bitmap.Config.ARGB_8888
        )

        Log.i("CustomMapFragment", "bitmapDescriptorFromVector: width: ${bitmap.width}, height: ${bitmap.height} ")
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    companion object {
        private const val LOG_TAG = "CustomMapFragment"
        private const val EARTHRADIUS = 6366198.0
        private const val CAMERA_LONGSHOT_RAT = 2f
        private const val CAMERA_LONGSHOT_TILT = 65f
        private const val MAP_PADDING = 30
        private const val NORTH = 360f
    }
}