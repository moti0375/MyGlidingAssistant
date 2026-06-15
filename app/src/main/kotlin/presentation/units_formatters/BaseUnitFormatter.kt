package com.bartovapps.gpstriprec.presentation.units_formatters

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.bartovapps.gpstriprec.domain.formatters.UnitsFormatter

abstract class BaseUnitFormatter : UnitsFormatter {
    abstract val unitsBuilder : StringBuilder
    protected fun createSpannable(baseText: String) : SpannableString {
        return SpannableString("$baseText$unitsBuilder").apply {
            setSpan(RelativeSizeSpan(0.5f), baseText.length, length, 0) // set size
            setSpan(ForegroundColorSpan(Color.GREEN), baseText.length, length, 0) // set color
        }
    }
}