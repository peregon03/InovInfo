package com.Electroinova.inovinfo.ui.screens.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.AuthApiService
import com.Electroinova.inovinfo.data.remote.CrearTecnicoRequest
import com.Electroinova.inovinfo.data.remote.CrearUnidadRequest
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import com.Electroinova.inovinfo.data.remote.SedeDto
import com.Electroinova.inovinfo.data.remote.UnidadDto
import com.Electroinova.inovinfo.data.remote.UsuarioAdminDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerfilUiState(
    val nombre:                String               = "",
    val email:                 String               = "",
    val rol:                   String               = "",
    val iniciales:             String               = "",
    val esCoordinadora:        Boolean              = false,

    // Equipo
    val usuarios:              List<UsuarioAdminDto> = emptyList(),
    val usuariosLoading:       Boolean              = false,
    val crearTecnicoLoading:   Boolean              = false,

    // Catálogos
    val sedes:                 List<SedeDto>        = emptyList(),
    val unidades:              List<UnidadDto>      = emptyList(),
    val catalogosLoading:      Boolean              = false,
    val crearUnidadLoading:    Boolean              = false,

    val mensajeExito:          String?              = null,
    val error:                 String?              = null
)

sealed interface PerfilNavEvent {
    data object CerrarSesion : PerfilNavEvent
}

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val inovInfoApi:    InovInfoApiService,
    private val authApi:        AuthApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<PerfilNavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    init {
        val nombre = sessionManager.getNombre()
        val iniciales = nombre.split(" ")
            .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("").ifEmpty { "?" }
        _uiState.update {
            it.copy(
                nombre          = nombre,
                email           = sessionManager.getEmail(),
                rol             = sessionManager.getRol(),
                iniciales       = iniciales,
                esCoordinadora  = sessionManager.esCoordinadora()
            )
        }
        if (sessionManager.esCoordinadora()) {
            cargarUsuarios()
            cargarCatalogos()
        }
    }

    fun cargarUsuarios() {
        viewModelScope.launch {
            _uiState.update { it.copy(usuariosLoading = true) }
            try {
                val lista = inovInfoApi.getUsuarios(sessionManager.bearerToken())
                _uiState.update { it.copy(usuarios = lista, usuariosLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(usuariosLoading = false) }
            }
        }
    }

    fun cargarCatalogos() {
        viewModelScope.launch {
            _uiState.update { it.copy(catalogosLoading = true) }
            try {
                val token    = sessionManager.bearerToken()
                val sedes    = inovInfoApi.getSedes(token)
                val unidades = inovInfoApi.getUnidades(token)
                _uiState.update { it.copy(sedes = sedes, unidades = unidades, catalogosLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(catalogosLoading = false) }
            }
        }
    }

    fun crearUnidad(numero: String, sedeId: Int) {
        if (numero.isBlank()) {
            _uiState.update { it.copy(error = "El número de unidad es requerido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(crearUnidadLoading = true, error = null) }
            try {
                inovInfoApi.crearUnidad(
                    sessionManager.bearerToken(),
                    CrearUnidadRequest(numero.trim(), sedeId)
                )
                _uiState.update {
                    it.copy(crearUnidadLoading = false, mensajeExito = "Unidad $numero agregada correctamente")
                }
                cargarCatalogos()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 409) "El número de unidad ya existe" else "Error al crear unidad"
                _uiState.update { it.copy(error = msg, crearUnidadLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión al servidor", crearUnidadLoading = false) }
            }
        }
    }

    fun crearTecnico(nombre: String, email: String, password: String) {
        if (nombre.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Completa todos los campos") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(crearTecnicoLoading = true, error = null) }
            try {
                authApi.crearTecnico(
                    sessionManager.bearerToken(),
                    CrearTecnicoRequest(nombre.trim(), email.trim(), password)
                )
                _uiState.update {
                    it.copy(
                        crearTecnicoLoading = false,
                        mensajeExito        = "Técnico ${nombre.trim()} creado correctamente"
                    )
                }
                cargarUsuarios()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 409) "El email ya está registrado" else "Error al crear técnico"
                _uiState.update { it.copy(error = msg, crearTecnicoLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión al servidor", crearTecnicoLoading = false) }
            }
        }
    }

    fun toggleEstadoUsuario(id: Int, activoActual: Boolean) {
        viewModelScope.launch {
            try {
                inovInfoApi.actualizarEstadoUsuario(
                    sessionManager.bearerToken(),
                    id,
                    com.Electroinova.inovinfo.data.remote.ActualizarEstadoRequest(!activoActual)
                )
                cargarUsuarios()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar el estado") }
            }
        }
    }

    fun cerrarSesion() {
        sessionManager.cerrarSesion()
        viewModelScope.launch { _navEvent.emit(PerfilNavEvent.CerrarSesion) }
    }

    fun limpiarMensaje() = _uiState.update { it.copy(mensajeExito = null, error = null) }
}
