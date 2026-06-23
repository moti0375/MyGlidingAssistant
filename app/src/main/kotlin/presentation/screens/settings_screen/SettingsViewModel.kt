package presentation.screens.settings_screen
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsState(
    val distanceUnits: String = "1",
    val speedUnits: String = "1",
    val altitudeUnits: String = "1",
    val lineColor: String = "1",
    val lineWidth: String = "5",
    val zoom: String = "15",
    val autoSave: String = "0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private fun loadState() = SettingsState(
        distanceUnits = prefs.getString("distance_units", "1") ?: "1",
        speedUnits = prefs.getString("speed_units", "1") ?: "1",
        altitudeUnits = prefs.getString("altitudeUnits", "1") ?: "1",
        lineColor = prefs.getString("LineColor", "1") ?: "1",
        lineWidth = prefs.getString("lineWidth", "5") ?: "5",
        zoom = prefs.getString("zoom", "15") ?: "15",
        autoSave = prefs.getString("AutoSavePrefKey", "0") ?: "0"
    )

    fun updatePreference(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        _state.value = loadState()
    }
}