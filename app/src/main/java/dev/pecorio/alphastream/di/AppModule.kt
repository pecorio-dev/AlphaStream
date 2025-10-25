package dev.pecorio.alphastream.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pecorio.alphastream.data.database.AlphaStreamDatabase
import dev.pecorio.alphastream.data.database.FavoriteDao
import dev.pecorio.alphastream.data.dao.WatchProgressDao
import dev.pecorio.alphastream.utils.PreferencesManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAlphaStreamDatabase(@ApplicationContext context: Context): AlphaStreamDatabase {
        return AlphaStreamDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideFavoriteDao(database: AlphaStreamDatabase): FavoriteDao {
        return database.favoriteDao()
    }
    
    @Provides
    fun provideWatchProgressDao(database: AlphaStreamDatabase): WatchProgressDao {
        return database.watchProgressDao()
    }
}