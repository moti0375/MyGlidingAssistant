package com.bartovapps.gpstriprec.domain.di
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    companion object{

        @Provides
        @Singleton
        fun providesSharedPreferences(@ApplicationContext context: Context) : SharedPreferences {
            return context.getSharedPreferences("GPS_TRIP_RECORDER", MODE_PRIVATE)
        }
    }
}