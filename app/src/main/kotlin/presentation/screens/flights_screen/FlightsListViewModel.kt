package com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dunihuliapps.myglidingassistnat.data.repositories.flights.FlightsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Flight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlightsListViewModel @Inject constructor(
    private val flightsRepository: FlightsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FlightsListState>(FlightsListState.Initiated)
    val state = _state.asStateFlow()

    private val _editSelectedFlight = MutableStateFlow<Flight?>(null)
    val editSelectedFlight = _editSelectedFlight.asStateFlow()

    private val selectedFlights = mutableListOf<Flight>()

    private var selectedFlight: Flight? = null

    fun mapEventToState(event: FlightsListEvent) {
        viewModelScope.launch {
            when (event) {
                is FlightsListEvent.GetAllFlights -> getAllFlights()
                is FlightsListEvent.DeleteFlight -> deleteFlight(event.flight)
                is FlightsListEvent.DeleteSelectedFlights -> deleteSelected()
                is FlightsListEvent.UpdateSelectedFlights -> toggleSelectedFlight(event.flights)
                is FlightsListEvent.ClearSelectedFlight -> clearSelectedFlights()
                is FlightsListEvent.UpdateFlightName -> updateFlightName(event.flight, event.name)
                is FlightsListEvent.OnFlightSelected -> onFlightSelected(event.flight)
                is FlightsListEvent.EditFlightClicked -> onEditFlightClicked()
            }
        }

    }

    private fun onEditFlightClicked() {
        selectedFlight?.let {
            _editSelectedFlight.value = it
        }
    }

    private fun clearSelectedFlights() {
        selectedFlights.clear()
        selectedFlight = null
    }

    private suspend fun getAllFlights() {
        _state.value = FlightsListState.Loading
        flightsRepository.getAllFlights().collect {
            _state.value = FlightsListState.FlightsLoaded(it)
        }
    }

    private suspend fun deleteFlight(flight: Flight) {
        flightsRepository.deleteFlight(flight)
        getAllFlights()
    }

    private suspend fun deleteSelected() {
        flightsRepository.deleteFlights(selectedFlights.map { it.id })
        getAllFlights()
    }

    private suspend fun updateFlightName(flight: Flight, name: String) {
        flightsRepository.updateFlightName(flight.id, name)
        getAllFlights()
    }

    private suspend fun onFlightSelected(flight: Flight) {
        selectedFlight = flight
    }

    private suspend fun toggleSelectedFlight(flights: List<Flight>) {
        selectedFlight = if(flights.size == 1){
            flights.first()
        } else {
            null
        }

        selectedFlights.apply {
            clear()
            addAll(flights)
        }
    }

}