package com.bartovapps.gpstriprec.presentation.screens.trip_details

import android.net.Uri
import com.bartovapps.gpstriprec.core.map_helper.ImageMarker
import com.google.android.gms.maps.model.LatLng
import data.model.Trip

sealed class TripDetailsState {
    data object Initiated : TripDetailsState()
    data object Loading : TripDetailsState()
    class TripLoaded(
        val trip: Trip,
        val markers: List<ImageMarker> = emptyList(),
        val locations: List<LatLng> = emptyList()
    ) : TripDetailsState()
    class MapImageFileReady(val file: String, val tripTitle: String?) : TripDetailsState()
    class TripKmlReady(val file: String, val tripTitle: String?) : TripDetailsState()
    class OpenGallery(val tripId: Long, val imageUri: Uri) : TripDetailsState()
    class FailedToLoadTrip(val details: String?) : TripDetailsState()
}