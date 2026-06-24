package presentation.screens.gliders.edit_glider_screen
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dunihuliapps.myglidingassistnat.data.repositories.gliders.GlidersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditGliderViewModel @Inject constructor(
    private val repository: GlidersRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    init {
        Log.i("EditGliderViewModel", "init: glider id: ${savedStateHandle.get<Long>("id")}" )
    }

    private val _state = MutableStateFlow(EditGliderScreenState(
        type = savedStateHandle.get<String>("type"),
        callsign = savedStateHandle.get<String>("callsign"),
        seats = savedStateHandle.get<Int>("seats") ?: DEFAULT_SEATS,
        ratio = savedStateHandle.get<Int>("ratio") ?: DEFAULT_RATIO,
        image = savedStateHandle.get<String>("image")?.takeIf { it.isNotEmpty() }
    ))
    val state = _state.asStateFlow()
    val isEditMode: Boolean get() = (savedStateHandle.get<Long>("id") ?: 0L) > 0L

    fun mapEventToState(event: EditGliderEvent){
        when (event){
            is EditGliderEvent.OnTypeChange -> _state.value = _state.value.copy(type = event.type)
            is EditGliderEvent.OnCallsignChange -> _state.value = _state.value.copy(callsign = event.callsign)
            is EditGliderEvent.OnSeatsChange -> _state.value = _state.value.copy(seats = event.seats)
            is EditGliderEvent.OnRatioChange -> _state.value = _state.value.copy(ratio = event.ratio)
            is EditGliderEvent.OnImageTaken -> {
                _state.value = _state.value.copy(image = event.imagePath)
            }
            is EditGliderEvent.Save -> save {}
        }
    }

    fun save(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val gliderId = savedStateHandle.get<Long>("id") ?: 0L
            val image = _state.value.image?.takeIf { it.isNotEmpty() }
            if (gliderId > 0L) {
                repository.update(gliderId, type = _state.value.type ?: "", callsign = _state.value.callsign ?: "", seats = _state.value.seats, ratio = _state.value.ratio, gliderImage = image)
            } else {
                repository.insertGlider(type = _state.value.type ?: "", callsign = _state.value.callsign ?: "", seats = _state.value.seats, ratio = _state.value.ratio, gliderImage = image)
            }
            onComplete()
        }
    }

    companion object {
        const val DEFAULT_RATIO = 20
        const val DEFAULT_SEATS = 1
    }
}