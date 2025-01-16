package com.bartovapps.gpstriprec.displayers;

import android.widget.TextView;

public class HmsDisplayer implements TimeDisplayer {

	int seconds;
	int minutes;
	int hours;
	@Override
	public void displayTime(TextView view, long millis) {

		seconds = (int) (millis / 1000) % 60 ; 
		minutes = (int) ((millis / (1000*60)) % 60);
		hours   = (int) ((millis / (1000*60*60)) % 24);
		
		view.setText((hours>0 ? String.format("%d:", hours) : "") + ((this.minutes<10 && this.hours > 0)? "0" + String.format("%d:", minutes) :  String.format("%d:", minutes)) + (this.seconds<10 ? "0" + this.seconds: this.seconds));
	}

}
