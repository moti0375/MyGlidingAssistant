package com.dunihuliapps.myglidingassistnat.domain.di
import android.content.Context
import android.content.res.Resources
import android.location.Geocoder
import com.dunihuliapps.myglidingassistnat.domain.timer.TimerManager
import com.dunihuliapps.myglidingassistnat.domain.timer.TripTimer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindTripManager(tripManager: TimerManager) : TripTimer

    companion object{

        @Provides
        @Singleton
        fun provideGeoCoder(@ApplicationContext context: Context) : Geocoder{
            return Geocoder(context)
        }

        @Provides
        @Singleton
        fun provideResourceCompat(@ApplicationContext context: Context) : Resources {
            return context.resources
        }

        @Provides
        @Singleton
        @QExternalDirectory
        fun provideDefaultDirectory(@ApplicationContext context: Context) : File? {
            return context.getExternalFilesDir(null)
        }
    }
}