package presentation.screens.flight_details_screen

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dunihuliapps.myglidingassistnat.domain.db.TripsDataSource
import com.dunihuliapps.myglidingassistnat.domain.files.kml.KmlManager
import com.dunihuliapps.myglidingassistnat.domain.files.path_provider.PathProvider
import com.dunihuliapps.myglidingassistnat.domain.map_helper.ImageMarker
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FlightDetailsViewModel @Inject constructor(
    private val tripsDataSource: TripsDataSource,
    private val kmlManager: KmlManager,
    private val pathProvider: PathProvider,
) : ViewModel() {

    private val tripDetailsMutableStateFlow =
        MutableStateFlow<FlightDetailsState>(FlightDetailsState.Initiated)
    val tripDetailsStateFlow = tripDetailsMutableStateFlow.asStateFlow()
    private var trip: Trip? = null
    private var markerImages = mutableListOf<ImageMarker>()


    fun addEvent(event: FlightDetailsEvent){
        mapEventToState(event)
    }

    private fun mapEventToState(event: FlightDetailsEvent) {
        when (event) {
            is FlightDetailsEvent.LoadFlight -> loadTrip(event.tripId)
            is FlightDetailsEvent.OnInfoWindowClicked -> handleInfoWindowClicked(event.markerUri)
            is FlightDetailsEvent.ShareFlightMapImage -> handleShareImage()
            is FlightDetailsEvent.ShareFlightKml -> handleShareKml()
        }
    }


    private fun handleInfoWindowClicked(markerImageUri: Uri) {
        trip?.let {
            publishState(FlightDetailsState.OpenGallery(it.id, markerImageUri))
        }
    }

    private fun loadTrip(tripId: Long) {
        Log.i(TAG, "loadTrip: tripId = $tripId")
        publishState(FlightDetailsState.Loading)
        tripsDataSource.let {
            val t = it.findTripById(tripId)
            t?.let { trip ->
                this.trip = t
                val markers = it.findAllMarkersForTrip(tripId)
                if(markers.isNotEmpty()){
                    markerImages.clear()
                    markerImages.addAll(markers)
                }
                val locations = trip.kml?.let { kmlPath ->
                    kmlManager.getLocationsFromKml(kmlPath)
                } ?: emptyList()
                Log.i(TAG, "Trip Loaded: $trip, markers: $markers, locations: $locations")
                publishState(FlightDetailsState.FlightLoaded(trip, markers, locations))
            } ?: run {
                publishState(FlightDetailsState.FailedToLoadFlight("Failed to load trip"))
            }
        }
    }


    private fun handleShareImage() {
        val filePath = File("${pathProvider.providesShareImagesDir()}/${trip?.date}_map.png")
        publishState(FlightDetailsState.MapImageFileReady(filePath.path, trip?.tripName))
    }

    private fun handleShareKml(){
        trip?.kml?.let {
            publishState(FlightDetailsState.FlightKmlReady(it, trip?.tripName))
        }
    }

    private fun publishState(flightDetailsState: FlightDetailsState) {
        viewModelScope.launch {
            tripDetailsMutableStateFlow.value = flightDetailsState
        }
    }


    companion object{
        const val TAG = "com.bartovapps.gpstriprec.presentation.screens.trip_details_screen.TripDetailsViewModel"
    }
}