package com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen

import data.model.Flight
import presentation.screens.flight_details_screen.FlightDetailsState

sealed class FlightsListState {
    data object Initiated : FlightsListState()
    data object Loading : FlightsListState()
    data class FlightsLoaded( val flights: List<Flight>) : FlightsListState()
}