package com.Electroinova.inovinfo.data.repository

import android.util.Log
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.local.dao.CatalogoDao
import com.Electroinova.inovinfo.data.local.entity.EstadoEntity
import com.Electroinova.inovinfo.data.local.entity.SedeEntity
import com.Electroinova.inovinfo.data.local.entity.SuministroEntity
import com.Electroinova.inovinfo.data.local.entity.UnidadEntity
import com.Electroinova.inovinfo.data.local.entity.toEntity
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CatalogoRepository"

@Singleton
class CatalogoRepository @Inject constructor(
    private val catalogoDao:    CatalogoDao,
    private val api:            InovInfoApiService,
    private val sessionManager: SessionManager
) {

    /**
     * Descarga todos los catálogos del servidor y los guarda en Room.
     * Si hay un error de red, el fallo se registra pero no se propaga —
     * los datos en caché siguen siendo accesibles.
     */
    suspend fun refreshCatalogos() {
        try {
            val token       = sessionManager.bearerToken()
            val sedes       = api.getSedes(token)
            val unidades    = api.getUnidades(token)
            val estados     = api.getEstados(token)
            val suministros = api.getSuministros(token)

            catalogoDao.insertarSedes(sedes.map { it.toEntity() })
            catalogoDao.insertarUnidades(unidades.map { it.toEntity() })
            catalogoDao.insertarEstados(estados.map { it.toEntity() })
            catalogoDao.insertarSuministros(suministros.map { it.toEntity() })

            Log.d(TAG, "Catálogos actualizados: ${sedes.size} sedes, ${unidades.size} unidades, " +
                    "${estados.size} estados, ${suministros.size} suministros")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo refrescar catálogos (se usará caché): ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    // ── Acceso a datos (siempre desde Room) ───────────────────────────────────

    /** Emite la lista de sedes almacenada en Room. */
    fun getSedes(): Flow<List<SedeEntity>> = catalogoDao.getSedes()

    /** Emite la lista de unidades almacenada en Room. */
    fun getUnidades(): Flow<List<UnidadEntity>> = catalogoDao.getUnidades()

    /** Emite la lista de estados almacenada en Room. */
    fun getEstados(): Flow<List<EstadoEntity>> = catalogoDao.getEstados()

    /** Emite la lista de suministros activos almacenada en Room. */
    fun getSuministros(): Flow<List<SuministroEntity>> = catalogoDao.getSuministros()

    /**
     * Busca unidades cuyo número o nombre de sede contenga [query].
     * La búsqueda es insensible a mayúsculas (LOWER en SQL).
     */
    fun buscarUnidades(query: String): Flow<List<UnidadEntity>> =
        catalogoDao.buscarUnidades(query)
}
