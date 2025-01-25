package com.bartovapps.gpstriprec.core.di

import com.bartovapps.gpstriprec.presentation.displayers.HmsFormatter
import com.bartovapps.gpstriprec.presentation.displayers.TimeFormatter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DisplayersModule {
    @Binds
    abstract fun bindsTimeDisplayer(timeDisplayer: HmsFormatter) : TimeFormatter
}