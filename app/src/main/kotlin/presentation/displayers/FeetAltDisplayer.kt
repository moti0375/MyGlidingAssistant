package com.bartovapps.gpstriprec.displayers

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.TextView
import com.bartovapps.gpstriprec.presentation.displayers.DataDisplayer
import java.text.DecimalFormat

class FeetAltDisplayer : DataDisplayer {
    private val distanceBuilder: StringBuilder = StringBuilder()
    private val unitBuilder: StringBuilder = StringBuilder("Ft")
    private val numFormat: DecimalFormat = DecimalFormat("#,###.##")

    override fun displayData(view: TextView, data: Double) {
        distanceBuilder.replace(0, distanceBuilder.length, numFormat.format(data * 3.28084))
        val ss1 = SpannableString(distanceBuilder.toString() + unitBuilder.toString())
        ss1.setSpan(RelativeSizeSpan(0.5f), distanceBuilder.length, ss1.length, 0) // set size
        ss1.setSpan(
            ForegroundColorSpan(Color.RED),
            distanceBuilder.length,
            ss1.length,
            0
        ) // set color


//		view.setText( String.format("%.1f", (value * 3.6)) + ss1);
        view.text = ss1
    }
}