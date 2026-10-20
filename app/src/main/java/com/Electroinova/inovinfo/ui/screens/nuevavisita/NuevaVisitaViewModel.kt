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

    // Guardado
    val guardando:        Boolean = false,
    val guardadoExitoso:  Boolean = false,
    val errorGuardar:     String? = null
)

internal const val OBSERVACION_OK_DEFAULT =
    "Se realizó visita técnica a la unidad. Sistema DVR Móvil verificado en óptimas " +
    "condiciones de operación. Cámaras operativas, GPS activo y batería en rango normal. " +
    "Sin novedades reportadas."

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

    fun onUnidadSelect(unidad: UnidadDto) =
        _uiState.update { it.copy(unidadSeleccionada = unidad, busquedaUnidad = "") }

    fun onEstadoSelect(estado: EstadoDto) {
        _uiState.update { it.copy(estadoSeleccionado = estado) }
        if (estado.nombre == "OK") {
            _uiState.update { it.copy(observacion = OBSERVACION_OK_DEFAULT, novedadTecnico = "") }
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

                // 2. Subir fotos una a una si existen y la visita se creó correctamente
                val fotosASubir = _uiState.value.fotos
                if (visitaId != null && fotosASubir.isNotEmpty()) {
                    fotosASubir.forEachIndexed { index, uri ->
                        _uiState.update {
                            it.copy(progresoFotos = "Subiendo foto ${index + 1}/${fotosASubir.size}...")
                        }
                        try {
                            val part = uriToMultipart(uri, index + 1)
                            inovInfoApi.subirFoto(token, visitaId, part)
                        } catch (e: Exception) {
                            // Un error en una foto no cancela el guardado — se registra y continúa
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

    // ── Utilidades de fotos ───────────────────────────────────────────────────

    /**
     * Convierte un [Uri] local en un [MultipartBody.Part] listo para Retrofit.
     * Lee los bytes en memoria (máx. 5 MB, validado en el servidor).
     */
    private fun uriToMultipart(uri: Uri, index: Int): MultipartBody.Part {
        val contentResolver = app.contentResolver
        val mimeType        = contentResolver.getType(uri) ?: "image/jpeg"
        val bytes           = contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val requestBody     = bytes.toRequestBody(mimeType.toMediaType())
        return MultipartBody.Part.createFormData("foto", "foto_$index.jpg", requestBody)
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
