package presentation.map
import com.dunihuliapps.myglidingassistnat.domain.map_helper.ImageMarker
import com.google.android.gms.maps.model.Marker

interface InfoWindowClickListener {
    fun onInfoWindowClicked(marker: Marker, imageMarker: ImageMarker?)
}