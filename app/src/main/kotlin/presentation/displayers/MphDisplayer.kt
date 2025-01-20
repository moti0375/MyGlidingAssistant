package com.bartovapps.gpstriprec.displayers

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.TextView
import com.bartovapps.gpstriprec.presentation.displayers.DataDisplayer
import java.util.Locale

class MphDisplayer : DataDisplayer {
    private val speedBuilder: StringBuilder = StringBuilder()
    private val unitBuilder: StringBuilder = StringBuilder("Mi/H")

    override fun displayData(view: TextView, value: Double) {
        // 1mph = 1 m/sec * 2.23694
        val mph = value * 2.23694

        speedBuilder.replace(
            0, speedBuilder.length,
            String.format(String.format(Locale.getDefault(), "%.1f", mph))
        )
        val ss1 = SpannableString(
            speedBuilder.toString()
                    + unitBuilder.toString()
        )
        ss1.setSpan(
            RelativeSizeSpan(0.5f), speedBuilder.length,
            ss1.length, 0
        ) // set size
        ss1.setSpan(
            ForegroundColorSpan(Color.RED), speedBuilder.length,
            ss1.length, 0
        ) // set color

        view.text = ss1
    }
}