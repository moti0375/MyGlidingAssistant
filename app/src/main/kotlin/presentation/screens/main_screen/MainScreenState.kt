package presentation.screens.main_screen
import android.location.Location
import com.dunihuliapps.myglidingassistnat.domain.map_helper.ImageMarker
import com.google.android.gms.maps.model.LatLng

sealed class FlightState {
    data object Initiated : FlightState()
    class FlightUpdated(val location: Location, val distance: Float) : FlightState()
    class StartLocation(val location: Location) : FlightState()
    class NewImageMarker(val imageMarker: ImageMarker) : FlightState()
    class FlightLoaded(val tripUploadedResult: TripUploadedResult) : FlightState()
    class FlightSaved(val saveStatus: SaveStatus) : FlightState()
    data object Stopped : FlightState()
    data object OnGoing : FlightState()
    data object StartRecording : FlightState()
    data object StopAndSave : FlightState()
    data object ShowSaveDialog : FlightState()
    data object ShowRecordingInBackground : FlightState()
}

sealed class SaveStatus(){
    class Success(val locations: List<LatLng>) : SaveStatus()
    data object NotEnoughData : SaveStatus()
}

sealed class TripUploadedResult{
    class Success(val route: List<LatLng>, val zoom: Float, val color: Int) : TripUploadedResult()
    class Failed(val failures: TripLoadFailures) : TripUploadedResult()
}

sealed class TripLoadFailures{
    data object UnableLoadingDuringRecording : TripLoadFailures()
    data object GenericUploadFailure : TripLoadFailures()
}