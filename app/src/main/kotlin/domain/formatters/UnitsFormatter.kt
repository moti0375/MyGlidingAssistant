package com.bartovapps.gpstriprec.domain.formatters

import android.text.SpannableString


interface UnitsFormatter {
    fun formatUnits(data: Double) : SpannableString
}