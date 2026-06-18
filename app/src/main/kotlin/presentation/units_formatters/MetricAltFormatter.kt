package com.dunihuliapps.myglidingassistnat.presentation.units_formatters

import android.text.SpannableString
import java.text.DecimalFormat

class MetricAltFormatter : BaseUnitFormatter() {
    private val distanceBuilder: StringBuilder = StringBuilder()
    private val numFormat: DecimalFormat = DecimalFormat("#,###.##")

    override val unitsBuilder: StringBuilder = StringBuilder("M")

    override fun formatUnits(data: Double): SpannableString {
        distanceBuilder.replace(0, distanceBuilder.length, numFormat.format(data))
        return createSpannable(distanceBuilder.toString())
    }
}