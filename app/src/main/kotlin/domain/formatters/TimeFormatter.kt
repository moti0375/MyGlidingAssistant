package com.dunihuliapps.myglidingassistnat.domain.formatters

interface TimeFormatter {
    fun formatTime(millis: Long) : String
}