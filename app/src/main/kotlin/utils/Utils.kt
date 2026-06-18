package com.dunihuliapps.myglidingassistnat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import java.io.File

object Utils {
    private val LOG_TAG: String = Utils::class.java.simpleName

    fun getApplicationVersion(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.applicationContext.packageName, 0).versionName
    }.getOrNull()

    fun isFileExists(fileName: String?): Boolean {
        if (fileName == null) return false
        return File(fileName).exists()
    }


    fun overlay(bmp1: Bitmap, bmp2: Bitmap, text: String): Bitmap? {
        try {
            val maxWidth = (if (bmp1.width > bmp2.width) bmp1.width else bmp2.width)
            val maxHeight = (if (bmp1.height > bmp2.height) bmp1.height else bmp2.height)
            val bmOverlay = Bitmap.createBitmap(maxWidth, maxHeight, bmp1.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmOverlay)
            canvas.drawBitmap(bmp1, 0f, 0f, null)
            canvas.drawBitmap(bmp2, 0f, 0f, null)

            val paint = Paint()
            paint.color = Color.BLACK // Text Color
            Log.i(LOG_TAG, "Logo width: " + bmp2.width + ", Logo height: " + bmp2.height)
            Log.i(
                LOG_TAG,
                "Logo scaled width: " + bmp2.getScaledWidth(canvas) + ", scaled height: " + bmp2.getScaledHeight(
                    canvas
                )
            )
            paint.strokeWidth = (bmp2.width * 4).toFloat() // Text Size
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER) // Text Overlapping Pattern

            // some more settings...
            canvas.drawBitmap(bmOverlay, 0f, 0f, paint)
            canvas.drawText(text, (bmp2.width * 0.7).toFloat(), (bmp2.width * 0.7).toFloat(), paint)


            return bmOverlay
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}