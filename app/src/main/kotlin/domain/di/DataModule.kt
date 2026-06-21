package com.dunihuliapps.myglidingassistnat.domain.di
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.dunihuliapps.myglidingassistnat.data.repositories.flights.FlightsRepository
import com.dunihuliapps.myglidingassistnat.data.repositories.flights.FlightsRepositoryImpl
import com.dunihuliapps.myglidingassistnat.data.repositories.gliders.GlidersRepository
import com.dunihuliapps.myglidingassistnat.data.repositories.gliders.GlidersRepositoryImpl
import com.dunihuliapps.myglidingassistnat.domain.datasources.flights.FlightsLocalDatasource
import com.dunihuliapps.myglidingassistnat.domain.datasources.flights.FlightsLocalDatasourceImpl
import com.dunihuliapps.myglidingassistnat.domain.datasources.gliders.GlidersLocalDatasource
import com.dunihuliapps.myglidingassistnat.domain.datasources.gliders.GlidersLocalDatasourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {


    @Binds
    @Singleton
    abstract fun bindFlightsLocalDatasource(flightsLocalDatasourceImpl: FlightsLocalDatasourceImpl): FlightsLocalDatasource

    @Binds
    @Singleton
    abstract fun bindFlightsRepository(flightsRepositoryImpl: FlightsRepositoryImpl): FlightsRepository


    @Binds
    @Singleton
    abstract fun bindGlidersLocalDatasource(glidersLocalDatasourceImpl: GlidersLocalDatasourceImpl): GlidersLocalDatasource

    @Binds
    @Singleton
    abstract fun bindGlidersRepository(glidersRepositoryImpl: GlidersRepositoryImpl): GlidersRepository


    companion object{
        @Provides
        @Singleton
        fun providesSharedPreferences(@ApplicationContext context: Context) : SharedPreferences {
            return context.getSharedPreferences("GPS_TRIP_RECORDER", MODE_PRIVATE)
        }
    }
}