package com.bartovapps.gpstriprec.core.map_helper

import android.net.Uri
data class ImageMarker (
    var imageUri: Uri? = null,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var description: String? = null)
