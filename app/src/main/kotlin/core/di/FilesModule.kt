package com.bartovapps.gpstriprec.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
abstract class FilesModule {

    companion object{
        @Provides
        @QDefaultFilesDir
        fun providesDefaultFilesDir(@ApplicationContext context: Context) : String {
            return "${context.filesDir.absolutePath}/trip_recorder".also { mkdir(it) }
        }

        @Provides
        @QTripsKmlDir
        fun providesTripKmlDir(@QDefaultFilesDir defaultFilesDir : String) : String {
            return "$defaultFilesDir/trips".also { mkdir(it) }
        }

        @Provides
        @QTripsImagesDir
        fun providesTripImagesDir(@QDefaultFilesDir defaultFilesDir : String) : String {
            return "$defaultFilesDir/map_images".also { mkdir(it) }
        }


        private fun mkdir(path: String){
            File(path).apply {
                if(!exists()){
                    mkdirs()
                }
            }
        }
    }
}