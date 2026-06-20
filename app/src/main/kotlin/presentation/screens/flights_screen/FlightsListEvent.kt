package com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen

import data.model.Flight

sealed class FlightsListEvent {
    object GetAllFlights : FlightsListEvent()
    data class DeleteFlight(val flight: Flight) : FlightsListEvent()
    object DeleteSelectedFlights : FlightsListEvent()

    data class OnFlightSelected(val flight: Flight) : FlightsListEvent()
    data class UpdateSelectedFlights(val flights: List<Flight>) : FlightsListEvent()
    object ClearSelectedFlight : FlightsListEvent()
    object EditFlightClicked : FlightsListEvent()

    data class UpdateFlightName(val flight: Flight, val name: String) : FlightsListEvent()
}