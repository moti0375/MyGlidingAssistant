package com.bartovapps.gpstriprec.core.di

import com.bartovapps.gpstriprec.presentation.displayers.HmsDisplayer
import com.bartovapps.gpstriprec.presentation.displayers.TimeDisplayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DisplayersModule {
    @Binds
    abstract fun bindsTimeDisplayer(timeDisplayer: HmsDisplayer) : TimeDisplayer
}