package presentation.screens.settings_screen
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(viewModel: SettingsViewModel, onNavigateUp: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            item {
                PreferenceCategory(title = "Units")
                PreferenceListItem(
                    title = "Distance Units",
                    currentValue = state.distanceUnits,
                    entries = listOf("Metric", "Miles"),
                    values = listOf("1", "2"),
                    onValueSelected = { viewModel.updatePreference("distance_units", it) }
                )
                PreferenceListItem(
                    title = "Speed Units",
                    currentValue = state.speedUnits,
                    entries = listOf("Metric", "Knots"),
                    values = listOf("1", "2"),
                    onValueSelected = { viewModel.updatePreference("speed_units", it) }
                )
                PreferenceListItem(
                    title = "Altitude Units",
                    currentValue = state.altitudeUnits,
                    entries = listOf("Feet", "Metric"),
                    values = listOf("1", "2"),
                    onValueSelected = { viewModel.updatePreference("altitudeUnits", it) }
                )

                PreferenceCategory(title = "Map")
                PreferenceListItem(
                    title = "Line Color",
                    currentValue = state.lineColor,
                    entries = listOf("Red", "Green", "Yellow", "Blue"),
                    values = listOf("1", "2", "3", "4"),
                    onValueSelected = { viewModel.updatePreference("LineColor", it) }
                )
                PreferenceListItem(
                    title = "Line Width",
                    currentValue = state.lineWidth,
                    entries = listOf("Thin", "Thick", "Thicker"),
                    values = listOf("5", "10", "15"),
                    onValueSelected = { viewModel.updatePreference("lineWidth", it) }
                )
                PreferenceListItem(
                    title = "Map Zoom",
                    currentValue = state.zoom,
                    entries = listOf("State", "City", "Block", "Street"),
                    values = listOf("6", "12", "15", "20"),
                    onValueSelected = { viewModel.updatePreference("zoom", it) }
                )

                PreferenceCategory(title = "Saving Options")
                PreferenceListItem(
                    title = "Auto Save",
                    currentValue = state.autoSave,
                    entries = listOf("Automatic save trip when done", "Ask me to save trip"),
                    values = listOf("0", "1"),
                    onValueSelected = { viewModel.updatePreference("AutoSavePrefKey", it) }
                )
            }
        }
    }
}

@Composable
private fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun PreferenceListItem(
    title: String,
    currentValue: String,
    entries: List<String>,
    values: List<String>,
    onValueSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val currentLabel = entries.getOrElse(values.indexOf(currentValue)) { currentValue }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = currentLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    entries.forEachIndexed { index, entry ->
                        val value = values.getOrElse(index) { entry }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueSelected(value)
                                    showDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentValue == value,
                                onClick = {
                                    onValueSelected(value)
                                    showDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(entry, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
