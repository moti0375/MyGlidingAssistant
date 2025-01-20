package com.bartovapps.gpstriprec.presentation.displayers
import android.widget.TextView

interface TimeDisplayer {
    fun displayTime(view: TextView, millis: Long)
}