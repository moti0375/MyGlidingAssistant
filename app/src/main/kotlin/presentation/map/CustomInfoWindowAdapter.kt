package com.bartovapps.gpstriprec.presentation.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.bartovapps.gpstriprec.R
import com.bartovapps.gpstriprec.domain.map_helper.ImageMarker
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker
import java.io.File
import java.io.IOException
import kotlin.math.min

class CustomInfoWindowAdapter(
    inflater: LayoutInflater,
    private val markersIdsMap: MutableMap<String, ImageMarker>,
) :
    InfoWindowAdapter {
    // These a both viewgroups containing an ImageView with id "badge" and two TextViews with id
    // "title" and "snippet".
    private val mWindow: View = inflater.inflate(R.layout.custom_info_window, null)

    override fun getInfoWindow(marker: Marker): View {
        render(marker, mWindow)
        return mWindow
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    private fun render(marker: Marker, view: View) {
        Log.i(LOG_TAG, "render function was called. marker id: " + marker.id)
        val imageView = view.findViewById<View>(R.id.markerImage) as ImageView
        if (markersIdsMap[marker.id] == null) {  //This means it's the a marker with no image.. (like trip start and end locations)
            imageView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    view.context.resources,
                    R.drawable.ic_launcher,
                    view.context.theme
                )
            )
        } else {
            val imageUri = markersIdsMap[marker.id]?.imageUri
            imageUri?.let {uri ->
                val imagePath = uri.path
                if(imagePath != null){
                    val imgFile = File(imagePath)
                    if (!imgFile.exists()) {  //This can happen if user erased the image from gallery
                        imageView.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                view.context.resources,
                                R.drawable.image_broken,
                                view.context.theme
                            )
                        )
                    } else {
                        val reducedSizeImage = getReducedImage(view.context, imagePath)
                        imageView.setImageBitmap(rotateImage(reducedSizeImage, imagePath))
                    }
                }
            }
        }
    }

    private fun getReducedImage(context: Context, imageFileLocation: String?): Bitmap {
        Log.i(LOG_TAG, "image file path: $imageFileLocation")
        val targetImageWidth = context.resources.getDimension(R.dimen.image_marker_width).toInt()
        val targetImageHeight = context.resources.getDimension(R.dimen.image_marker_height).toInt()

        Log.i(
            LOG_TAG,
            "image view sizes: $targetImageWidth, $targetImageHeight"
        )

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFileLocation, options)
        val cameraImageWidth = options.outWidth
        val cameraImageHeight = options.outHeight

        val scaleFactor = min(
            (cameraImageWidth / targetImageWidth).toDouble(),
            (cameraImageHeight / targetImageHeight).toDouble()
        ).toInt()
        options.inSampleSize = scaleFactor
        options.inJustDecodeBounds = false

        val reducedSizeImage = BitmapFactory.decodeFile(imageFileLocation, options)
        return reducedSizeImage
    }


    private fun rotateImage(bitmap: Bitmap, imageFileLocation: String): Bitmap {
        val matrix = Matrix()

        val rotation = getImageRotation(imageFileLocation)
        matrix.setRotate(rotation.toFloat())

        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return rotatedBitmap
    }


    private fun getImageRotation(imageFileLocation: String): Int {
        var exifInterface: ExifInterface? = null

        try {
            exifInterface = ExifInterface(imageFileLocation)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val orientation = exifInterface?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        Log.i(LOG_TAG, "image orientation: $orientation")


        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0

        }
    }

    companion object {
        const val LOG_TAG = "CustomInfoWindowAdapter"
    }
}
