package com.bartovapps.gpstriprec.presentation.map

import com.bartovapps.gpstriprec.core.map_helper.ImageMarker
import com.google.android.gms.maps.model.Marker

interface InfoWindowClickListener {
    fun onInfoWindowClicked(marker: Marker, imageMarker: ImageMarker?)
}