package com.Electroinova.inovinfo.data.repository

import android.app.Application
import android.net.Uri
import android.util.Log
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.local.dao.VisitaDao
import com.Electroinova.inovinfo.data.local.entity.VisitaEntity
import com.Electroinova.inovinfo.data.local.entity.VisitaFotoEntity
import com.Electroinova.inovinfo.data.local.entity.VisitaSuministroEntity
import com.Electroinova.inovinfo.data.local.entity.toCache
import com.Electroinova.inovinfo.data.local.entity.toDto
import com.Electroinova.inovinfo.data.remote.CrearVisitaRequest
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import com.Electroinova.inovinfo.data.remote.PendienteDto
import com.Electroinova.inovinfo.data.remote.SuministroRequest
import com.Electroinova.inovinfo.data.remote.VisitaCompletaDto
import com.Electroinova.inovinfo.data.remote.VisitaHoyDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VisitaRepository"

@Singleton
class VisitaRepository @Inject constructor(
    private val visitaDao:      VisitaDao,
    private val api:            InovInfoApiService,
    private val sessionManager: SessionManager,
    private val app:            Application
) {

    // ── Creación de visitas locales ───────────────────────────────────────────

    /**
     * Guarda una visita en Room con estado PENDING.
     * Retorna el [localId] asignado por Room.
     */
    suspend fun guardarVisitaLocal(
        unidadId:                Int,
        estadoId:                Int,
        novedadTexto:            String?,
        observacionEstructurada: String?,
        requiereSeguimiento:     Boolean,
        fechaSeguimiento:        String?,
        notaSeguimiento:         String?,
        suministros:             Map<Int, Int>,   // suministroId → cantidad
        fotosUri:                List<Uri>
    ): Long {
        val visita = VisitaEntity(
            unidadId                = unidadId,
            estadoId                = estadoId,
            novedadTexto            = novedadTexto?.ifBlank { null },
            observacionEstructurada = observacionEstructurada,
            requiereSeguimiento     = requiereSeguimiento,
            fechaSeguimiento        = fechaSeguimiento?.ifBlank { null },
            notaSeguimiento         = notaSeguimiento?.ifBlank { null },
            syncStatus              = "PENDING"
        )
        val localId = visitaDao.insertarVisita(visita)

        if (suministros.isNotEmpty()) {
            val entidades = suministros.map { (suministroId, cantidad) ->
                VisitaSuministroEntity(
                    visitaLocalId = localId,
                    suministroId  = suministroId,
                    cantidad      = cantidad
                )
            }
            visitaDao.insertarSuministros(entidades)
        }

        if (fotosUri.isNotEmpty()) {
            val entidades = fotosUri.map { uri ->
                VisitaFotoEntity(
                    visitaLocalId = localId,
                    localUri      = uri.toString(),
                    uploaded      = false
                )
            }
            visitaDao.insertarFotos(entidades)
        }

        Log.d(TAG, "Visita guardada localmente: localId=$localId, suministros=${suministros.size}, fotos=${fotosUri.size}")
        return localId
    }

    // ── Lecturas desde Room (Flows para la UI) ────────────────────────────────

    /** Emite la lista de visitas del día desde el caché de Room. */
    fun getVisitasHoy(): Flow<List<VisitaHoyDto>> =
        visitaDao.getVisitasHoy().map { lista -> lista.map { it.toDto() } }

    /** Emite la lista de pendientes desde el caché de Room. */
    fun getPendientes(): Flow<List<PendienteDto>> =
        visitaDao.getPendientes().map { lista -> lista.map { it.toDto() } }

    // ── Refresh desde el servidor → Room ─────────────────────────────────────

    /** Descarga las visitas del día del servidor y actualiza el caché en Room. */
    suspend fun refreshVisitasHoy() {
        try {
            val token   = sessionManager.bearerToken()
            val visitas = api.getVisitasHoy(token)
            visitaDao.limpiarCacheVisitasHoy()
            visitaDao.insertarVisitasHoy(visitas.map { it.toCache() })
            Log.d(TAG, "VisitasHoy actualizadas: ${visitas.size} registros")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo refrescar visitas de hoy (se usará caché): ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    /** Descarga los pendientes del servidor y actualiza el caché en Room. */
    suspend fun refreshPendientes() {
        try {
            val token      = sessionManager.bearerToken()
            val pendientes = api.getPendientes(token)
            visitaDao.limpiarCachePendientes()
            visitaDao.insertarPendientes(pendientes.map { it.toCache() })
            Log.d(TAG, "Pendientes actualizados: ${pendientes.size} registros")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo refrescar pendientes (se usará caché): ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    /**
     * Carga el historial de visitas de una unidad.
     * Intenta primero desde el servidor; si falla, retorna los datos en caché.
     */
    suspend fun getHistorial(unidadId: Int): List<VisitaCompletaDto> {
        return try {
            val token   = sessionManager.bearerToken()
            val visitas = api.getVisitasPorUnidad(token, unidadId)
            Log.d(TAG, "Historial cargado desde servidor: ${visitas.size} visitas para unidad $unidadId")
            visitas
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo cargar historial del servidor: ${e.javaClass.simpleName} - ${e.message}")
            emptyList()
        }
    }

    // ── Sincronización de visitas PENDING → servidor ──────────────────────────

    /**
     * Sincroniza todas las visitas con estado PENDING enviándolas al servidor.
     * Actualiza el estado a SYNCED (con serverId) o ERROR según el resultado.
     */
    suspend fun syncPendingVisitas() {
        val pendientes = visitaDao.getVisitasPendientes()
        if (pendientes.isEmpty()) return

        Log.d(TAG, "Sincronizando ${pendientes.size} visitas pendientes...")
        val token = sessionManager.bearerToken()

        pendientes.forEach { visita ->
            try {
                val suministros = visitaDao.getSuministrosDeVisita(visita.localId)
                    .map { SuministroRequest(suministro_id = it.suministroId, cantidad = it.cantidad) }

                val request = CrearVisitaRequest(
                    unidad_id                = visita.unidadId,
                    estado_id                = visita.estadoId,
                    novedad_texto            = visita.novedadTexto,
                    observacion_estructurada = visita.observacionEstructurada,
                    requiere_seguimiento     = visita.requiereSeguimiento,
                    fecha_seguimiento        = visita.fechaSeguimiento,
                    nota_seguimiento         = visita.notaSeguimiento,
                    suministros              = suministros
                )

                val respuesta  = api.crearVisita(token, request)
                val serverId   = (respuesta["id"] as? Double)?.toInt()
                    ?: (respuesta["id"] as? Number)?.toInt()

                if (serverId != null) {
                    visitaDao.actualizarSyncStatus(visita.localId, serverId, "SYNCED")
                    Log.d(TAG, "Visita localId=${visita.localId} sincronizada → serverId=$serverId")
                } else {
                    visitaDao.actualizarSyncStatus(visita.localId, null, "ERROR")
                    Log.e(TAG, "Visita localId=${visita.localId}: el servidor no retornó ID")
                }
            } catch (e: Exception) {
                visitaDao.actualizarSyncStatus(visita.localId, null, "ERROR")
                Log.e(TAG, "Error sincronizando visita localId=${visita.localId}: ${e.javaClass.simpleName} - ${e.message}")
            }
        }
    }

    // ── Sincronización de fotos PENDING → servidor ────────────────────────────

    /**
     * Sube las fotos que aún no se han enviado al servidor.
     * Solo procesa fotos de visitas que ya tienen un [serverId] asignado.
     */
    suspend fun syncPendingFotos() {
        val fotos = visitaDao.getFotosPendientesDeSubir()
        if (fotos.isEmpty()) return

        Log.d(TAG, "Subiendo ${fotos.size} fotos pendientes...")
        val token = sessionManager.bearerToken()

        // Para cada foto necesitamos el serverId de su visita padre
        fotos.forEachIndexed { index, foto ->
            try {
                // Obtener el serverId de la visita padre (la query ya garantiza que existe)
                val serverId = obtenerServerIdDeVisita(foto.visitaLocalId) ?: run {
                    Log.w(TAG, "Foto id=${foto.id}: no se encontró serverId para localId=${foto.visitaLocalId}")
                    return@forEachIndexed
                }

                val uri  = Uri.parse(foto.localUri)
                val part = comprimirYCrearPart(uri, index + 1)
                val resp = api.subirFoto(token, serverId, part)

                visitaDao.marcarFotoSubida(foto.id, resp.url)
                Log.d(TAG, "Foto id=${foto.id} subida → url=${resp.url}")
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo subir foto id=${foto.id}: ${e.javaClass.simpleName} - ${e.message}")
            }
        }
    }

    // ── Utilidades internas ───────────────────────────────────────────────────

    /** Obtiene el serverId de una visita dado su localId. */
    private suspend fun obtenerServerIdDeVisita(visitaLocalId: Long): Int? =
        visitaDao.getServerIdByLocalId(visitaLocalId)

    /**
     * Comprime un [Uri] a JPEG ≤ 4 MB (máx 1920 px en el lado mayor)
     * y retorna un [MultipartBody.Part] listo para subir.
     * Lógica extraída del ViewModel para reutilización en el Worker.
     */
    suspend fun comprimirYCrearPart(uri: Uri, index: Int): MultipartBody.Part =
        withContext(Dispatchers.IO) {
            val inputStream = app.contentResolver.openInputStream(uri)!!
            val original    = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val maxSide = 1920
            val scaled  = if (original.width > maxSide || original.height > maxSide) {
                val ratio = maxSide.toFloat() / maxOf(original.width, original.height)
                val newW  = (original.width  * ratio).toInt()
                val newH  = (original.height * ratio).toInt()
                android.graphics.Bitmap.createScaledBitmap(original, newW, newH, true).also {
                    if (it !== original) original.recycle()
                }
            } else original

            var quality = 85
            var bytes:   ByteArray
            do {
                val baos = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
                bytes   = baos.toByteArray()
                quality -= 10
            } while (bytes.size > 4 * 1024 * 1024 && quality > 10)
            scaled.recycle()

            Log.d(TAG, "Foto $index comprimida: ${bytes.size / 1024}KB, quality=${quality + 10}")
            val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData("foto", "foto_$index.jpg", requestBody)
        }
}
