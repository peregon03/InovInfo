package com.Electroinova.inovinfo.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Módulo Hilt principal.
 * Aquí se proveerán: AppDatabase, DAOs, Repositorios, ApiServices y clientes Retrofit.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
