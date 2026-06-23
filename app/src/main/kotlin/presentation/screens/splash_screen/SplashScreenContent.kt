package presentation.screens.splash_screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.presentation.screens.splash_screen.PermissionsViewModel
import kotlinx.coroutines.delay

private const val SPLASH_TIMEOUT = 3500L

@Composable
fun SplashScreenContent(
    viewModel: PermissionsViewModel,
    onNavigateToMain: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val currentStep by viewModel.currentStep.collectAsState()
    val navigateToMain by viewModel.navigateToMain.collectAsState()
    val shouldFinish by viewModel.shouldFinish.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onStepResult(isGranted)
    }

    // Init the permission chain once
    LaunchedEffect(Unit) {
        fun isGranted(permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        viewModel.initPermissionChain(
            hasLocation = isGranted(Manifest.permission.ACCESS_FINE_LOCATION),
            hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                isGranted(Manifest.permission.POST_NOTIFICATIONS) else true,
            hasMediaImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                isGranted(Manifest.permission.READ_MEDIA_IMAGES) else true,
            hasStorage = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) else true
        )
    }

    // Wait for splash duration then navigate
    LaunchedEffect(navigateToMain) {
        if (navigateToMain) {
            delay(SPLASH_TIMEOUT)
            onNavigateToMain()
        }
    }

    // Exit if a mandatory permission was denied
    LaunchedEffect(shouldFinish) {
        if (shouldFinish) onFinish()
    }

    // Splash image
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.gliding_assistant_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }

    // Permission rationale dialog
    currentStep?.let { step ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(step.title) },
            text = { Text(step.message) },
            confirmButton = {
                TextButton(onClick = { permissionLauncher.launch(step.permission) }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onStepResult(false) }) {
                    Text("Refuse")
                }
            }
        )
    }
}