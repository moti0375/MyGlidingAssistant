package com.bartovapps.gpstriprec.domain.di
import com.bartovapps.gpstriprec.core.files.kml.KmlManager
import com.bartovapps.gpstriprec.core.files.kml.KmlManagerImpl
import com.bartovapps.gpstriprec.domain.trip_manager.KmlParser
import com.bartovapps.gpstriprec.domain.trip_manager.KmlParserImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.jdom2.input.SAXBuilder
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class KmlModule {

    @Binds
    abstract fun bindsKmlParser(manager: KmlParserImpl) : KmlParser

    @Binds
    abstract fun bindsKmlManager(kmlManager: KmlManagerImpl) : KmlManager

    companion object{
        @Provides
        @Singleton
        fun provideSaxBuilder() : SAXBuilder {
            return SAXBuilder()
        }
    }
}