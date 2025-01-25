package com.bartovapps.gpstriprec.presentation.displayers

import javax.inject.Inject

class HmsFormatter @Inject constructor() : TimeFormatter {
    override fun displayTime(millis: Long): String {
        val seconds = (millis / 1000).toInt() % 60
        val minutes = ((millis / (1000 * 60)) % 60).toInt()
        val hours = ((millis / (1000 * 60 * 60)) % 24).toInt()
        return (if (hours > 0) "$hours" else "") + (if ((minutes < 10 && hours > 0)) "0$minutes" else "$minutes") + (if (seconds < 10) "0$seconds" else seconds)
    }
}