package presentation.screens.main_screen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dunihuliapps.myglidingassistant.R
import presentation.composables.main_screen.GaugesPanel
import presentation.composables.main_screen.GliderToolbarIcon
import presentation.composables.main_screen.MainMapContainer
import presentation.composables.main_screen.MetricIndicator
import presentation.map.CustomSupportMapFragment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    timerText: AnnotatedString,
    speedText: AnnotatedString,
    distanceText: AnnotatedString,
    altitudeText: AnnotatedString,
    isRecording: Boolean,
    showSaveDialog: Boolean,
    isLoading: Boolean,
    loadingMessage: String,
    onStartStopClick: () -> Unit,
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
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}





