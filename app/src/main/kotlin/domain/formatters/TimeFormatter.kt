package com.bartovapps.gpstriprec.domain.formatters

interface TimeFormatter {
    fun formatTime(millis: Long) : String
}