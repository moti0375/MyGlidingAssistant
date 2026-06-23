package presentation.screens.main_screen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import presentation.composables.main_screen.GaugesPanel
import presentation.composables.main_screen.GliderToolbarIcon
import presentation.composables.main_screen.MainMapContainer
import presentation.composables.main_screen.MetricIndicator
import presentation.map.CustomSupportMapFragment

private enum class UnitDialogType { Speed, Distance, Altitude }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    timerText: AnnotatedString,
    speedText: AnnotatedString,
    distanceText: AnnotatedString,
    altitudeText: AnnotatedString,
    isRecording: Boolean,
    showSaveDialog: Boolean,
    showNewFlightDialog: Boolean,
    gliders: List<Glider>,
    initialFlightGlider: String?,
    initialFlightFirstPilot: String,
    initialFlightSecondPilot: String,
    isLoading: Boolean,
    loadingMessage: String,
    speedUnitValue: String,
    distanceUnitValue: String,
    altitudeUnitValue: String,
    onUnitChanged: (key: String, value: String) -> Unit,
    onStartStopClick: () -> Unit,
    onTakeOffConfirmed: (glider: String?, firstPilot: String?, secondPilot: String?) -> Unit,
    onNewFlightDismiss: (glider: String?, firstPilot: String, secondPilot: String) -> Unit,
    onFlightsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onGlidersClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onMapReady: (CustomSupportMapFragment) -> Unit,
    onGaugesHeightChanged: (Int) -> Unit,
    onAltitudeHeightChanged: (Int) -> Unit,
    onSaveConfirm: () -> Unit,
    onSaveCancel: () -> Unit,
    onLoadingCancel: () -> Unit,
) {
    var activeUnitDialog by remember { mutableStateOf<UnitDialogType?>(null) }

    activeUnitDialog?.let { dialogType ->
        val (title, entries, values, currentValue, key) = when (dialogType) {
            UnitDialogType.Speed -> UnitDialogConfig(
                title = "Speed Units",
                entries = listOf("Metric (km/h)", "Knots"),
                values = listOf("1", "2"),
                currentValue = speedUnitValue,
                key = "speed_units"
            )
            UnitDialogType.Distance -> UnitDialogConfig(
                title = "Distance Units",
                entries = listOf("Metric (km)", "Miles"),
                values = listOf("1", "2"),
                currentValue = distanceUnitValue,
                key = "distance_units"
            )
            UnitDialogType.Altitude -> UnitDialogConfig(
                title = "Altitude Units",
                entries = listOf("Feet", "Metric (m)"),
                values = listOf("1", "2"),
                currentValue = altitudeUnitValue,
                key = "altitudeUnits"
            )
        }
        AlertDialog(
            onDismissRequest = { activeUnitDialog = null },
            title = { Text(title) },
            text = {
                Column {
                    entries.forEachIndexed { index, entry ->
                        val value = values[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentValue == value,
                                onClick = {
                                    onUnitChanged(key, value)
                                    activeUnitDialog = null
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(entry)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeUnitDialog = null }) { Text(stringResource(R.string.Cancel)) }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = onSaveCancel,
            title = { Text(stringResource(R.string.SAVE_TRIP)) },
            text = { Text(stringResource(R.string.SaveDialog)) },
            confirmButton = {
                TextButton(onClick = onSaveConfirm) { Text(stringResource(R.string.YES)) }
            },
            dismissButton = {
                TextButton(onClick = onSaveCancel) { Text(stringResource(R.string.NO)) }
            }
        )
    }

    if (isLoading) {
        AlertDialog(
            onDismissRequest = onLoadingCancel,
            title = { Text(stringResource(R.string.app_name)) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text(loadingMessage)
                }
            },
            confirmButton = {
                TextButton(onClick = onLoadingCancel) { Text(stringResource(R.string.Cancel)) }
            }
        )
    }

    if (showNewFlightDialog) {
        NewFlightDialog(
            gliders = gliders,
            initialGlider = initialFlightGlider,
            initialFirstPilot = initialFlightFirstPilot,
            initialSecondPilot = initialFlightSecondPilot,
            onConfirm = onTakeOffConfirmed,
            onDismiss = onNewFlightDismiss,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onFlightsClick) {
                        Icon(Icons.Default.Map, contentDescription = "Flights")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onGlidersClick) {
                        GliderToolbarIcon()
                    }
                    IconButton(onClick = onLicenseClick) {
                        Icon(Icons.Default.Info, contentDescription = "License")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            MainMapContainer(
                modifier = Modifier.fillMaxSize(),
                onFragmentReady = onMapReady
            )

            GaugesPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { onGaugesHeightChanged(it.size.height) }
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                timerText = timerText,
                speedText = speedText,
                distanceText = distanceText,
                isRecording = isRecording,
                onSpeedClick = { activeUnitDialog = UnitDialogType.Speed },
                onDistanceClick = { activeUnitDialog = UnitDialogType.Distance },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.End,
            ) {
                FloatingActionButton(
                    onClick = onStartStopClick,
                    modifier = Modifier.padding(end = 16.dp, bottom = 8.dp),
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Stop recording" else "Start recording"
                    )
                }
                MetricIndicator(
                    value = altitudeText,
                    endIcon = Icons.Default.Terrain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { onAltitudeHeightChanged(it.size.height) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    onClick = { activeUnitDialog = UnitDialogType.Altitude }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewFlightDialog(
    gliders: List<Glider>,
    initialGlider: String?,
    initialFirstPilot: String,
    initialSecondPilot: String,
    onConfirm: (glider: String?, firstPilot: String?, secondPilot: String?) -> Unit,
    onDismiss: (glider: String?, firstPilot: String, secondPilot: String) -> Unit,
) {
    var selectedGliderCallsign by remember { mutableStateOf(initialGlider) }
    var firstPilot by remember { mutableStateOf(initialFirstPilot) }
    var secondPilot by remember { mutableStateOf(initialSecondPilot) }
    var gliderDropdownExpanded by remember { mutableStateOf(false) }

    val selectedGlider = gliders.find { it.callsign == selectedGliderCallsign }
    val isTwoSeater = selectedGlider?.seats == 2
    val selectedGliderLabel = selectedGlider
        ?.let { "${it.callsign} (${it.type})" }
        ?: selectedGliderCallsign
        ?: ""

    AlertDialog(
        onDismissRequest = { onDismiss(selectedGliderCallsign, firstPilot, secondPilot) },
        icon = {
            GliderToolbarIcon()
        },
        title = { Text("New Flight") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = gliderDropdownExpanded,
                    onExpandedChange = { gliderDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        value = selectedGliderLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Glider") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = gliderDropdownExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = gliderDropdownExpanded,
                        onDismissRequest = { gliderDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedGliderCallsign = null
                                secondPilot = ""
                                gliderDropdownExpanded = false
                            }
                        )
                        gliders.forEach { glider ->
                            DropdownMenuItem(
                                text = { Text("${glider.callsign} (${glider.type})") },
                                onClick = {
                                    selectedGliderCallsign = glider.callsign
                                    if (glider.seats != 2) secondPilot = ""
                                    gliderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = firstPilot,
                    onValueChange = { firstPilot = it },
                    singleLine = true,
                    label = { Text("First pilot") }
                )

                if (isTwoSeater) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = secondPilot,
                        onValueChange = { secondPilot = it },
                        singleLine = true,
                        label = { Text("Second pilot") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    selectedGliderCallsign,
                    firstPilot.takeIf { it.isNotBlank() },
                    if (isTwoSeater) secondPilot.takeIf { it.isNotBlank() } else null
                )
            }) { Text("Take Off") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(selectedGliderCallsign, firstPilot, secondPilot) }) {
                Text(stringResource(R.string.Cancel))
            }
        }
    )
}

private data class UnitDialogConfig(
    val title: String,
    val entries: List<String>,
    val values: List<String>,
    val currentValue: String,
    val key: String,
)
