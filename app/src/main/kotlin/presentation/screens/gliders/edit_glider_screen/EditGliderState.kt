package presentation.screens.gliders.edit_glider_screen
data class EditGliderScreenState(
    val type: String?,
    val callsign: String?,
    val seats: Int,
    val ratio: Int,
    val image: String?
)