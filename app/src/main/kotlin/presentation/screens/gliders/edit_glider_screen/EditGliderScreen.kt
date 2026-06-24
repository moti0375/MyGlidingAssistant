package presentation.screens.gliders.edit_glider_screen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.dunihuliapps.myglidingassistant.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGliderScreen(viewModel: EditGliderViewModel, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state by viewModel.state.collectAsState()

    val gliderImagesDir = remember {
        File(context.filesDir, "glider_images").apply { mkdirs() }
    }

    val tempCameraFile = remember {
        File(gliderImagesDir, "glider_${System.currentTimeMillis()}.jpg")
    }

    val tempUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempCameraFile
        )
    }

    // Activity Result Launcher for CameraActivity
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val destFile = File(gliderImagesDir, "glider_${System.currentTimeMillis()}.jpg")
                val copied = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(it)?.use { input ->
                            FileOutputStream(destFile).use { output -> input.copyTo(output) }
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
                if (copied) {
                    viewModel.mapEventToState(EditGliderEvent.OnImageTaken(destFile.absolutePath))
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.mapEventToState(EditGliderEvent.OnImageTaken(tempCameraFile.absolutePath))
        }
    }

    var showImagePickerOptions by remember { mutableStateOf(false) }

    // Launcher for Gallery

    if (showImagePickerOptions) {
        AlertDialog(
            onDismissRequest = { showImagePickerOptions = false },
            title = { Text("Select Image Source") },
            text = { Text("Would you like to take a new photo or choose one from your gallery?") },
            confirmButton = {
                TextButton(onClick = {
                    galleryLauncher.launch("image/*")
                    showImagePickerOptions = false
                }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    cameraLauncher.launch(tempUri)
                    showImagePickerOptions = false
                }) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        // --- TOP AREA (1/3 Ratio) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Weight 1 of 3 (approx 33%)
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.clickable {
                    // Intent to your custom CameraActivity
                    showImagePickerOptions = true
                },
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(160.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    val imageUri = state.image
                    imageUri?.let {
                        AsyncImage(
                            model = File(imageUri),
                            contentDescription = "Taken Glider Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: run {
                        Image(
                            painter = painterResource(id = R.drawable.ic_glider_icon),
                            contentDescription = null,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                    // Use a placeholder if gliderImage is null

                }

                // Camera Badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Image",
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // --- LOWER AREA (2/3 Ratio) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f) // Weight 2 of 3 (approx 66%)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Glider Details", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = state.type ?: "",
                onValueChange = {
                    viewModel.mapEventToState(EditGliderEvent.OnTypeChange(it))
                },
                label = { Text("Type (e.g. ASW-28)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.callsign ?: "",
                onValueChange = {
                    viewModel.mapEventToState(EditGliderEvent.OnCallsignChange(it))
                },
                label = { Text("Callsign") },
                modifier = Modifier.fillMaxWidth()
            )

            // Seats Selection (1 or 2)
            Column {
                Text("Seats", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2).forEach { option ->
                        FilterChip(
                            selected = state.seats == option,
                            onClick = {
                                viewModel.mapEventToState(EditGliderEvent.OnSeatsChange(option))
                            },
                            label = { Text("$option Seat${if (option > 1) "s" else ""}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Ratio Selection (Slider 15 - 100)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Glide Ratio", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "1:${state.ratio}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Slider(
                    value = state.ratio.toFloat(),
                    onValueChange = {
                        viewModel.mapEventToState(EditGliderEvent.OnRatioChange(it.toInt()))
                    },
                    valueRange = 15f..100f,
                    steps = 85
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.save()
                    onSaved()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Save Glider")
            }
        }
    }
    }
}