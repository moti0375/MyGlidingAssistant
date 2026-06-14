package com.bartovapps.gpstriprec.presentation.screens.main_screen

import android.location.Location
import com.bartovapps.gpstriprec.domain.map_helper.ImageMarker
import com.google.android.gms.maps.model.LatLng

sealed class TripState {
    data object Initiated : TripState()
    class TripUpdated(val location: Location, val distance: Float) : TripState()
    class StartLocation(val location: Location) : TripState()
    class NewImageMarker(val imageMarker: ImageMarker) : TripState()
    class TripLoaded(val tripUploadedResult: TripUploadedResult) : TripState()
    class TripSaved(val saveStatus: SaveStatus) : TripState()
    data object Stopped : TripState()
    data object OnGoing : TripState()
    data object StartRecording : TripState()
    data object StopAndSave : TripState()
    data object ShowSaveDialog : TripState()
    data object ShowRecordingInBackground : TripState()
}

sealed class SaveStatus(){
    class Success(val locations: List<LatLng>) : SaveStatus()
    data object NotEnoughData : SaveStatus()
}

sealed class TripUploadedResult{
    class Success(val route: List<LatLng>, val zoom: Float, val color: Int, val markers: List<ImageMarker>) : TripUploadedResult()
    class Failed(val failures: TripLoadFailures) : TripUploadedResult()
}

sealed class TripLoadFailures{
    data object UnableLoadingDuringRecording : TripLoadFailures()
    data object GenericUploadFailure : TripLoadFailures()
}