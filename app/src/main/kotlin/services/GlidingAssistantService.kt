package services
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.data.enums.AltitudeUnits
import com.dunihuliapps.myglidingassistnat.data.enums.DistanceUnits
import presentation.screens.MainActivity
import java.util.Locale

/**
 * Created by BartovMoti on 15/07/26.
 */
class GlidingAssistanceService : Service(), OnSharedPreferenceChangeListener {
    private lateinit var notificationManager: NotificationManager
    private lateinit var mBuilder: NotificationCompat.Builder
    private lateinit var settings: SharedPreferences
    private var distanceUnits = DistanceUnits.Metric
    private var altUnits = AltitudeUnits.Feet


    override fun onCreate() {
        super.onCreate()
        settings = PreferenceManager.getDefaultSharedPreferences(this).apply {
            registerOnSharedPreferenceChangeListener(this@GlidingAssistanceService)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationService = NOTIFICATION_SERVICE
        notificationManager = getSystemService(notificationService) as NotificationManager
        val notificationIntent = Intent(
            this,
            MainActivity::class.java
        )
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val channelId = createNotificationChannel("GpsTripRecorderNotification")

        val notiPendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)
        mBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.app_name)))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(notiPendingIntent)


        startForeground(NOTIFICATION_ID, mBuilder!!.build())
        return START_REDELIVER_INTENT
    }

    private fun createNotificationChannel(channelId: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_LOW)

            notificationManager.createNotificationChannel(notificationChannel)
        }
        return channelId
    }

    private val mBinder: IBinder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        val service: GlidingAssistanceService
            get() = this@GlidingAssistanceService
    }

    fun updateService(distance: Float) {
        updateNotification(distance)
    }

    private fun updateNotification(distance: Float) {
        if (distanceUnits == DistanceUnits.Millage) {
            mBuilder.setContentText(
                getString(R.string.Distance) + ": " + String.format(Locale.getDefault(),
                    "%.1f Km",
                    distance / 1609.34
                )
            )
        } else {
            mBuilder.setContentText(
                getString(R.string.Distance) + ": " + (if (distance < 1000) String.format(Locale.getDefault(),
                    "%.1f M",
                    distance
                ) else String.format(Locale.getDefault(), "%.1f Km", distance / 1000))
            )
        }
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build())
    }

    private fun updatePreferences() {
        val units = settings.getString(
            resources
                .getString(R.string.distance_units_key), "1"
        )?.toInt()
        val altitudeUnits = settings.getString(
            resources
                .getString(R.string.altitude_units_key), "1"
        )?.toInt()

        if (units == 2) {
            this.distanceUnits = DistanceUnits.Millage
        } else {
            this.distanceUnits = DistanceUnits.Metric
        }

        if (altitudeUnits == 2) {
            this.altUnits = AltitudeUnits.Metric
        } else {
            this.altUnits = AltitudeUnits.Feet
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePreferences()
    }

    companion object {
        private const val NOTIFICATION_ID = 100
    }
}