package com.bartovapps.gpstriprec.presentation.units_formatters
import com.bartovapps.gpstriprec.domain.formatters.TimeFormatter
import java.util.Locale
import javax.inject.Inject

class HmsFormatter @Inject constructor() : TimeFormatter {
    override fun formatTime(millis: Long): String {
        val seconds = (millis / 1000).toInt() % 60
        val minutes = ((millis / (1000 * 60)) % 60).toInt()
        val hours = ((millis / (1000 * 60 * 60)) % 24).toInt()
        return (if (hours > 0) String.format(
            Locale.getDefault(),
            "%d:",
            hours
        ) else "") + (if ((minutes < 10 && hours > 0)) "0" + String.format(Locale.getDefault(),
            "%d:",
            minutes
        ) else String.format(
            Locale.getDefault(),
            "%d:",
            minutes
        )) + (if (seconds < 10) "0$seconds"  else seconds)

    }
}