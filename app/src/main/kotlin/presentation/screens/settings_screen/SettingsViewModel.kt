package com.bartovapps.gpstriprec.presentation.screens.settings_screen

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _preferenceChanged = MutableLiveData<String>()
    val preferenceChanged: LiveData<String> = _preferenceChanged

    fun onPreferenceChanged(key: String) {
        _preferenceChanged.value = key
        // Add logic here to update app behavior based on specific keys

        Log.i("SettingsViewModel", "onPreferenceChanged: $key")
    }
}