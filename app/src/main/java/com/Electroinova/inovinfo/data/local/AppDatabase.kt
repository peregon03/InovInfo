package com.Electroinova.inovinfo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.Electroinova.inovinfo.data.local.dao.CatalogoDao
import com.Electroinova.inovinfo.data.local.dao.VisitaDao
import com.Electroinova.inovinfo.data.local.entity.EstadoEntity
import com.Electroinova.inovinfo.data.local.entity.PendienteCache
import com.Electroinova.inovinfo.data.local.entity.SedeEntity
import com.Electroinova.inovinfo.data.local.entity.SuministroEntity
import com.Electroinova.inovinfo.data.local.entity.UnidadEntity
import com.Electroinova.inovinfo.data.local.entity.VisitaEntity
import com.Electroinova.inovinfo.data.local.entity.VisitaFotoEntity
import com.Electroinova.inovinfo.data.local.entity.VisitaHoyCache
import com.Electroinova.inovinfo.data.local.entity.VisitaSuministroEntity

@Database(
    entities = [
        // Catálogos
        SedeEntity::class,
        UnidadEntity::class,
        EstadoEntity::class,
        SuministroEntity::class,
        // Visitas locales
        VisitaEntity::class,
        VisitaSuministroEntity::class,
        VisitaFotoEntity::class,
        // Cachés del servidor
        VisitaHoyCache::class,
        PendienteCache::class
    ],
    version      = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
    abstract fun visitaDao():   VisitaDao
}
