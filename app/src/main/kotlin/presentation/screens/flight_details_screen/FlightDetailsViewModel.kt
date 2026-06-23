package presentation.screens.flight_details_screen

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dunihuliapps.myglidingassistnat.data.repositories.flights.FlightsRepository
import com.dunihuliapps.myglidingassistnat.domain.files.kml.KmlManager
import com.dunihuliapps.myglidingassistnat.domain.files.path_provider.PathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Flight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FlightDetailsViewModel @Inject constructor(
    private val flightsRepository: FlightsRepository,
    private val kmlManager: KmlManager,
    private val pathProvider: PathProvider,
) : ViewModel() {

    private val tripDetailsMutableStateFlow =
        MutableStateFlow<FlightDetailsState>(FlightDetailsState.Initiated)
    val tripDetailsStateFlow = tripDetailsMutableStateFlow.asStateFlow()
    private var flight: Flight? = null


    fun addEvent(event: FlightDetailsEvent){
        mapEventToState(event)
    }

    private fun mapEventToState(event: FlightDetailsEvent) {
        when (event) {
            is FlightDetailsEvent.LoadFlight -> loadTrip(event.tripId)
            is FlightDetailsEvent.OnInfoWindowClicked -> handleInfoWindowClicked(event.markerUri)
            is FlightDetailsEvent.ShareFlightMapImage -> handleShareImage()
        }
    }


    private fun handleInfoWindowClicked(markerImageUri: Uri) {
        flight?.let {
            publishState(FlightDetailsState.OpenGallery(it.id, markerImageUri))
        }
    }

    private fun loadTrip(tripId: Long) {
        Log.i(TAG, "loadTrip: tripId = $tripId")
        publishState(FlightDetailsState.Loading)
        viewModelScope.launch {
            flightsRepository.let {
                val flight = it.findById(tripId)
                flight?.let { f ->
                    this@FlightDetailsViewModel.flight = f
                    val locations = f.kml?.let { kmlPath ->
                        kmlManager.getLocationsFromKml(kmlPath)
                    } ?: emptyList()
                    publishState(FlightDetailsState.FlightLoaded(f, locations))
                } ?: run {
                    publishState(FlightDetailsState.FailedToLoadFlight("Failed to load trip"))
                }
            }

        }
    }


    private fun handleShareImage() {
        val filePath = File("${pathProvider.providesShareImagesDir()}/${flight?.date}_map.png")
        publishState(FlightDetailsState.MapImageFileReady(filePath.path, flight?.name))
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