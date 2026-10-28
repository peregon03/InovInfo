package com.Electroinova.inovinfo.ui.screens.nuevavisita

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.BuildConfig
import com.Electroinova.inovinfo.core.sync.ConnectivityObserver
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.local.entity.toDto
import com.Electroinova.inovinfo.data.remote.EstadoDto
import com.Electroinova.inovinfo.data.remote.GeminiContent
import com.Electroinova.inovinfo.data.remote.GeminiPart
import com.Electroinova.inovinfo.data.remote.GeminiRequest
import com.Electroinova.inovinfo.data.remote.GeminiService
import com.Electroinova.inovinfo.data.remote.PendienteDto
import com.Electroinova.inovinfo.data.remote.SuministroDto
import com.Electroinova.inovinfo.data.remote.UnidadDto
import com.Electroinova.inovinfo.data.remote.VisitaHoyDto
import com.Electroinova.inovinfo.data.remote.textoGenerado
import com.Electroinova.inovinfo.data.repository.CatalogoRepository
import com.Electroinova.inovinfo.data.repository.VisitaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Estado UI ─────────────────────────────────────────────────────────────────

data class SedeProgreso(
    val sedeId:            Int,
    val sedeNombre:        String,
    val unidadesSede:      List<UnidadDto>,
    val unidadesRevisadas: Map<String, String> = emptyMap()
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
    val suministrosSeleccionados: Map<Int, Int> = emptyMap(),
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

    // Conectividad
    val isOnline: Boolean = true,

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
    private val sessionManager: SessionManager,
    private val catalogoRepo:   CatalogoRepository,
    private val visitaRepo:     VisitaRepository,
    private val connectivity:   ConnectivityObserver,
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

        // Colectar catálogos desde Room
        viewModelScope.launch {
            catalogoRepo.getUnidades().collect { entities ->
                _uiState.update { it.copy(unidades = entities.map { e -> e.toDto() }) }
            }
        }
        viewModelScope.launch {
            catalogoRepo.getSuministros().collect { entities ->
                _uiState.update { it.copy(suministrosCatalogo = entities.map { e -> e.toDto() }) }
            }
        }
        viewModelScope.launch {
            catalogoRepo.getEstados().collect { entities ->
                val dtos = entities.map { e -> e.toDto() }
                val estadoOk = dtos.firstOrNull { it.nombre == "OK" }
                _uiState.update { it.copy(estados = dtos, estadoSeleccionado = it.estadoSeleccionado ?: estadoOk) }
            }
        }

        // Colectar visitas del día desde Room
        viewModelScope.launch {
            visitaRepo.getVisitasHoy().collect { visitasHoy ->
                val visitasPorSede = visitasHoy.groupBy { it.sede_id }
                val todasUnidades  = _uiState.value.unidades
                val sedesProgreso = visitasPorSede.map { (sedeId, visitas) ->
                    val sedeNombre = visitas.first().sede_nombre
                    val revisadas  = visitas.associate { it.unidad_numero to it.estado }
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
                        sedesVisitadas     = sedesProgreso,
                        sedeProgresoActiva = sedesProgreso.firstOrNull()?.sedeId
                    )
                }
            }
        }
        viewModelScope.launch {
            visitaRepo.getPendientes().collect { pendientes ->
                _uiState.update { it.copy(pendientes = pendientes) }
            }
        }

        // Colectar conectividad
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        // Refresh inicial (fire and forget — si falla se usa el caché de Room)
        viewModelScope.launch {
            catalogoRepo.refreshCatalogos()
            _uiState.update { it.copy(catalogosLoading = false) }
        }
        viewModelScope.launch {
            visitaRepo.refreshVisitasHoy()
            visitaRepo.refreshPendientes()
        }
    }

    // ── Catálogos ─────────────────────────────────────────────────────────────

    /** Fuerza un refresh manual de catálogos desde el servidor. */
    fun cargarCatalogos() {
        viewModelScope.launch {
            _uiState.update { it.copy(catalogosLoading = true, errorCatalogos = null) }
            catalogoRepo.refreshCatalogos()
            _uiState.update { it.copy(catalogosLoading = false) }
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
                    unidadesRevisadas = emptyMap()
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
            state.suministrosSeleccionados + (id to 1)
        state.copy(suministrosSeleccionados = nuevos)
    }

    fun onSuministroCantidadChange(id: Int, delta: Int) = _uiState.update { state ->
        val cantidadActual = state.suministrosSeleccionados[id] ?: return@update state
        val nuevaCantidad  = cantidadActual + delta
        val nuevos = if (nuevaCantidad < 1)
            state.suministrosSeleccionados - id
        else
            state.suministrosSeleccionados + (id to nuevaCantidad)
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
        val unidad = _uiState.value.unidadSeleccionada ?: run {
            _uiState.update { it.copy(errorGuardar = "Selecciona una unidad") }; return
        }
        val estado = _uiState.value.estadoSeleccionado ?: run {
            _uiState.update { it.copy(errorGuardar = "Selecciona un estado") }; return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorGuardar = null) }
            try {
                val localId = visitaRepo.guardarVisitaLocal(
                    unidadId                = unidad.id,
                    estadoId                = estado.id,
                    novedadTexto            = _uiState.value.novedadTecnico.ifBlank { null },
                    observacionEstructurada = _uiState.value.observacion,
                    requiereSeguimiento     = _uiState.value.requiereSeguimiento,
                    fechaSeguimiento        = _uiState.value.fechaSeguimiento.ifBlank { null },
                    notaSeguimiento         = _uiState.value.notaSeguimiento.ifBlank { null },
                    suministros             = _uiState.value.suministrosSeleccionados,
                    fotosUri                = _uiState.value.fotos
                )
                Log.d(TAG, "Visita guardada localmente — localId=$localId")

                // Resetear formulario conservando catálogos e info de usuario
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
                        esCoordinadora       = snap.esCoordinadora,
                        isOnline             = snap.isOnline,
                        guardadoExitoso      = true
                    )
                }

                // Sincronizar en background (fire and forget)
                launch {
                    visitaRepo.syncPendingVisitas()
                    visitaRepo.syncPendingFotos()
                    visitaRepo.refreshVisitasHoy()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando visita: ${e.javaClass.simpleName} - ${e.message}", e)
                _uiState.update { it.copy(errorGuardar = "Error: ${e.message}", guardando = false) }
            }
        }
    }

    fun limpiarExito()       = _uiState.update { it.copy(guardadoExitoso = false) }
    fun limpiarErrorGuardar() = _uiState.update { it.copy(errorGuardar = null) }

    fun onSedeProgresoChange(sedeId: Int) =
        _uiState.update { it.copy(sedeProgresoActiva = sedeId) }

    // ── Gemini ────────────────────────────────────────────────────────────────

    fun estructurarObservacion() {
        val novedad = _uiState.value.novedadTecnico.trim()
        if (novedad.isBlank()) return

        val suministrosNombres = _uiState.value.suministrosCatalogo
            .filter { it.id in _uiState.value.suministrosSeleccionados.keys }
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
                val msg = when (e.code()) {
                    429      -> "Cuota agotada. Espera un momento."
                    401, 403 -> "API Key inválida"
                    else     -> "Error HTTP ${e.code()}"
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
