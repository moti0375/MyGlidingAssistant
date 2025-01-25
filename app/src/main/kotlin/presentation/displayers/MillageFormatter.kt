package com.bartovapps.gpstriprec.presentation.displayers
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import java.util.Locale

class MillageFormatter : UnitsFormatter {
    private val distanceBuilder: StringBuilder = StringBuilder()
    private val unitBuilder: StringBuilder = StringBuilder("Mi")

    override fun formatUnits(data: Double) : SpannableString{
        val miles = data / 1609.34
        distanceBuilder.replace(0, distanceBuilder.length, String.format(Locale.getDefault(), "%.1f", miles))
        val ss1 = SpannableString(distanceBuilder.toString() + unitBuilder.toString())
        ss1.setSpan(RelativeSizeSpan(0.5f), distanceBuilder.length, ss1.length, 0) // set size
        ss1.setSpan(
            ForegroundColorSpan(Color.RED),
            distanceBuilder.length,
            ss1.length,
            0
        )

        return ss1
    }
}