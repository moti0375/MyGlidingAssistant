package com.bartovapps.gpstriprec.presentation.displayers

import android.widget.TextView

interface DataDisplayer {
    fun displayData(view: TextView, data: Double)
}