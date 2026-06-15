package com.dunihuliapps.myglidingassistnat.domain.di
import com.dunihuliapps.myglidingassistnat.domain.formatters.TimeFormatter
import com.dunihuliapps.myglidingassistnat.presentation.units_formatters.HmsFormatter
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