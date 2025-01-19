package com.bartovapps.gpstriprec.core.di

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThreadModule {

    companion object{

        @Provides
        @QMainThread
        @Singleton
        fun provideMainHandler() : Handler {
            return Handler(Looper.getMainLooper())
        }

        @Provides
        @QTimerThread
        @Singleton
        fun provideTimerHandler() : Handler {
            val timerHandlerThread = HandlerThread("timer-thread")
            timerHandlerThread.start()
            val looper: Looper = timerHandlerThread.getLooper()
            return  Handler(looper)
        }

    }
}