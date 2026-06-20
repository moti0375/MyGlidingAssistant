package com.dunihuliapps.myglidingassistnat.presentation.screens.gliders.edit_glider_screen

import androidx.activity.result.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.data.repositories.gliders.GlidersRepository
import com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen.FlightsListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditGliderViewModel @Inject constructor(
    private val repository: GlidersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(EditGliderScreenState(
        type = savedStateHandle.get<String>("type"),
        callsign = savedStateHandle.get<String>("callsign"),
        seats = savedStateHandle.get<Int>("seats") ?: DEFAULT_SEATS,
        ratio = savedStateHandle.get<Int>("ratio") ?: DEFAULT_RATIO,
        image = savedStateHandle.get<String>("image")
    ))
    val state = _state.asStateFlow()

    fun mapEventToState(event: EditGliderEvent){
        when (event){
            is EditGliderEvent.OnTypeChange -> _state.value = _state.value.copy(type = event.type)
            is EditGliderEvent.OnCallsignChange -> _state.value = _state.value.copy(callsign = event.callsign)
            is EditGliderEvent.OnSeatsChange -> _state.value = _state.value.copy(seats = event.seats)
            is EditGliderEvent.OnRatioChange -> _state.value = _state.value.copy(ratio = event.ratio)
            is EditGliderEvent.OnImageTaken ->  {
                _state.value =  _state.value.copy(image = event.imageUri.toString())
            }
            is EditGliderEvent.Save -> save()
        }
    }

    fun save() {
        viewModelScope.launch {
            repository.insertGlider(
                Glider(type = _state.value.type ?: "", callsign = _state.value.callsign ?: "", seats = _state.value.seats, ratio = _state.value.ratio))
        }
    }

    companion object {
        const val DEFAULT_RATIO = 20
        const val DEFAULT_SEATS = 1
    }
}