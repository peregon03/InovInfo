package com.Electroinova.inovinfo.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.GenerarInformeRequest
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import com.Electroinova.inovinfo.data.remote.SedeDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    // Sedes
    val sedes: List<SedeDto> = emptyList(),
    val sedeSeleccionada: SedeDto? = null,

    // Fechas (formato "YYYY-MM-DD")
    val fechaDesde: String = "",
    val fechaHasta: String = "",

    // Estado de generación
    val generando: Boolean = false,
    val sheetUrl: String? = null,
    val pestana: String? = null,
    val totalVisitas: Int? = null,
    val error: String? = null,
    val exito: Boolean = false
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiService: InovInfoApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        cargarSedes()
    }

    private fun cargarSedes() {
        viewModelScope.launch {
            try {
                val token = sessionManager.bearerToken()
                val sedes = apiService.getSedes(token)
                _uiState.update { it.copy(sedes = sedes) }
            } catch (e: Exception) {
                // Sedes no críticas, se puede usar sin filtrar
            }
        }
    }

    fun onSedeSeleccionada(sede: SedeDto?) {
        _uiState.update { it.copy(sedeSeleccionada = sede) }
    }

    fun onFechaDesdeChanged(fecha: String) {
        _uiState.update { it.copy(fechaDesde = fecha) }
    }

    fun onFechaHastaChanged(fecha: String) {
        _uiState.update { it.copy(fechaHasta = fecha) }
    }

    fun generarInforme() {
        val state = _uiState.value
        if (state.fechaDesde.isBlank() || state.fechaHasta.isBlank()) {
            _uiState.update { it.copy(error = "Selecciona las fechas del informe") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(generando = true, error = null, exito = false, sheetUrl = null) }

            try {
                val token = sessionManager.bearerToken()
                val request = GenerarInformeRequest(
                    fechaDesde = state.fechaDesde,
                    fechaHasta = state.fechaHasta,
                    sedeId     = state.sedeSeleccionada?.id,
                    sedeNombre = state.sedeSeleccionada?.nombre
                )

                val response = apiService.generarInforme(token, request)

                _uiState.update {
                    it.copy(
                        generando    = false,
                        exito        = true,
                        sheetUrl     = response.sheetUrl,
                        pestana      = response.pestana,
                        totalVisitas = response.totalVisitas
                    )
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val msg = try {
                    val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                    json.get("error")?.asString ?: "Error ${e.code()}"
                } catch (_: Exception) {
                    "Error ${e.code()}: ${e.message()}"
                }
                _uiState.update { it.copy(generando = false, error = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(generando = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun limpiarEstado() {
        _uiState.update { it.copy(error = null, exito = false, sheetUrl = null) }
    }
}
