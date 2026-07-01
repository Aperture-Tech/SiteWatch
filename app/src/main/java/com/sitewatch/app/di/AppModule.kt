package com.sitewatch.app.di

import android.content.Context
import androidx.room.Room
import com.sitewatch.app.data.local.NotificationDao
import com.sitewatch.app.data.local.SiteWatchDatabase
import com.sitewatch.app.data.local.WatchedSiteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SiteWatchDatabase =
        Room.databaseBuilder(context, SiteWatchDatabase::class.java, SiteWatchDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWatchedSiteDao(db: SiteWatchDatabase): WatchedSiteDao = db.watchedSiteDao()

    @Provides
    fun provideNotificationDao(db: SiteWatchDatabase): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
}
