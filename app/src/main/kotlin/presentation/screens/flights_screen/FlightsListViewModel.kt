package presentation.screens.flights_screen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.data.repositories.flights.FlightsRepository
import com.dunihuliapps.myglidingassistnat.data.repositories.gliders.GlidersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Flight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlightsListViewModel @Inject constructor(
    private val flightsRepository: FlightsRepository,
    private val glidersRepository: GlidersRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FlightsListState>(FlightsListState.Initiated)
    val state = _state.asStateFlow()

    private val _editSelectedFlight = MutableStateFlow<Flight?>(null)
    val editSelectedFlight = _editSelectedFlight.asStateFlow()

    private val _selectedFlightIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFlightIds = _selectedFlightIds.asStateFlow()

    val gliders: StateFlow<List<Glider>> = glidersRepository.getAllGliders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Kept for legacy delete/edit path
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
                is FlightsListEvent.UpdateFlightDetails -> updateFlightDetails(event.flight)
                is FlightsListEvent.OnFlightSelected -> onFlightSelected(event.flight)
                is FlightsListEvent.EditFlightClicked -> onEditFlightClicked()
                is FlightsListEvent.ToggleFlightSelection -> toggleFlightId(event.flightId)
                is FlightsListEvent.DismissEditFlight -> _editSelectedFlight.value = null
            }
        }
    }

    private fun toggleFlightId(flightId: Long) {
        _selectedFlightIds.update { current ->
            if (flightId in current) current - flightId else current + flightId
        }
        val ids = _selectedFlightIds.value
        val currentFlights = (state.value as? FlightsListState.FlightsLoaded)?.flights ?: emptyList()
        selectedFlights.apply {
            clear()
            addAll(currentFlights.filter { it.id in ids })
        }
        selectedFlight = if (ids.size == 1) selectedFlights.firstOrNull() else null
    }

    private fun onEditFlightClicked() {
        selectedFlight?.let { _editSelectedFlight.value = it }
    }

    private fun clearSelectedFlights() {
        selectedFlights.clear()
        selectedFlight = null
        _selectedFlightIds.value = emptySet()
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
        clearSelectedFlights()
        getAllFlights()
    }

    private suspend fun updateFlightName(flight: Flight, name: String) {
        flightsRepository.updateFlightName(flight.id, name)
        getAllFlights()
    }

    private suspend fun updateFlightDetails(flight: Flight) {
        flightsRepository.update(flight)
        getAllFlights()
    }

    private suspend fun onFlightSelected(flight: Flight) {
        selectedFlight = flight
    }

    private suspend fun toggleSelectedFlight(flights: List<Flight>) {
        selectedFlight = if (flights.size == 1) flights.first() else null
        selectedFlights.apply {
            clear()
            addAll(flights)
        }
    }
}