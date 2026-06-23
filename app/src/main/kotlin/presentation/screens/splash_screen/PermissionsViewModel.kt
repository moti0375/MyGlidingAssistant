package com.dunihuliapps.myglidingassistnat.presentation.screens.splash_screen

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor() : ViewModel() {

    data class PermissionStep(
        val permission: String,
        val title: String,
        val message: String,
        val isMandatory: Boolean
    )

    private val _currentStep = MutableStateFlow<PermissionStep?>(null)
    val currentStep: StateFlow<PermissionStep?> = _currentStep.asStateFlow()

    private val _navigateToMain = MutableStateFlow(false)
    val navigateToMain: StateFlow<Boolean> = _navigateToMain.asStateFlow()

    private val _shouldFinish = MutableStateFlow(false)
    val shouldFinish: StateFlow<Boolean> = _shouldFinish.asStateFlow()

    private val steps = mutableListOf<PermissionStep>()
    private var stepIndex = 0

    fun initPermissionChain(
        hasLocation: Boolean,
        hasNotifications: Boolean,
        hasMediaImages: Boolean,
        hasStorage: Boolean
    ) {
        steps.clear()
        stepIndex = 0

        // 1. Location (Mandatory)
        if (!hasLocation) {
            steps.add(PermissionStep(
                Manifest.permission.ACCESS_FINE_LOCATION,
                "Location Access",
                "This app needs location access to track and record your trips accurately.",
                true
            ))
        }

        // 2. Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifications) {
            steps.add(PermissionStep(
                Manifest.permission.POST_NOTIFICATIONS,
                "Notifications",
                "We need notification access to show your recording status in the background.",
                false
            ))
        }

        // 3. Photos/Media (Android 13+) or Storage (Legacy)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasMediaImages) {
                steps.add(PermissionStep(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    "Photo Access",
                    "Access is needed to attach photos to your recorded trips.",
                    false
                ))
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasStorage) {
            steps.add(PermissionStep(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "Storage Access",
                "Storage access is required to save your trip KML files.",
                false
            ))
        }

        processNext()
    }

    fun onStepResult(isGranted: Boolean) {
        val current = steps.getOrNull(stepIndex)
        if (!isGranted && current?.isMandatory == true) {
            _currentStep.value = null
            _shouldFinish.value = true
        } else {
            stepIndex++
            processNext()
        }
    }

    private fun processNext() {
        if (stepIndex < steps.size) {
            _currentStep.value = steps[stepIndex]
        } else {
            _navigateToMain.value = true
        }
    }
}
