package com.Electroinova.inovinfo.ui.screens.nuevavisita

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.BuildConfig
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    // Formulario
    val unidadSeleccionada:      UnidadDto?  = null,
    val estadoSeleccionado:      EstadoDto?  = null,
    val novedadTecnico:          String      = "",
    val observacion:             String      = OBSERVACION_OK_DEFAULT,
    val suministrosSeleccionados: Set<Int>   = emptySet(),
    val requiereSeguimiento:     Boolean     = false,
    val fechaSeguimiento:        String      = "",
    val notaSeguimiento:         String      = "",
    val photoCount:              Int         = 0,

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
    private val sessionManager: SessionManager
) : ViewModel() {

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
                Log.e(TAG, "Error cargando catálogos: ${e.message}")
                _uiState.update { it.copy(catalogosLoading = false, errorCatalogos = "Sin conexión al servidor") }
            }
        }
    }

    // ── Actualizaciones de campos ─────────────────────────────────────────────

    fun onUnidadSelect(unidad: UnidadDto) =
        _uiState.update { it.copy(unidadSeleccionada = unidad) }

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

    fun onAddPhoto() =
        _uiState.update { if (it.photoCount < 8) it.copy(photoCount = it.photoCount + 1) else it }

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
                val suministros = _uiState.value.suministrosSeleccionados.map {
                    SuministroRequest(suministro_id = it, cantidad = 1)
                }
                inovInfoApi.crearVisita(
                    token   = sessionManager.bearerToken(),
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
                // Reset conservando catálogos e info de usuario
                val snap = _uiState.value
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
                Log.e(TAG, "Error guardando visita: ${e.message}")
                _uiState.update { it.copy(errorGuardar = "Error al guardar la visita", guardando = false) }
            }
        }
    }

    fun limpiarExito()       = _uiState.update { it.copy(guardadoExitoso = false) }
    fun limpiarErrorGuardar() = _uiState.update { it.copy(errorGuardar = null) }

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
            "Se instalaron/suministraron: ${suministros.joinToString(", ")}."
        else ""
        return """
Eres el asistente de redacción técnica de Electroinova Soluciones, empresa de rastreo vehicular.
El técnico ya realizó la visita a la unidad $numeroUnidad y describe lo que encontró con sus propias palabras.
Tu tarea es convertir esa descripción informal en una observación técnica formal para un informe oficial.

REGLAS ESTRICTAS:
- Usa voz pasiva impersonal: "Se evidenció...", "Se verificó...", "Se realizó...", "Se constató..."
- Tiempo pasado — la visita YA ocurrió
- Menciona los componentes específicos que el técnico nombró (cámaras, GPS, batería, DVR, etc.)
- Si el técnico realizó una acción correctiva, inclúyela como un hecho: "Se reemplazó...", "Se reconectó..."
- Si quedó algo sin resolver, escribe "Queda pendiente..." — NUNCA sugieras buscar otro especialista
- El técnico ES el especialista; no uses frases como "se recomienda revisión especializada"
- Estado reportado: $estado
- $suministrosTexto
- Máximo 3 oraciones. Sin saludo, sin cierre, sin guiones, solo el párrafo técnico.

DESCRIPCIÓN DEL TÉCNICO:
"$novedadLibre"

OBSERVACIÓN TÉCNICA FORMAL:
        """.trimIndent()
    }
}
