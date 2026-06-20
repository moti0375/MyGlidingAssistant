package com.dunihuliapps.myglidingassistnat.presentation.screens.main_screen

import android.graphics.Bitmap
import android.location.Location
import com.dunihuliapps.myglidingassistnat.data.enums.RecordingMode
import data.model.Flight

sealed class MainScreenViewModelEvent {

    data object StartStopButtonClicked : MainScreenViewModelEvent()
    data object TakeOff : MainScreenViewModelEvent()
    data object FinishFlight : MainScreenViewModelEvent()
    class LocationFounded(val location: Location) : MainScreenViewModelEvent()
    class OnNewLocation(val location: Location) : MainScreenViewModelEvent()
    class TripEnded(val bitmap: Bitmap?) : MainScreenViewModelEvent()
    class AccuracyChanged(val accuracy: Float) : MainScreenViewModelEvent()
    class SpeedFilterUpdated(val filter: Double) : MainScreenViewModelEvent()
    class UploadTrip(val flight: Flight) : MainScreenViewModelEvent()
    class RecordingModeChanged(val recordingMode: RecordingMode) : MainScreenViewModelEvent()
}
