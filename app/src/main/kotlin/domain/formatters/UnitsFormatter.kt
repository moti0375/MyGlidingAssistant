package com.dunihuliapps.myglidingassistnat.domain.formatters

import android.text.SpannableString


interface UnitsFormatter {
    fun formatUnits(data: Double) : SpannableString
}