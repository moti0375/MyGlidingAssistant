package com.dunihuliapps.myglidingassistnat.domain.di
import javax.inject.Qualifier

/**
 * Qualifier for Core Database Api
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QMainThread

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QTimerThread

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QExternalDirectory


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QDefaultFilesDir

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QTripsKmlDir

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QTripsImagesDir

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QShareImagesDir