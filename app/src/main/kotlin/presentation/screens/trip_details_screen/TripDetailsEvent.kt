package com.bartovapps.gpstriprec.presentation.screens.trip_details_screen

import android.net.Uri

sealed class TripDetailsEvent {
    class LoadTrip(val tripId: Long) : TripDetailsEvent()
    class OnInfoWindowClicked(val markerUri: Uri) : TripDetailsEvent()
    data object ShareTripMapImage : TripDetailsEvent()
    data object ShareTripKml : TripDetailsEvent()
}