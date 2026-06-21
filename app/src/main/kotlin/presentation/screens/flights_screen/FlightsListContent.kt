package com.dunihuliapps.myglidingassistnat.presentation.screens.flights_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.HmsFormatter
import data.model.Flight
import java.io.File

private const val TRIP_NAME_MAX_LENGTH = 25

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightsListContent(
    viewModel: FlightsListViewModel,
    onFlightClick: (Flight) -> Unit,
    onUploadConfirmed: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val selectedFlightIds by viewModel.selectedFlightIds.collectAsState()
    val editingFlight by viewModel.editSelectedFlight.collectAsState()
    val gliders by viewModel.gliders.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.mapEventToState(FlightsListEvent.GetAllFlights)
    }

    val inSelectionMode = selectedFlightIds.isNotEmpty()
    val flights = (state as? FlightsListState.FlightsLoaded)?.flights ?: emptyList()

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Flight") },
            text = { Text(stringResource(R.string.DeleteDialog)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.mapEventToState(FlightsListEvent.DeleteSelectedFlights)
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.YES)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.NO))
                }
            }
        )
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(stringResource(R.string.UploadTrip)) },
            confirmButton = {
                TextButton(onClick = {
                    showUploadDialog = false
                    onUploadConfirmed()
                }) { Text(stringResource(R.string.YES)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUploadDialog = false
                    viewModel.mapEventToState(FlightsListEvent.ClearSelectedFlight)
                }) { Text(stringResource(R.string.NO)) }
            }
        )
    }

    editingFlight?.let { flight ->
        EditFlightDetailsDialog(
            flight = flight,
            gliders = gliders,
            onConfirm = { updatedFlight ->
                viewModel.mapEventToState(FlightsListEvent.UpdateFlightDetails(updatedFlight))
                viewModel.mapEventToState(FlightsListEvent.DismissEditFlight)
                viewModel.mapEventToState(FlightsListEvent.ClearSelectedFlight)
            },
            onDismiss = {
                viewModel.mapEventToState(FlightsListEvent.DismissEditFlight)
                viewModel.mapEventToState(FlightsListEvent.ClearSelectedFlight)
            }
        )
    }

    Scaffold(
        topBar = {
            if (inSelectionMode) {
                TopAppBar(
                    title = { Text("Selected ${selectedFlightIds.size}") },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.mapEventToState(FlightsListEvent.ClearSelectedFlight)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                        if (selectedFlightIds.size == 1) {
                            IconButton(onClick = {
                                viewModel.mapEventToState(FlightsListEvent.EditFlightClicked)
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { showUploadDialog = true }) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Upload")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Recorded Flights") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (flights.isEmpty() && state is FlightsListState.FlightsLoaded) {
            Box(modifier = Modifier.padding(paddingValues)) {
                EmptyFlightsContent()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(flights, key = { it.id }) { flight ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.mapEventToState(FlightsListEvent.DeleteFlight(flight))
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        gesturesEnabled = !inSelectionMode,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    ) {
                        FlightItem(
                            flight = flight,
                            isSelected = flight.id in selectedFlightIds,
                            onClick = {
                                if (inSelectionMode) {
                                    viewModel.mapEventToState(
                                        FlightsListEvent.ToggleFlightSelection(flight.id)
                                    )
                                } else {
                                    onFlightClick(flight)
                                }
                            },
                            onLongClick = {
                                viewModel.mapEventToState(
                                    FlightsListEvent.ToggleFlightSelection(flight.id)
                                )
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightItem(
    flight: Flight,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val timeFormatter = remember { HmsFormatter() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(50.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            AsyncImage(
                model = flight.imageFileName?.let { File(it) },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_google_map_hdpi_active),
                placeholder = painterResource(R.drawable.ic_google_map_hdpi_active),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = flight.date ?: "",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!flight.name.isNullOrEmpty()) {
                    Text(
                        text = flight.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!flight.firstPilot.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = flight.firstPilot,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (!flight.secondPilot.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = flight.secondPilot,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (!flight.glider.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_glider_icon),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = flight.glider,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeFormatter.formatTime(flight.duration),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun EmptyFlightsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_no_flights_image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nothing Here",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFlightDetailsDialog(
    flight: Flight,
    gliders: List<Glider>,
    onConfirm: (Flight) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(flight.id) { mutableStateOf(flight.name ?: "") }
    var selectedGliderCallsign by remember(flight.id) { mutableStateOf(flight.glider) }
    var firstPilot by remember(flight.id) { mutableStateOf(flight.firstPilot ?: "") }
    var secondPilot by remember(flight.id) { mutableStateOf(flight.secondPilot ?: "") }
    var gliderDropdownExpanded by remember { mutableStateOf(false) }

    val selectedGlider = gliders.find { it.callsign == selectedGliderCallsign }
    val isTwoSeater = selectedGlider?.seats == 2
    val selectedGliderLabel = selectedGlider
        ?.let { "${it.callsign} (${it.type})" }
        ?: selectedGliderCallsign
        ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_glider_icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Edit flight details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Flight name
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { if (it.length <= TRIP_NAME_MAX_LENGTH) name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.EnterTripTitle)) },
                    trailingIcon = {
                        Text(
                            text = "${name.length}/$TRIP_NAME_MAX_LENGTH",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                )

                // Glider picker
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

                // First pilot
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = firstPilot,
                    onValueChange = { firstPilot = it },
                    singleLine = true,
                    label = { Text("First pilot") }
                )

                // Second pilot — only for 2-seat gliders
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
                    flight.copy(
                        name = name.takeIf { it.isNotBlank() },
                        glider = selectedGliderCallsign,
                        firstPilot = firstPilot.takeIf { it.isNotBlank() },
                        secondPilot = if (isTwoSeater) secondPilot.takeIf { it.isNotBlank() } else null
                    )
                )
            }) { Text(stringResource(R.string.Done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        }
    )
}