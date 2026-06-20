package presentation.screens.flight_details_screen

import android.net.Uri
import com.dunihuliapps.myglidingassistnat.domain.map_helper.ImageMarker
import com.google.android.gms.maps.model.LatLng
import data.model.Flight

sealed class FlightDetailsState {
    data object Initiated : FlightDetailsState()
    data object Loading : FlightDetailsState()
    class FlightLoaded(
        val flight: Flight,
        val locations: List<LatLng> = emptyList()
    ) : FlightDetailsState()
    class MapImageFileReady(val file: String, val tripTitle: String?) : FlightDetailsState()
    class FlightKmlReady(val file: String, val tripTitle: String?) : FlightDetailsState()
    class OpenGallery(val tripId: Long, val imageUri: Uri) : FlightDetailsState()
    class FailedToLoadFlight(val details: String?) : FlightDetailsState()
}