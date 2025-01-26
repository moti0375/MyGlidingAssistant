package com.bartovapps.gpstriprec.core.map_helper

import android.net.Uri
data class ImageMarker (
    val imageUri: Uri? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String? = null)
