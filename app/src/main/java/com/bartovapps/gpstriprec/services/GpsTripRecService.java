package com.bartovapps.gpstriprec.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bartovapps.gpstriprec.GpsRecMain;
import com.bartovapps.gpstriprec.R;
import com.bartovapps.gpstriprec.enums.AltUnits;
import com.bartovapps.gpstriprec.enums.Units;

/**
 * Created by BartovMoti on 03/11/15.
 */
public class GpsTripRecService extends Service {
    private static final int NOTIFICATION_ID = 100;
    NotificationManager notificationManager;
    NotificationCompat.Builder mBuilder;
    private SharedPreferences settings;
    private Units units = Units.Metric;
    private AltUnits altUnits = AltUnits.Feet;


    @Override
    public void onCreate() {
        super.onCreate();
        settings = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        settings.registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String notificationService = Context.NOTIFICATION_SERVICE;
        notificationManager = (NotificationManager) getSystemService(notificationService);
        Intent notificationIntent = new Intent(this,
                GpsRecMain.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        String channelId = createNotificationChannel("GpsTripRecorderNotification");

        PendingIntent notiPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,PendingIntent.FLAG_MUTABLE);
        mBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.app_name)))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(notiPendingIntent);


        startForeground(NOTIFICATION_ID, mBuilder.build());
        return START_REDELIVER_INTENT;
    }

    private String createNotificationChannel(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_LOW);

            notificationManager.createNotificationChannel(notificationChannel);
        }
        return channelId;
    }

    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder{
        public GpsTripRecService getService(){
            return GpsTripRecService.this;
        }
    }

    public void updateService(float distance){
        updateNotification(distance);
    }

    private void updateNotification(float distance) {
        if (units == Units.Mileage) {
            mBuilder.setContentText(getString(R.string.Distance)+ ": " + String.format("%.1f Km", distance / 1609.34));
        } else {
            mBuilder.setContentText(getString(R.string.Distance)+ ": " + (distance < 1000 ? String.format("%.1f M", distance) : String.format("%.1f Km", distance / 1000)));
        }
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    SharedPreferences.OnSharedPreferenceChangeListener prefListener = (sharedPreferences, key) -> updatePreferences();

    private void updatePreferences() {
        int units = Integer.parseInt(settings.getString(getResources()
                .getString(R.string.units), "1"));

        int altitudeUnits = Integer.parseInt(settings.getString(getResources()
                .getString(R.string.altitudeUnitsKey), "1"));

        // Log.i(LOG_TAG, "Selected units: " + units);

        switch (units) {
            case 1:
                this.units = Units.Metric;
                break;
            case 2:
                this.units = Units.Mileage;
                break;
        }

        switch (altitudeUnits) {
            case 1:
                this.altUnits = AltUnits.Feet;
                break;
            case 2:
                this.altUnits = AltUnits.Metric;
                break;
        }

    }
}
