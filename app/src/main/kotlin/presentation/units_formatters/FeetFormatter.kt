package com.dunihuliapps.myglidingassistnat.presentation.units_formatters

import android.text.SpannableString
import java.text.DecimalFormat

class FeetFormatter : BaseUnitFormatter() {
    private val distanceBuilder: StringBuilder = StringBuilder()
    private val numFormat: DecimalFormat = DecimalFormat("#,###.##")

    override val unitsBuilder: StringBuilder = StringBuilder("Ft")
    override fun formatUnits(data: Double): SpannableString{
        distanceBuilder.replace(0, distanceBuilder.length, numFormat.format(data * 3.28084))
        return createSpannable(distanceBuilder.toString())
    }
}