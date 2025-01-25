package com.bartovapps.gpstriprec.presentation.displayers

import android.text.SpannableString


interface UnitsFormatter {
    fun formatUnits(data: Double) : SpannableString
}