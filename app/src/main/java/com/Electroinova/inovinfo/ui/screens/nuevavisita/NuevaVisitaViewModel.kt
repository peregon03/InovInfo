package com.Electroinova.inovinfo.ui.screens.nuevavisita

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.BuildConfig
import com.Electroinova.inovinfo.data.remote.GeminiContent
import com.Electroinova.inovinfo.data.remote.GeminiPart
import com.Electroinova.inovinfo.data.remote.GeminiRequest
import com.Electroinova.inovinfo.data.remote.GeminiService
import com.Electroinova.inovinfo.data.remote.textoGenerado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Estado de UI ──────────────────────────────────────────────────────────────

data class NuevaVisitaUiState(
    val numeroUnidad:        String          = "",
    val estadoSeleccionado:  String          = "OK",
    val novedadTecnico:      String          = "",
    val observacion:         String          = OBSERVACION_OK_DEFAULT,
    val selectedSuministros: Set<String>     = emptySet(),
    val requiereSeguimiento: Boolean         = false,
    val fechaSeguimiento:    String          = "",
    val notaSeguimiento:     String          = "",
    val photoCount:          Int             = 0,
    val geminiState:         GeminiUiState   = GeminiUiState.Idle
)

sealed interface GeminiUiState {
    data object Idle      : GeminiUiState
    data object Loading   : GeminiUiState
    data object Success   : GeminiUiState
    data class  Error(val mensaje: String) : GeminiUiState
}

private const val OBSERVACION_OK_DEFAULT =
    "Se realizó visita técnica a la unidad. Sistema DVR Móvil verificado en óptimas " +
    "condiciones de operación. Cámaras operativas, GPS activo y batería en rango normal. " +
    "Sin novedades reportadas."

// ── ViewModel ─────────────────────────────────────────────────────────────────

private const val TAG = "NuevaVisitaVM"

@HiltViewModel
class NuevaVisitaViewModel @Inject constructor(
    private val geminiService: GeminiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NuevaVisitaUiState())
    val uiState: StateFlow<NuevaVisitaUiState> = _uiState.asStateFlow()

    // ── Actualizaciones de campos ─────────────────────────────────────────────

    fun onNumeroUnidadChange(value: String) =
        _uiState.update { it.copy(numeroUnidad = value) }

    fun onEstadoChange(estado: String) {
        _uiState.update { it.copy(estadoSeleccionado = estado) }
        if (estado == "OK") {
            _uiState.update { it.copy(observacion = OBSERVACION_OK_DEFAULT) }
        }
    }

    fun onNovedadChange(value: String) =
        _uiState.update { it.copy(novedadTecnico = value) }

    fun onSuministroToggle(suministro: String) = _uiState.update { state ->
        val nuevos = if (suministro in state.selectedSuministros)
            state.selectedSuministros - suministro
        else
            state.selectedSuministros + suministro
        state.copy(selectedSuministros = nuevos)
    }

    fun onSeguimientoToggle(value: Boolean) =
        _uiState.update { it.copy(requiereSeguimiento = value) }

    fun onFechaSeguimientoChange(value: String) =
        _uiState.update { it.copy(fechaSeguimiento = value) }

    fun onNotaSeguimientoChange(value: String) =
        _uiState.update { it.copy(notaSeguimiento = value) }

    fun onAddPhoto() =
        _uiState.update { if (it.photoCount < 8) it.copy(photoCount = it.photoCount + 1) else it }

    // ── Gemini: estructurar observación ──────────────────────────────────────

    fun estructurarObservacion() {
        val estado  = _uiState.value.estadoSeleccionado
        val novedad = _uiState.value.novedadTecnico.trim()

        if (novedad.isBlank()) return

        _uiState.update { it.copy(geminiState = GeminiUiState.Loading) }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Llamando a Gemini 2.5 Flash. Key presente: ${BuildConfig.GEMINI_API_KEY.isNotBlank()}")

                val prompt = buildPrompt(
                    estado        = estado,
                    novedadLibre  = novedad,
                    numeroUnidad  = _uiState.value.numeroUnidad.ifBlank { "sin número" },
                    suministros   = _uiState.value.selectedSuministros
                )

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                    )
                )

                val response   = geminiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val textResult = response.textoGenerado()

                Log.d(TAG, "Respuesta Gemini: candidates=${response.candidates?.size}, texto=${textResult?.take(80)}")

                if (textResult != null) {
                    _uiState.update { it.copy(
                        observacion = textResult,
                        geminiState = GeminiUiState.Success
                    )}
                } else {
                    val msg = "Gemini no devolvió texto. Candidates: ${response.candidates}"
                    Log.w(TAG, msg)
                    _uiState.update { it.copy(geminiState = GeminiUiState.Error(msg)) }
                }

            } catch (e: retrofit2.HttpException) {
                val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: "sin cuerpo"
                Log.e(TAG, "HttpException ${e.code()} — body: $errorBody", e)
                val msg = when (e.code()) {
                    400  -> "400 Bad Request — body: $errorBody"
                    429  -> "Cuota agotada. Espera un momento y reintenta. Genera una nueva key si persiste."
                    401,
                    403  -> "API Key inválida o sin permisos. Verifica tu key en local.properties."
                    404  -> "Modelo no encontrado. Revisa el nombre del modelo en GeminiService."
                    else -> "Error HTTP ${e.code()}: $errorBody"
                }
                _uiState.update { it.copy(geminiState = GeminiUiState.Error(msg)) }

            } catch (e: Exception) {
                Log.e(TAG, "Error llamando a Gemini: ${e.javaClass.simpleName} — ${e.message}", e)
                _uiState.update { it.copy(
                    geminiState = GeminiUiState.Error("Error de conexión: ${e.message}")
                )}
            }
        }
    }

    fun limpiarErrorGemini() =
        _uiState.update { it.copy(geminiState = GeminiUiState.Idle) }

    // ── Construcción del prompt ───────────────────────────────────────────────

    private fun buildPrompt(
        estado:       String,
        novedadLibre: String,
        numeroUnidad: String,
        suministros:  Set<String>
    ): String {
        val suministrosTexto = if (suministros.isNotEmpty())
            "Se instalaron/suministraron: ${suministros.joinToString(", ")}."
        else
            ""

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
