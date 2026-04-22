package com.dailyverse.app.di

import android.content.Context
import com.dailyverse.app.data.local.BibleDatabase
import com.dailyverse.app.data.local.SettingsDataStore
import com.dailyverse.app.data.remote.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBibleDatabase(@ApplicationContext context: Context): BibleDatabase {
        return BibleDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideNetworkModule(): NetworkModule {
        return NetworkModule()
    }
}
