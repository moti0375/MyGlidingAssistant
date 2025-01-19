package com.bartovapps.gpstriprec.core.di

import com.bartovapps.gpstriprec.kmlhleper.KmlParser
import com.bartovapps.gpstriprec.kmlhleper.KmlParserImpl
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
    abstract fun bindsKmlManager(manager: KmlParserImpl) : KmlParser

    companion object{
        @Provides
        @Singleton
        fun provideSaxBuilder() : SAXBuilder {
            return SAXBuilder()
        }
    }
}