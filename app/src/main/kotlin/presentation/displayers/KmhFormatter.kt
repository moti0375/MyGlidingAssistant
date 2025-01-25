package com.bartovapps.gpstriprec.presentation.displayers

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import java.util.Locale


class KmhFormatter : UnitsFormatter {
    private val speedBuilder: StringBuilder = StringBuilder()
    private val unitBuilder: StringBuilder = StringBuilder("KM/H")
    override fun formatUnits(data: Double) : SpannableString{
        // km\h = m\sec * 3.6
        speedBuilder.replace(0, speedBuilder.length, String.format(Locale.getDefault(),"%.1f", (data * 3.6)))
        val ss1 = SpannableString(speedBuilder.toString() + unitBuilder.toString())
        ss1.setSpan(RelativeSizeSpan(0.5f), speedBuilder.length, ss1.length, 0) // set size
        ss1.setSpan(ForegroundColorSpan(Color.RED), speedBuilder.length, ss1.length, 0) // set color
        return ss1
    }
}