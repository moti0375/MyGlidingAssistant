package com.bartovapps.gpstriprec.domain.di
import com.bartovapps.gpstriprec.domain.formatters.TimeFormatter
import com.bartovapps.gpstriprec.presentation.units_formatters.HmsFormatter
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