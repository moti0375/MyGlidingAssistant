package com.dunihuliapps.myglidingassistnat.presentation.screens.main_screen

import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import com.dunihuliapps.myglidingassistnat.data.enums.RecordingMode
import data.model.Trip

sealed class MainScreenViewModelEvent {

    data object StartStopButtonClicked : MainScreenViewModelEvent()
    data object StartTrip : MainScreenViewModelEvent()
    data object StopTrip : MainScreenViewModelEvent()
    class LocationFounded(val location: Location) : MainScreenViewModelEvent()
    class OnNewLocation(val location: Location) : MainScreenViewModelEvent()
    class TripEnded(val bitmap: Bitmap?) : MainScreenViewModelEvent()
    class AccuracyChanged(val accuracy: Float) : MainScreenViewModelEvent()
    class SpeedFilterUpdated(val filter: Double) : MainScreenViewModelEvent()
    class MergeTrips(val tripA: Trip, val tripB: Trip) : MainScreenViewModelEvent()
    class UploadTrip(val trip: Trip) : MainScreenViewModelEvent()
    class PictureTaken(val capturedImageUri: Uri) : MainScreenViewModelEvent()
    class RecordingModeChanged(val recordingMode: RecordingMode) : MainScreenViewModelEvent()
}
