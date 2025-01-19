//package com.bartovapps.gpstriprec.timer;
//
//import android.content.Context;
//import android.os.Handler;
//import android.widget.TextView;
//
//import com.bartovapps.gpstriprec.displayers.HmsDisplayer;
//import com.bartovapps.gpstriprec.displayers.TimeDisplayer;
//
//public class com.bartovapps.gpstriprec.core.timer.TimerManager {
//
//	Context context;
//	TextView tv;
//
//	private long startTime = 0;
//	private long millis = 0;
//	private int minutes = 0;
//	private int seconds = 0;
//
//	private long pausedMsec = 0;
//	TimeDisplayer timeDisplayer;
//
//	Handler timerHandler = new Handler();
//	Runnable timerRunnable = new Runnable() {
//
//		@Override
//		public void run() {
//			millis = System.currentTimeMillis() - startTime;
//			timeDisplayer.displayTime(tv, millis);
//			timerHandler.postDelayed(this, 1000);
//		}
//	};
//
//	public com.bartovapps.gpstriprec.core.timer.TimerManager(Context context, TextView tv){
//		this.context = context;
//		this.tv = tv;
//		timeDisplayer = new HmsDisplayer();
//	}
//
//    public com.bartovapps.gpstriprec.core.timer.TimerManager(Context context){
//        this.context = context;
//    }
//
//	public void startTimer() {
//		startTime = System.currentTimeMillis();
//		timerHandler.postDelayed(timerRunnable, 0);
//	}
//
//	public void stopTimer(){
//		timerHandler.removeCallbacks(timerRunnable);
//	}
//
//	public void pauseTimer(){
//		timerHandler.removeCallbacks(timerRunnable);
//		pausedMsec = millis;
//	//	startTime = System.currentTimeMillis();
//	}
//
//	public void resetTimer(){
//		startTime = System.currentTimeMillis();
//	}
//
//	public void resumeTimer(){
//		timerHandler.postDelayed(timerRunnable, 0);
//		startTime = System.currentTimeMillis() - pausedMsec;
//	}
//
//	public double getTimeSec(){
//		return (double)(minutes * 60 + seconds);
//	}
//
//	public long getTimeMillis(){
//		return millis;
//	}
//
//	public void setStartTime(long startTime){
//		this.millis = startTime;
//		this.pausedMsec = startTime;
//		resumeTimer();
//		pauseTimer();
//	}
//
//
//
//}
