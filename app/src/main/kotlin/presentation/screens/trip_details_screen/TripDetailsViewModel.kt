package com.bartovapps.gpstriprec.presentation.screens.trip_details

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bartovapps.gpstriprec.core.db.TripsDataSource
import com.bartovapps.gpstriprec.core.files.kml.KmlManager
import com.bartovapps.gpstriprec.core.files.path_provider.PathProvider
import com.bartovapps.gpstriprec.core.map_helper.ImageMarker
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TripDetailsViewModel @Inject constructor(
    private val tripsDataSource: TripsDataSource,
    private val kmlManager: KmlManager,
    private val pathProvider: PathProvider,
) : ViewModel() {

    private val tripDetailsMutableStateFlow =
        MutableStateFlow<TripDetailsState>(TripDetailsState.Initiated)
    val tripDetailsStateFlow = tripDetailsMutableStateFlow.asStateFlow()
    private var trip: Trip? = null
    private var markerImages = mutableListOf<ImageMarker>()


    fun addEvent(event: TripDetailsEvent){
        mapEventToState(event)
    }

    private fun mapEventToState(event: TripDetailsEvent) {
        when (event) {
            is TripDetailsEvent.LoadTrip -> loadTrip(event.tripId)
            is TripDetailsEvent.OnInfoWindowClicked -> handleInfoWindowClicked(event.markerUri)
            is TripDetailsEvent.ShareTripMapImage -> handleShareImage()
            is TripDetailsEvent.ShareTripKml -> handleShareKml()
        }
    }


    private fun handleInfoWindowClicked(markerImageUri: Uri) {
        trip?.let {
            publishState(TripDetailsState.OpenGallery(it.id, markerImageUri))
        }
    }

    private fun loadTrip(tripId: Long) {
        Log.i(TAG, "loadTrip: tripId = $tripId")
        publishState(TripDetailsState.Loading)
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
                publishState(TripDetailsState.TripLoaded(trip, markers, locations))
            } ?: run {
                publishState(TripDetailsState.FailedToLoadTrip("Failed to load trip"))
            }
        }
    }


    private fun handleShareImage() {
        val filePath = File("${pathProvider.providesShareImagesDir()}/${trip?.date}_map.png")
        publishState(TripDetailsState.MapImageFileReady(filePath.path, trip?.tripName))
    }

    private fun handleShareKml(){
        trip?.kml?.let {
            publishState(TripDetailsState.TripKmlReady(it, trip?.tripName))
        }
    }

    private fun publishState(tripDetailsState: TripDetailsState) {
        viewModelScope.launch {
            tripDetailsMutableStateFlow.value = tripDetailsState
        }
    }


    companion object{
        const val TAG = "TripDetailsViewModel"
    }
}