package com.bartovapps.gpstriprec.core.trip_manager

import android.location.Location
import com.bartovapps.gpstriprec.core.map_helper.ImageMarker
import com.google.android.gms.maps.model.LatLng

sealed class TripState {
    data object Initiated : TripState()
    class TripUpdated(val location: Location, val distance: Float) : TripState()
    class StartLocation(val location: Location) : TripState()
    class NewImageMarker(val imageMarker: ImageMarker) : TripState()
    class OverlayRoute(val locations: List<LatLng>, val zoom: Float, val color: Int, val markers: List<ImageMarker>) : TripState()
    class TripSaved(val locations: List<LatLng>) : TripState()
    data object Stopped : TripState()
    data object OnGoing : TripState()
}