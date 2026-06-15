package com.bartovapps.gpstriprec.presentation.units_formatters
import android.text.SpannableString
import java.util.Locale

class MphFormatter : BaseUnitFormatter() {
    override val unitsBuilder: StringBuilder = StringBuilder("Mi/H")

    override fun formatUnits(data: Double): SpannableString {
        val speedBuilder = StringBuilder()
        // 1mph = 1 m/sec * 2.23694
        val mph = data * 2.23694
        speedBuilder.replace(0, speedBuilder.length, String.format(Locale.getDefault(), "%d", mph.toInt()))
        return createSpannable(speedBuilder.toString())
    }
}