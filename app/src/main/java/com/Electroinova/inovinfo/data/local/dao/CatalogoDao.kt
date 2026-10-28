package com.Electroinova.inovinfo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.Electroinova.inovinfo.data.local.entity.EstadoEntity
import com.Electroinova.inovinfo.data.local.entity.SedeEntity
import com.Electroinova.inovinfo.data.local.entity.SuministroEntity
import com.Electroinova.inovinfo.data.local.entity.UnidadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogoDao {

    // ── Sedes ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSedes(sedes: List<SedeEntity>)

    @Query("SELECT * FROM sedes ORDER BY nombre ASC")
    fun getSedes(): Flow<List<SedeEntity>>

    @Query("DELETE FROM sedes")
    suspend fun eliminarSedes()

    // ── Unidades ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUnidades(unidades: List<UnidadEntity>)

    @Query("SELECT * FROM unidades ORDER BY sede_nombre ASC, numero ASC")
    fun getUnidades(): Flow<List<UnidadEntity>>

    @Query("""
        SELECT * FROM unidades
        WHERE LOWER(numero) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(sede_nombre) LIKE '%' || LOWER(:query) || '%'
        ORDER BY sede_nombre ASC, numero ASC
    """)
    fun buscarUnidades(query: String): Flow<List<UnidadEntity>>

    @Query("DELETE FROM unidades")
    suspend fun eliminarUnidades()

    // ── Estados ───────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEstados(estados: List<EstadoEntity>)

    @Query("SELECT * FROM estados ORDER BY nombre ASC")
    fun getEstados(): Flow<List<EstadoEntity>>

    @Query("DELETE FROM estados")
    suspend fun eliminarEstados()

    // ── Suministros ───────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSuministros(suministros: List<SuministroEntity>)

    @Query("SELECT * FROM suministros WHERE activo = 1 ORDER BY categoria ASC, nombre ASC")
    fun getSuministros(): Flow<List<SuministroEntity>>

    @Query("DELETE FROM suministros")
    suspend fun eliminarSuministros()
}
