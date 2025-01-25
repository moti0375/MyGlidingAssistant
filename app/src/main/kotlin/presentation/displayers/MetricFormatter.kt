package com.bartovapps.gpstriprec.presentation.displayers
import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan

@SuppressLint("DefaultLocale")
class MetricFormatter : UnitsFormatter {
    private val distanceBuilder: StringBuilder = StringBuilder()
    private val unitBuilder: StringBuilder = StringBuilder("M")

    override fun formatUnits( data: Double) : SpannableString{
        var distance = data
        if (distance < 1000) {
            unitBuilder.replace(0, unitBuilder.length, "M")
        } else {
            distance /= 1000.0
            unitBuilder.replace(0, unitBuilder.length, "KM")
        }

        distanceBuilder.replace(0, distanceBuilder.length, String.format("%.1f", distance))
        val ss1 = SpannableString(distanceBuilder.toString() + unitBuilder.toString())
        ss1.setSpan(RelativeSizeSpan(0.5f), distanceBuilder.length, ss1.length, 0) // set size
        ss1.setSpan(
            ForegroundColorSpan(Color.RED),
            distanceBuilder.length,
            ss1.length,
            0
        ) // set color
        return ss1
    }

}