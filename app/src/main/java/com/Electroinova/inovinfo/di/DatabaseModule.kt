package com.Electroinova.inovinfo.di

import android.content.Context
import androidx.room.Room
import com.Electroinova.inovinfo.data.local.AppDatabase
import com.Electroinova.inovinfo.data.local.dao.CatalogoDao
import com.Electroinova.inovinfo.data.local.dao.VisitaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "inovinfo.db"
        ).build()

    @Provides
    @Singleton
    fun provideCatalogoDao(database: AppDatabase): CatalogoDao =
        database.catalogoDao()

    @Provides
    @Singleton
    fun provideVisitaDao(database: AppDatabase): VisitaDao =
        database.visitaDao()
}
