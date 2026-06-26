package com.Electroinova.inovinfo.data.local

import androidx.room.RoomDatabase

/**
 * Base de datos Room local.
 *
 * Implementación pendiente — se añadirá cuando se defina la entidad [VisitaEntity].
 *
 * @Database(
 *     entities = [VisitaEntity::class],
 *     version  = 1,
 *     exportSchema = false
 * )
 * abstract class AppDatabase : RoomDatabase() {
 *     abstract fun visitaDao(): VisitaDao
 * }
 */
abstract class AppDatabase : RoomDatabase()
