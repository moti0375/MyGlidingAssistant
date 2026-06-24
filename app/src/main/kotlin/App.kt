package com.dunihuliapps.myglidingassistnat
import android.app.Application
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Maps SDK 18+ requires explicit initialization before any SupportMapFragment
        // is created. Without this, onCreateView fails with a Play Services error
        // when the fragment is added synchronously early in the activity lifecycle.
        MapsInitializer.initialize(this, Renderer.LATEST, null)
    }
}