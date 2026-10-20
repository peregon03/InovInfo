package com.Electroinova.inovinfo.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.ActualizarSedeRequest
import com.Electroinova.inovinfo.data.remote.CrearSedeRequest
import com.Electroinova.inovinfo.data.remote.GenerarInformeRequest
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import com.Electroinova.inovinfo.data.remote.SedeDto
import com.Electroinova.inovinfo.data.remote.UnidadDto
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
    val sedesLoading: Boolean = false,
    val crearSedeLoading: Boolean = false,

    // Unidades
    val unidades: List<UnidadDto> = emptyList(),
    val unidadesLoading: Boolean = false,

    // Fechas (formato "YYYY-MM-DD")
    val fechaDesde: String = "",
    val fechaHasta: String = "",

    // Estado de generación
    val generando: Boolean = false,
    val sheetUrl: String? = null,
    val pestana: String? = null,
    val totalVisitas: Int? = null,
    val error: String? = null,
    val exito: Boolean = false,

    // Mensajes gestión catálogos
    val mensajeExito: String? = null
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
        cargarUnidades()
    }

    fun cargarSedes() {
        viewModelScope.launch {
            _uiState.update { it.copy(sedesLoading = true) }
            try {
                val token = sessionManager.bearerToken()
                val sedes = apiService.getSedes(token)
                _uiState.update { it.copy(sedes = sedes, sedesLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(sedesLoading = false) }
            }
        }
    }

    fun cargarUnidades() {
        viewModelScope.launch {
            _uiState.update { it.copy(unidadesLoading = true) }
            try {
                val token    = sessionManager.bearerToken()
                val unidades = apiService.getUnidades(token)
                _uiState.update { it.copy(unidades = unidades, unidadesLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(unidadesLoading = false) }
            }
        }
    }

    fun crearSede(nombre: String) {
        if (nombre.isBlank()) {
            _uiState.update { it.copy(error = "El nombre de la sede es requerido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(crearSedeLoading = true, error = null) }
            try {
                val token = sessionManager.bearerToken()
                apiService.crearSede(token, CrearSedeRequest(nombre.trim()))
                _uiState.update { it.copy(crearSedeLoading = false, mensajeExito = "Sede '${nombre.trim()}' creada correctamente") }
                cargarSedes()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 409) "Ya existe una sede con ese nombre" else "Error al crear la sede"
                _uiState.update { it.copy(crearSedeLoading = false, error = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(crearSedeLoading = false, error = "Sin conexión al servidor") }
            }
        }
    }

    fun actualizarSede(id: Int, nombre: String) {
        if (nombre.isBlank()) {
            _uiState.update { it.copy(error = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val token = sessionManager.bearerToken()
                apiService.actualizarSede(token, id, ActualizarSedeRequest(nombre.trim(), activa = true))
                _uiState.update { it.copy(mensajeExito = "Sede actualizada correctamente") }
                cargarSedes()
                cargarUnidades()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar la sede") }
            }
        }
    }

    fun limpiarMensaje() = _uiState.update { it.copy(mensajeExito = null, error = null) }

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
