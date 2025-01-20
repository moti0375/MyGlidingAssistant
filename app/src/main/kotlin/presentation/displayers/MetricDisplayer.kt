package com.bartovapps.gpstriprec.displayers

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.TextView
import com.bartovapps.gpstriprec.presentation.displayers.DataDisplayer

@SuppressLint("DefaultLocale")
class MetricDisplayer : DataDisplayer {
    private val distanceBuilder: StringBuilder = StringBuilder()
    private val unitBuilder: StringBuilder = StringBuilder("M")

    override fun displayData(view: TextView, data: Double) {
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
        view.text = ss1
    }
}