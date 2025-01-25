package com.bartovapps.gpstriprec.presentation.displayers
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import java.text.DecimalFormat

class FeetFormatter : UnitsFormatter {
    private val distanceBuilder: StringBuilder = StringBuilder()
    private val unitBuilder: StringBuilder = StringBuilder("Ft")
    private val numFormat: DecimalFormat = DecimalFormat("#,###.##")
    override fun formatUnits( data: Double): SpannableString{
        distanceBuilder.replace(0, distanceBuilder.length, numFormat.format(data * 3.28084))
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