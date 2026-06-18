package com.dunihuliapps.myglidingassistnat.presentation.units_formatters

import android.annotation.SuppressLint
import android.text.SpannableString

@SuppressLint("DefaultLocale")
class MetricFormatter : BaseUnitFormatter() {
    private val distanceBuilder: StringBuilder = StringBuilder()

    override val unitsBuilder: StringBuilder = StringBuilder("M")

    override fun formatUnits(data: Double): SpannableString {
        var distance = data
        if (distance < 1000) {
            unitsBuilder.replace(0, unitsBuilder.length, "M")
        } else {
            distance /= 1000.0
            unitsBuilder.replace(0, unitsBuilder.length, "KM")
        }
        distanceBuilder.replace(0, distanceBuilder.length, String.format("%.1f", distance))
        return createSpannable(distanceBuilder.toString())
    }

}