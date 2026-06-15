package com.dunihuliapps.myglidingassistnat.domain.timer
import android.os.Handler
import com.dunihuliapps.myglidingassistnat.domain.di.QTimerThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface TripTimer {
    fun startTimer(reset: Boolean = true)
    fun stopTimer()
    fun pauseTimer()
    fun resetTimer()
    fun resumeTimer()
    fun setStartTime(timer: Long)
    val timerStateFlow: StateFlow<Long>
    fun getDuration(): Long
}

class TimerManager @Inject constructor(
    @QTimerThread private val timerHandler: Handler,
) : TripTimer {
    private var startTime: Long = 0

    private val minutes = 0
    private val seconds = 0

    private var pausedMsec: Long = 0

    private val timerMutableStateFlow = MutableStateFlow<Long>(0)
    override val timerStateFlow = timerMutableStateFlow.asStateFlow()


    private val timerRunnable = Runnable {
        timerMutableStateFlow.value = System.currentTimeMillis() - startTime
        postTimerEvent(1000)
    }

    override fun startTimer(reset: Boolean) {
        if(reset){
            resetTimer()
        }

        startTime = System.currentTimeMillis()
        timerHandler.postDelayed(timerRunnable, 0)
    }

    override fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun pauseTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        pausedMsec = timerMutableStateFlow.value
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
        this.timerMutableStateFlow.value = startTime
        this.pausedMsec = startTime
        resumeTimer()
        pauseTimer()
    }

    override fun getDuration(): Long {
        return this.timerMutableStateFlow.value
    }
}