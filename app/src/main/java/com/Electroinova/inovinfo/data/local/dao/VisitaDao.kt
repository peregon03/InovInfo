package com.Electroinova.inovinfo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.Electroinova.inovinfo.data.local.entity.PendienteCache
import com.Electroinova.inovinfo.data.local.entity.VisitaEntity
import com.Electroinova.inovinfo.data.local.entity.VisitaFotoEntity
import com.Electroinova.inovinfo.data.local.entity.VisitaHoyCache
import com.Electroinova.inovinfo.data.local.entity.VisitaSuministroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitaDao {

    // ── Creación de visitas locales ───────────────────────────────────────────

    /** Inserta una visita en estado PENDING y retorna el localId generado. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarVisita(visita: VisitaEntity): Long

    /** Inserta los suministros asociados a una visita local. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSuministros(suministros: List<VisitaSuministroEntity>)

    /** Inserta las fotos asociadas a una visita local. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarFotos(fotos: List<VisitaFotoEntity>)

    // ── Sincronización de visitas ─────────────────────────────────────────────

    /** Retorna todas las visitas con sync_status = 'PENDING'. */
    @Query("SELECT * FROM visitas WHERE sync_status = 'PENDING' ORDER BY created_at_local ASC")
    suspend fun getVisitasPendientes(): List<VisitaEntity>

    /** Actualiza el serverId y el estado de sincronización de una visita local. */
    @Query("""
        UPDATE visitas
        SET server_id   = :serverId,
            sync_status = :syncStatus
        WHERE local_id = :localId
    """)
    suspend fun actualizarSyncStatus(localId: Long, serverId: Int?, syncStatus: String)

    /** Obtiene los suministros de una visita local (necesario para sincronizar). */
    @Query("SELECT * FROM visita_suministros WHERE visita_local_id = :visitaLocalId")
    suspend fun getSuministrosDeVisita(visitaLocalId: Long): List<VisitaSuministroEntity>

    // ── Sincronización de fotos ───────────────────────────────────────────────

    /**
     * Retorna las fotos que aún no se han subido (uploaded = 0) cuya visita ya fue
     * sincronizada (sync_status = 'SYNCED') y tiene un server_id asignado.
     */
    @Query("""
        SELECT f.*
        FROM visita_fotos f
        INNER JOIN visitas v ON f.visita_local_id = v.local_id
        WHERE f.uploaded = 0
          AND v.sync_status = 'SYNCED'
          AND v.server_id IS NOT NULL
        ORDER BY f.visita_local_id ASC, f.id ASC
    """)
    suspend fun getFotosPendientesDeSubir(): List<VisitaFotoEntity>

    /** Marca una foto como subida y guarda la URL remota. */
    @Query("""
        UPDATE visita_fotos
        SET uploaded   = 1,
            remote_url = :remoteUrl
        WHERE id = :fotoId
    """)
    suspend fun marcarFotoSubida(fotoId: Long, remoteUrl: String)

    // ── Caché de visitas del día ──────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarVisitasHoy(visitas: List<VisitaHoyCache>)

    @Query("SELECT * FROM visitas_hoy_cache ORDER BY sede_nombre ASC, unidad_numero ASC")
    fun getVisitasHoy(): Flow<List<VisitaHoyCache>>

    @Query("DELETE FROM visitas_hoy_cache")
    suspend fun eliminarVisitasHoyCache()

    // ── Caché de pendientes ───────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarPendientes(pendientes: List<PendienteCache>)

    @Query("SELECT * FROM pendientes_cache ORDER BY urgencia ASC, fecha_seguimiento ASC")
    fun getPendientes(): Flow<List<PendienteCache>>

    @Query("DELETE FROM pendientes_cache")
    suspend fun eliminarPendientesCache()

    // ── Historial por unidad (desde caché de visitas del día) ─────────────────

    /**
     * Retorna las visitas del día que pertenecen a una unidad específica.
     * Se usa como fallback offline para la pantalla de historial.
     */
    @Query("""
        SELECT * FROM visitas_hoy_cache
        WHERE unidad_id = :unidadId
        ORDER BY id DESC
    """)
    fun getVisitasHoyPorUnidad(unidadId: Int): Flow<List<VisitaHoyCache>>

    // ── Consultas auxiliares ──────────────────────────────────────────────────

    /**
     * Retorna el server_id de una visita dado su local_id.
     * Usado por [VisitaRepository] para subir fotos de visitas ya sincronizadas.
     */
    @Query("SELECT server_id FROM visitas WHERE local_id = :localId LIMIT 1")
    suspend fun getServerIdByLocalId(localId: Long): Int?

    // ── Limpieza de caché antiguo ─────────────────────────────────────────────

    /** Elimina todo el caché de visitas del día y pendientes (para refresh). */
    @Query("DELETE FROM visitas_hoy_cache")
    suspend fun limpiarCacheVisitasHoy()

    @Query("DELETE FROM pendientes_cache")
    suspend fun limpiarCachePendientes()
}
