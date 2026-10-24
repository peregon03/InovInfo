package com.Electroinova.inovinfo.ui.screens.nuevavisita

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.BuildConfig
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

// ── Estado UI ─────────────────────────────────────────────────────────────────

data class SedeProgreso(
    val sedeId:            Int,
    val sedeNombre:        String,
    val unidadesSede:      List<UnidadDto>,
    val unidadesRevisadas: Set<String>
)

sealed interface GeminiUiState {
    data object Idle    : GeminiUiState
    data object Loading : GeminiUiState
    data object Success : GeminiUiState
    data class  Error(val mensaje: String) : GeminiUiState
}

data class NuevaVisitaUiState(
    // Info usuario
    val usuarioNombre:    String = "",
    val usuarioIniciales: String = "",

    // Catálogos
    val unidades:           List<UnidadDto>     = emptyList(),
    val suministrosCatalogo: List<SuministroDto> = emptyList(),
    val estados:            List<EstadoDto>     = emptyList(),
    val catalogosLoading:   Boolean             = true,
    val errorCatalogos:     String?             = null,

    // Búsqueda/filtro de unidades
    val busquedaUnidad:  String      = "",
    val filtroSedeId:    Int?        = null,

    // Formulario
    val unidadSeleccionada:      UnidadDto?  = null,
    val estadoSeleccionado:      EstadoDto?  = null,
    val novedadTecnico:          String      = "",
    val observacion:             String      = OBSERVACION_OK_DEFAULT,
    val suministrosSeleccionados: Set<Int>   = emptySet(),
    val requiereSeguimiento:     Boolean     = false,
    val fechaSeguimiento:        String      = "",
    val notaSeguimiento:         String      = "",
    val fotos:                   List<Uri>   = emptyList(),
    val subiendoFotos:           Boolean     = false,
    val progresoFotos:           String      = "",

    // Gemini
    val geminiState: GeminiUiState = GeminiUiState.Idle,

    // Rol
    val esCoordinadora: Boolean = false,

    // Progreso del día (multi-sede)
    val visitasHoy:           List<VisitaHoyDto>  = emptyList(),
    val sedesVisitadas:       List<SedeProgreso>   = emptyList(),
    val sedeProgresoActiva:   Int?                = null, // sede_id of currently shown progress

    // Seguimiento pendiente de la unidad seleccionada
    val seguimientoPendiente: PendienteDto?      = null,
    val pendientes:           List<PendienteDto> = emptyList(),

    // Guardado
    val guardando:        Boolean = false,
    val guardadoExitoso:  Boolean = false,
    val errorGuardar:     String? = null
)

internal const val OBSERVACION_OK_DEFAULT =
    "Se ejecutó el servicio de revisión, diagnóstico técnico, ajuste, reparación, " +
    "mantenimiento general y limpieza del sistema de cámaras"

private const val OBSERVACION_INOPERATIVA =
    "Al momento de la visita esta unidad se encuentra inoperativa"

private const val OBSERVACION_SIN_LLAVES =
    "Al momento de la inspección no se encontraron llaves"

private const val OBSERVACION_BAT_DESCONECTADA =
    "Al momento de la visita la batería de la unidad se encontraba desconectada"

private const val TAG = "NuevaVisitaVM"

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class NuevaVisitaViewModel @Inject constructor(
    private val geminiService:  GeminiService,
    private val inovInfoApi:    InovInfoApiService,
    private val sessionManager: SessionManager,
    private val app:            Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(NuevaVisitaUiState())
    val uiState: StateFlow<NuevaVisitaUiState> = _uiState.asStateFlow()

    init {
        val nombre = sessionManager.getNombre()
        val iniciales = nombre.split(" ")
            .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
        _uiState.update {
            it.copy(
                usuarioNombre    = nombre,
                usuarioIniciales = iniciales,
                esCoordinadora   = sessionManager.esCoordinadora()
            )
        }
        cargarCatalogos()
    }

    // ── Catálogos ─────────────────────────────────────────────────────────────

    fun cargarCatalogos() {
        viewModelScope.launch {
            _uiState.update { it.copy(catalogosLoading = true, errorCatalogos = null) }
            try {
                val token       = sessionManager.bearerToken()
                val unidades    = inovInfoApi.getUnidades(token)
                val suministros = inovInfoApi.getSuministros(token)
                val estados     = inovInfoApi.getEstados(token)
                val estadoOk    = estados.firstOrNull { it.nombre == "OK" }
                _uiState.update {
                    it.copy(
                        unidades            = unidades,
                        suministrosCatalogo = suministros,
                        estados             = estados,
                        estadoSeleccionado  = estadoOk,
                        catalogosLoading    = false
                    )
                }
                cargarProgresoHoy()
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando catálogos: ${e.javaClass.simpleName} - ${e.message}", e)
                val msg = when (e) {
                    is java.net.UnknownHostException,
                    is java.net.ConnectException -> "Sin conexión al servidor"
                    is retrofit2.HttpException -> "Error del servidor: ${e.code()}"
                    else -> "Error: ${e.message}"
                }
                _uiState.update { it.copy(catalogosLoading = false, errorCatalogos = msg) }
            }
        }
    }

    // ── Actualizaciones de campos ─────────────────────────────────────────────

    fun onBusquedaUnidadChange(query: String) =
        _uiState.update { it.copy(busquedaUnidad = query) }

    fun onFiltroSedeChange(sedeId: Int?) =
        _uiState.update { it.copy(filtroSedeId = sedeId) }

    fun unidadesFiltradas(): List<UnidadDto> {
        val state = _uiState.value
        val query = state.busquedaUnidad.trim().lowercase()
        return state.unidades.filter { u ->
            val coincideSede = state.filtroSedeId == null || u.sede_id == state.filtroSedeId
            val coincideBusqueda = query.isBlank() ||
                u.numero.lowercase().contains(query) ||
                u.sede_nombre.lowercase().contains(query)
            coincideSede && coincideBusqueda
        }
    }

    fun onObservacionChange(value: String) =
        _uiState.update { it.copy(observacion = value) }

    fun onUnidadSelect(unidad: UnidadDto) {
        val pendiente = _uiState.value.pendientes.firstOrNull { it.unidad_id == unidad.id }
        _uiState.update { state ->
            // Si esta sede no está aún en sedesVisitadas, se agrega
            val sedesActualizadas = if (state.sedesVisitadas.none { it.sedeId == unidad.sede_id }) {
                state.sedesVisitadas + SedeProgreso(
                    sedeId            = unidad.sede_id,
                    sedeNombre        = unidad.sede_nombre,
                    unidadesSede      = state.unidades.filter { it.sede_id == unidad.sede_id },
                    unidadesRevisadas = emptySet()
                )
            } else state.sedesVisitadas

            state.copy(
                unidadSeleccionada   = unidad,
                busquedaUnidad       = "",
                seguimientoPendiente = pendiente,
                sedesVisitadas       = sedesActualizadas,
                sedeProgresoActiva   = unidad.sede_id
            )
        }
    }

    // ── Progreso del día ──────────────────────────────────────────────────────

    private fun cargarProgresoHoy() {
        viewModelScope.launch {
            try {
                val token      = sessionManager.bearerToken()
                val visitasHoy = inovInfoApi.getVisitasHoy(token)
                val pendientes = inovInfoApi.getPendientes(token)

                // Agrupar visitas por sede para soporte multi-sede
                val visitasPorSede = visitasHoy.groupBy { it.sede_id }
                val todasUnidades  = _uiState.value.unidades

                val sedesProgreso = visitasPorSede.map { (sedeId, visitas) ->
                    val sedeNombre = visitas.first().sede_nombre
                    val revisadas  = visitas.map { it.unidad_numero }.toSet()
                    val unidadesDeSede = todasUnidades.filter { it.sede_id == sedeId }
                    SedeProgreso(
                        sedeId            = sedeId,
                        sedeNombre        = sedeNombre,
                        unidadesSede      = unidadesDeSede,
                        unidadesRevisadas = revisadas
                    )
                }

                _uiState.update {
                    it.copy(
                        visitasHoy         = visitasHoy,
                        pendientes         = pendientes,
                        sedesVisitadas     = sedesProgreso,
                        sedeProgresoActiva = sedesProgreso.firstOrNull()?.sedeId
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error cargando progreso: ${e.message}")
            }
        }
    }

    fun onEstadoSelect(estado: EstadoDto) {
        _uiState.update { it.copy(estadoSeleccionado = estado) }
        val textoDefault = when (estado.nombre) {
            "OK"                  -> OBSERVACION_OK_DEFAULT
            "Inoperativa"         -> OBSERVACION_INOPERATIVA
            "Sin llaves"          -> OBSERVACION_SIN_LLAVES
            "Bat. desconectada"   -> OBSERVACION_BAT_DESCONECTADA
            else                  -> null
        }
        if (textoDefault != null) {
            _uiState.update { it.copy(observacion = textoDefault, novedadTecnico = "") }
        }
    }

    fun onNovedadChange(value: String) =
        _uiState.update { it.copy(novedadTecnico = value) }

    fun onSuministroToggle(id: Int) = _uiState.update { state ->
        val nuevos = if (id in state.suministrosSeleccionados)
            state.suministrosSeleccionados - id
        else
            state.suministrosSeleccionados + id
        state.copy(suministrosSeleccionados = nuevos)
    }

    fun onSeguimientoToggle(value: Boolean) =
        _uiState.update { it.copy(requiereSeguimiento = value) }

    fun onFechaSeguimientoChange(value: String) =
        _uiState.update { it.copy(fechaSeguimiento = value) }

    fun onNotaSeguimientoChange(value: String) =
        _uiState.update { it.copy(notaSeguimiento = value) }

    fun onFotoAgregada(uri: Uri) = _uiState.update { state ->
        if (state.fotos.size < 8) state.copy(fotos = state.fotos + uri) else state
    }

    fun onFotoEliminada(index: Int) = _uiState.update { state ->
        state.copy(fotos = state.fotos.toMutableList().also { it.removeAt(index) })
    }

    // ── Guardar visita ────────────────────────────────────────────────────────

    fun guardarVisita() {
        val unidad = _uiState.value.unidadSeleccionada
        val estado = _uiState.value.estadoSeleccionado

        if (unidad == null) {
            _uiState.update { it.copy(errorGuardar = "Selecciona una unidad") }
            return
        }
        if (estado == null) {
            _uiState.update { it.copy(errorGuardar = "Selecciona un estado") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorGuardar = null) }
            try {
                val token       = sessionManager.bearerToken()
                val suministros = _uiState.value.suministrosSeleccionados.map {
                    SuministroRequest(suministro_id = it, cantidad = 1)
                }

                // 1. Crear la visita y obtener su ID
                val visitaMap = inovInfoApi.crearVisita(
                    token   = token,
                    request = CrearVisitaRequest(
                        unidad_id                = unidad.id,
                        estado_id                = estado.id,
                        novedad_texto            = _uiState.value.novedadTecnico.ifBlank { null },
                        observacion_estructurada = _uiState.value.observacion,
                        requiere_seguimiento     = _uiState.value.requiereSeguimiento,
                        fecha_seguimiento        = _uiState.value.fechaSeguimiento.ifBlank { null },
                        nota_seguimiento         = _uiState.value.notaSeguimiento.ifBlank { null },
                        suministros              = suministros
                    )
                )
                val visitaId = (visitaMap["id"] as? Double)?.toInt()
                    ?: (visitaMap["id"] as? Number)?.toInt()
                Log.d(TAG, "Visita creada — id=$visitaId, keys=${visitaMap.keys}, fotos=${_uiState.value.fotos.size}")

                // 2. Subir fotos comprimidas al servidor → Drive
                val fotosASubir = _uiState.value.fotos
                if (visitaId != null && fotosASubir.isNotEmpty()) {
                    fotosASubir.forEachIndexed { index, uri ->
                        _uiState.update {
                            it.copy(progresoFotos = "Subiendo foto ${index + 1}/${fotosASubir.size}...")
                        }
                        try {
                            val part = comprimirYCrearPart(uri, index + 1)
                            inovInfoApi.subirFoto(token, visitaId, part)
                            Log.d(TAG, "Foto ${index + 1} subida correctamente")
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo subir foto ${index + 1}: ${e.message}")
                        }
                    }
                }

                // 3. Reset conservando catálogos e info de usuario
                val snap     = _uiState.value
                val estadoOk = snap.estados.firstOrNull { it.nombre == "OK" }
                _uiState.update {
                    NuevaVisitaUiState(
                        usuarioNombre        = snap.usuarioNombre,
                        usuarioIniciales     = snap.usuarioIniciales,
                        unidades             = snap.unidades,
                        suministrosCatalogo  = snap.suministrosCatalogo,
                        estados              = snap.estados,
                        catalogosLoading     = false,
                        estadoSeleccionado   = estadoOk,
                        guardadoExitoso      = true
                    )
                }
                cargarProgresoHoy()
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando visita: ${e.javaClass.simpleName} - ${e.message}", e)
                val msg = when (e) {
                    is java.net.UnknownHostException,
                    is java.net.ConnectException -> "Sin conexión al servidor"
                    is retrofit2.HttpException -> "Error del servidor: ${e.code()} - ${e.response()?.errorBody()?.string()}"
                    else -> "Error: ${e.javaClass.simpleName} - ${e.message}"
                }
                _uiState.update { it.copy(errorGuardar = msg, guardando = false) }
            }
        }
    }

    fun limpiarExito()       = _uiState.update { it.copy(guardadoExitoso = false) }
    fun limpiarErrorGuardar() = _uiState.update { it.copy(errorGuardar = null) }

    fun onSedeProgresoChange(sedeId: Int) =
        _uiState.update { it.copy(sedeProgresoActiva = sedeId) }

    // ── Utilidades de fotos ───────────────────────────────────────────────────

    /**
     * Comprime un [Uri] a JPEG ≤ 4 MB, máx 1920px, y devuelve un MultipartBody.Part.
     */
    private suspend fun comprimirYCrearPart(uri: Uri, index: Int): MultipartBody.Part =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val inputStream = app.contentResolver.openInputStream(uri)!!
            val original = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val maxSide = 1920
            val scaled = if (original.width > maxSide || original.height > maxSide) {
                val ratio = maxSide.toFloat() / maxOf(original.width, original.height)
                val newW = (original.width * ratio).toInt()
                val newH = (original.height * ratio).toInt()
                android.graphics.Bitmap.createScaledBitmap(original, newW, newH, true).also {
                    if (it !== original) original.recycle()
                }
            } else original

            var quality = 85
            var bytes: ByteArray
            do {
                val baos = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
                bytes = baos.toByteArray()
                quality -= 10
            } while (bytes.size > 4 * 1024 * 1024 && quality > 10)
            scaled.recycle()

            Log.d(TAG, "Foto $index comprimida: ${bytes.size / 1024}KB, quality=${quality + 10}")
            val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData("foto", "foto_$index.jpg", requestBody)
        }

    // ── Gemini ────────────────────────────────────────────────────────────────

    fun estructurarObservacion() {
        val novedad = _uiState.value.novedadTecnico.trim()
        if (novedad.isBlank()) return

        val suministrosNombres = _uiState.value.suministrosCatalogo
            .filter { it.id in _uiState.value.suministrosSeleccionados }
            .map { it.nombre }
            .toSet()

        _uiState.update { it.copy(geminiState = GeminiUiState.Loading) }
        viewModelScope.launch {
            try {
                val prompt = buildPrompt(
                    estado       = _uiState.value.estadoSeleccionado?.nombre ?: "",
                    novedadLibre = novedad,
                    numeroUnidad = _uiState.value.unidadSeleccionada?.numero ?: "sin número",
                    suministros  = suministrosNombres
                )
                val request = GeminiRequest(
                    contents = listOf(GeminiContent("user", listOf(GeminiPart(prompt))))
                )
                val response   = geminiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val textResult = response.textoGenerado()

                if (textResult != null) {
                    _uiState.update { it.copy(observacion = textResult, geminiState = GeminiUiState.Success) }
                } else {
                    _uiState.update { it.copy(geminiState = GeminiUiState.Error("Sin respuesta de la IA")) }
                }
            } catch (e: retrofit2.HttpException) {
                val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: ""
                val msg = when (e.code()) {
                    429  -> "Cuota agotada. Espera un momento."
                    401, 403 -> "API Key inválida"
                    else -> "Error HTTP ${e.code()}"
                }
                _uiState.update { it.copy(geminiState = GeminiUiState.Error(msg)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(geminiState = GeminiUiState.Error("Error: ${e.message}")) }
            }
        }
    }

    fun limpiarErrorGemini() = _uiState.update { it.copy(geminiState = GeminiUiState.Idle) }

    private fun buildPrompt(
        estado:       String,
        novedadLibre: String,
        numeroUnidad: String,
        suministros:  Set<String>
    ): String {
        val suministrosTexto = if (suministros.isNotEmpty())
            "\nSuministros utilizados: ${suministros.joinToString(", ")}."
        else ""
        return """
Eres el redactor técnico de Electroinova SAS. Redactas informes de mantenimiento de DVR Móvil (sistema de cámaras en buses).

TEXTO BASE que siempre inicia la observación cuando hay servicio ejecutado:
"Se ejecutó el servicio de revisión, diagnóstico técnico, ajuste, reparación, mantenimiento general y limpieza del sistema de cámaras"

REGLAS DE REDACCIÓN:
1. Tercera persona, tiempo pasado, tono técnico formal. Sin punto final en la celda.
2. Si NO hay novedad (estado OK): devuelve SOLO el texto base, tal cual, sin modificarlo.
3. Si HAY hallazgo: al texto base se agrega tras una coma: ", durante la inspección se encuentra [hallazgo]" o ", durante la inspección se evidencia [hallazgo]".
   Ejemplo real: "Se ejecutó el servicio de revisión, diagnóstico técnico, ajuste, reparación, mantenimiento general y limpieza del sistema de cámaras, durante la inspección se encuentra visor fracturado"
4. Si la visita fue a solicitud de mantenimiento: "A solicitud del área de mantenimiento se realiza visita, encontrándose [hallazgo y causa si se conoce]".
5. Si se hizo acción correctiva, agregarla como hecho: "se ajustan conexiones", "se realiza cambio de este porque presentaba mala imagen".
6. Si hay causa del hallazgo (manipulación, inversión de conexiones, intervención eléctrica, deterioro), describir de forma objetiva. NUNCA culpar personas ni usar lenguaje coloquial.
7. El técnico ES el especialista — NUNCA sugieras "revisión especializada" ni "se recomienda consultar".
8. Si quedó algo sin resolver, escribir "Queda pendiente [qué]".
9. Sin saludo, sin cierre, sin guiones, sin listas. Solo el párrafo técnico continuo.
10. Sin punto final.
11. Ortografía correcta: "visita" (no "vista"), "imagen", "deterioro", "limpieza", "encontraron".
12. Máximo 2-3 oraciones unidas por comas o puntos seguidos.

DATOS DE ESTA VISITA:
- Unidad: $numeroUnidad
- Estado reportado: $estado$suministrosTexto

DESCRIPCIÓN INFORMAL DEL TÉCNICO:
"$novedadLibre"

Reescribe la descripción del técnico aplicando las reglas. Devuelve SOLO el texto de la observación, nada más:
        """.trimIndent()
    }
}
