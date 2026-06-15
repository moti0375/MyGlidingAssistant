package com.bartovapps.gpstriprec.presentation.units_formatters.presentation.units_formatters
import android.text.SpannableString
import com.bartovapps.gpstriprec.presentation.units_formatters.BaseUnitFormatter
import java.util.Locale

class KnotsFormatter : BaseUnitFormatter() {
    override val unitsBuilder: StringBuilder = StringBuilder("Knots")

    override fun formatUnits(data: Double): SpannableString {
        val speedBuilder = StringBuilder()
        // 1knot = 1 m/sec * 1.94384
        val knots = data * 1.94384
        speedBuilder.replace(0, speedBuilder.length, String.format(Locale.getDefault(), "%d", knots.toInt()))
        return createSpannable(speedBuilder.toString())
    }
}