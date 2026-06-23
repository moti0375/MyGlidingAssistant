package com.dunihuliapps.myglidingassistnat.presentation.screens.gliders.edit_glider_screen

sealed class EditGliderEvent {
    data class OnTypeChange(val type: String) : EditGliderEvent()
    data class OnCallsignChange(val callsign: String) : EditGliderEvent()
    data class OnSeatsChange(val seats: Int) : EditGliderEvent()
    data class OnRatioChange(val ratio: Int) : EditGliderEvent()
    data class OnImageTaken(val imagePath: String) : EditGliderEvent()
    object Save : EditGliderEvent()
}