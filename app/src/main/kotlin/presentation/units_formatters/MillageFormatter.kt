package com.bartovapps.gpstriprec.presentation.units_formatters

import android.text.SpannableString
import java.util.Locale

class MillageFormatter : BaseUnitFormatter() {

    override val unitsBuilder: StringBuilder = StringBuilder("Mi")

    override fun formatUnits(data: Double) : SpannableString{
        val distanceBuilder: StringBuilder = StringBuilder()
        val miles = data / 1609.34
        distanceBuilder.replace(0, distanceBuilder.length, String.format(Locale.getDefault(), "%.1f", miles))
        return createSpannable(distanceBuilder.toString())
    }
}