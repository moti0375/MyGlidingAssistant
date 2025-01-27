package com.bartovapps.gpstriprec.presentation.screens.main_screen

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import domain.trip_manager.TripManager
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(private val tripManager: TripManager) :
    ViewModel() {


}