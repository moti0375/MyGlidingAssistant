package com.bartovapps.gpstriprec.presentation.displayers

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.TextView


class KmhDisplayer : DataDisplayer {
    private val speedBuilder: StringBuilder = StringBuilder()
    private val unitBuilder: StringBuilder = StringBuilder("KM/H")
    override fun displayData(view: TextView, value: Double) {
        // km\h = m\sec * 3.6
        speedBuilder.replace(0, speedBuilder.length, String.format("%.1f", (value * 3.6)))
        val ss1 = SpannableString(speedBuilder.toString() + unitBuilder.toString())
        ss1.setSpan(RelativeSizeSpan(0.5f), speedBuilder.length, ss1.length, 0) // set size
        ss1.setSpan(ForegroundColorSpan(Color.RED), speedBuilder.length, ss1.length, 0) // set color
        view.text = ss1
    }
}