package com.Electroinova.inovinfo.ui.screens.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import com.Electroinova.inovinfo.data.remote.UnidadDto
import com.Electroinova.inovinfo.data.remote.VisitaCompletaDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistorialUiState(
    // Catálogo de unidades para búsqueda
    val unidades:          List<UnidadDto>         = emptyList(),
    val busquedaUnidad:    String                  = "",
    val unidadSeleccionada: UnidadDto?             = null,

    // Historial de la unidad seleccionada
    val visitas:           List<VisitaCompletaDto> = emptyList(),
    val isLoading:         Boolean                 = false,
    val error:             String?                 = null
)

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val api:            InovInfoApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init { cargarCatalogo() }

    /** Carga el catálogo de unidades al iniciar. */
    private fun cargarCatalogo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val unidades = api.getUnidades(sessionManager.bearerToken())
                _uiState.update { it.copy(unidades = unidades, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("HistorialVM", "Error catálogo: ${e.javaClass.simpleName} - ${e.message}", e)
                val msg = mensajeError(e)
                _uiState.update { it.copy(error = msg, isLoading = false) }
            }
        }
    }

    /** Actualiza el texto de búsqueda de unidad. */
    fun onBusquedaChange(query: String) {
        _uiState.update { it.copy(busquedaUnidad = query) }
    }

    /** Devuelve las unidades que coinciden con el texto de búsqueda (número o sede). */
    fun unidadesFiltradas(): List<UnidadDto> {
        val query = _uiState.value.busquedaUnidad.trim().lowercase()
        if (query.isEmpty()) return _uiState.value.unidades
        return _uiState.value.unidades.filter {
            it.numero.lowercase().contains(query) ||
                it.sede_nombre.lowercase().contains(query)
        }
    }

    /** Selecciona una unidad y carga su historial de visitas. */
    fun onUnidadSeleccionada(unidad: UnidadDto) {
        _uiState.update { it.copy(unidadSeleccionada = unidad, busquedaUnidad = "") }
        cargarHistorial(unidad.id)
    }

    /** Llama al endpoint y almacena el historial de la unidad. */
    fun cargarHistorial(unidadId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val visitas = api.getVisitasPorUnidad(sessionManager.bearerToken(), unidadId)
                _uiState.update { it.copy(visitas = visitas, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("HistorialVM", "Error historial: ${e.javaClass.simpleName} - ${e.message}", e)
                val msg = mensajeError(e)
                _uiState.update { it.copy(error = msg, isLoading = false) }
            }
        }
    }

    /** Vuelve al modo de búsqueda limpiando la selección y el historial. */
    fun limpiarSeleccion() {
        _uiState.update {
            it.copy(
                unidadSeleccionada = null,
                visitas            = emptyList(),
                busquedaUnidad     = "",
                error              = null
            )
        }
    }

    private fun mensajeError(e: Exception): String = when (e) {
        is java.net.UnknownHostException,
        is java.net.ConnectException -> "Sin conexión al servidor"
        is retrofit2.HttpException   -> "Error del servidor: ${e.code()}"
        else                         -> "Error: ${e.message}"
    }
}
