package com.bartovapps.gpstriprec.core.di

import android.content.Context
import android.content.res.Resources
import android.location.Geocoder
import com.bartovapps.gpstriprec.core.timer.TimerManager
import com.bartovapps.gpstriprec.core.timer.TripTimer
import domain.trip_manager.TripManager
import domain.trip_manager.TripManagerImpl
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
    abstract fun bindTripManager(tripManager: TripManagerImpl) : TripManager

    @Binds
    @Singleton
    abstract fun bindTimerManager(timerManager: TimerManager) : TripTimer

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