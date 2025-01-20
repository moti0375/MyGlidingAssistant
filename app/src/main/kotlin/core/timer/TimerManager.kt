package com.bartovapps.gpstriprec.core.timer

import android.os.Handler
import com.bartovapps.gpstriprec.core.di.QTimerThread
import com.bartovapps.gpstriprec.presentation.displayers.TimeDisplayer
import javax.inject.Inject

interface TripTimer {
    fun startTimer()
    fun stopTimer()
    fun pauseTimer()
    fun resetTimer()
    fun resumeTimer()
    fun setStartTime(timer: Long)
    fun subscribeTimerChanges()
    val timeMillis : Long
}

class TimerManager @Inject constructor(
    @QTimerThread private val timerHandler: Handler,
    private val timeDisplayer: TimeDisplayer,
) : TripTimer {
    private var startTime: Long = 0

    private val minutes = 0
    private val seconds = 0

    private var pausedMsec: Long = 0

    override var timeMillis: Long = 0

    private val timerRunnable = Runnable {
        timeMillis = System.currentTimeMillis() - startTime
        //todo post timer value
        postTimerEvent(1000)
    }

    override fun startTimer() {
        startTime = System.currentTimeMillis()
        timerHandler.postDelayed(timerRunnable, 0)
    }

    override fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun pauseTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        pausedMsec = timeMillis
    }

    override fun resetTimer() {
        startTime = System.currentTimeMillis()
    }

    override fun resumeTimer() {
        postTimerEvent(0)
        startTime = System.currentTimeMillis() - pausedMsec
    }


    private fun postTimerEvent(millis: Long) {
        timerHandler.postDelayed(timerRunnable, millis)
    }

    val timeSec: Double
        get() = (minutes * 60 + seconds).toDouble()

    override fun setStartTime(startTime: Long) {
        this.timeMillis = startTime
        this.pausedMsec = startTime
        resumeTimer()
        pauseTimer()
    }

    override fun subscribeTimerChanges() {
        TODO("Not yet implemented")
    }
}