package com.bartovapps.gpstriprec.presentation.units_formatters
import android.text.SpannableString
import java.util.Locale


class KmhFormatter : BaseUnitFormatter() {
    private val speedBuilder: StringBuilder = StringBuilder()

    override val unitsBuilder: StringBuilder = StringBuilder("KM/H")

    override fun formatUnits(data: Double): SpannableString {
        // km\h = m\sec * 3.6
        speedBuilder.replace(0, speedBuilder.length, String.format(Locale.getDefault(), "%d", (data * 3.6).toInt()))
        return createSpannable(speedBuilder.toString())
    }
}