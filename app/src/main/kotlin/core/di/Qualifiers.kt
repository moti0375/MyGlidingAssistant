package com.bartovapps.gpstriprec.core.di

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