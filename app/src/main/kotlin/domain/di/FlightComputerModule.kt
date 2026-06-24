package domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import domain.flight_computer.FlightComputer
import domain.flight_computer.FlightComputerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FlightComputerModule {

    @Binds
    @Singleton
    abstract fun bindFlightComputer(impl: FlightComputerImpl): FlightComputer
}
