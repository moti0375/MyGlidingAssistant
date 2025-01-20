package com.bartovapps.gpstriprec.presentation.displayers

import android.widget.TextView
import java.util.Locale
import javax.inject.Inject

class HmsDisplayer @Inject constructor(): TimeDisplayer {
    override fun displayTime(view: TextView, millis: Long) {
        val seconds = (millis / 1000).toInt() % 60
        val minutes = ((millis / (1000 * 60)) % 60).toInt()
        val hours = ((millis / (1000 * 60 * 60)) % 24).toInt()

        view.text = (if (hours > 0) String.format(
            Locale.getDefault(),
            "$hours:",
        ) else "") + (if ((minutes < 10 && hours > 0)) "0" + String.format(
            "%d:",
            minutes
        ) else String.format(Locale.getDefault(),
            "%d:",
            minutes
        )) + (if (seconds < 10) "0" + seconds else seconds)
    }
}