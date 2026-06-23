package presentation.screens.flights_screen
import data.model.Flight

sealed class FlightsListState {
    data object Initiated : FlightsListState()
    data object Loading : FlightsListState()
    data class FlightsLoaded( val flights: List<Flight>) : FlightsListState()
}