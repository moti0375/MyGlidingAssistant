package presentation.screens.flight_details_screen
import android.net.Uri

sealed class FlightDetailsEvent {
    class LoadFlight(val tripId: Long) : FlightDetailsEvent()
    class OnInfoWindowClicked(val markerUri: Uri) : FlightDetailsEvent()
    data object ShareFlightMapImage : FlightDetailsEvent()
    data object ShareFlightKml : FlightDetailsEvent()
}