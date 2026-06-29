package com.Electroinova.inovinfo.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.AuthApiService
import com.Electroinova.inovinfo.data.remote.LoginRequest
import com.Electroinova.inovinfo.data.remote.RecuperarPasswordRequest
import com.Electroinova.inovinfo.data.remote.SolicitarRegistroRequest
import com.Electroinova.inovinfo.data.remote.VerificarCodigoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading:      Boolean = false,
    val error:          String? = null,
    val mensaje:        String? = null,
    val primerUsuario:  Boolean = false,
    val emailPendiente: String  = ""
)

sealed interface AuthNavEvent {
    data object IrAMain            : AuthNavEvent
    data object IrALogin           : AuthNavEvent
    data class  IrAVerificacion(val modo: String) : AuthNavEvent
    data object IrARecuperar       : AuthNavEvent
    data object RecuperacionExitosa: AuthNavEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi:        AuthApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState  = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<AuthNavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    // ── Verificar si ya hay sesión activa ─────────────────────────────────────
    fun checkSesion() {
        if (!sessionManager.isLoggedIn()) {
            checkPrimerUsuario()
            return
        }
        viewModelScope.launch {
            try {
                authApi.me(sessionManager.bearerToken())
                _navEvent.emit(AuthNavEvent.IrAMain)
            } catch (e: Exception) {
                sessionManager.cerrarSesion()
                checkPrimerUsuario()
            }
        }
    }

    fun checkPrimerUsuario() {
        viewModelScope.launch {
            try {
                val result = authApi.checkPrimerUsuario()
                _uiState.update { it.copy(primerUsuario = result.primerUsuario) }
            } catch (e: Exception) {
                _uiState.update { it.copy(primerUsuario = false) }
            }
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Completa todos los campos") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authApi.login(LoginRequest(email.trim(), password))
                guardarSesion(response)
                _navEvent.emit(AuthNavEvent.IrAMain)
            } catch (e: retrofit2.HttpException) {
                val msg = when (e.code()) {
                    401  -> "Email o contraseña incorrectos"
                    else -> "Error al iniciar sesión"
                }
                _uiState.update { it.copy(error = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión con el servidor") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Registro (primer usuario = coordinadora) ──────────────────────────────
    fun solicitarRegistro(nombre: String, email: String, password: String) {
        if (nombre.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Completa todos los campos") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authApi.solicitarRegistro(SolicitarRegistroRequest(nombre.trim(), email.trim(), password))
                _uiState.update { it.copy(emailPendiente = email.trim()) }
                _navEvent.emit(AuthNavEvent.IrAVerificacion("registro"))
            } catch (e: retrofit2.HttpException) {
                val body = e.response()?.errorBody()?.string() ?: ""
                _uiState.update { it.copy(error = extraerError(body, "Error al enviar código")) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión con el servidor") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun verificarRegistro(codigo: String) {
        val email = _uiState.value.emailPendiente
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authApi.verificarRegistro(VerificarCodigoRequest(email, codigo))
                guardarSesion(response)
                _navEvent.emit(AuthNavEvent.IrAMain)
            } catch (e: retrofit2.HttpException) {
                _uiState.update { it.copy(error = "Código inválido o expirado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión con el servidor") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Recuperar contraseña ──────────────────────────────────────────────────
    fun solicitarRecuperacion(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa tu email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authApi.solicitarRecuperacion(mapOf("email" to email.trim()))
                _uiState.update { it.copy(emailPendiente = email.trim(), mensaje = "Código enviado a $email") }
                _navEvent.emit(AuthNavEvent.IrAVerificacion("recuperacion"))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión con el servidor") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun verificarRecuperacion(codigo: String, nuevaPassword: String) {
        val email = _uiState.value.emailPendiente
        if (codigo.isBlank() || nuevaPassword.isBlank()) {
            _uiState.update { it.copy(error = "Completa todos los campos") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authApi.verificarRecuperacion(RecuperarPasswordRequest(email, codigo, nuevaPassword))
                _navEvent.emit(AuthNavEvent.RecuperacionExitosa)
            } catch (e: retrofit2.HttpException) {
                _uiState.update { it.copy(error = "Código inválido o expirado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión con el servidor") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun limpiarError()   = _uiState.update { it.copy(error = null) }
    fun limpiarMensaje() = _uiState.update { it.copy(mensaje = null) }

    private fun guardarSesion(response: com.Electroinova.inovinfo.data.remote.AuthResponse) {
        sessionManager.guardarSesion(
            token    = response.token,
            id       = response.usuario.id,
            nombre   = response.usuario.nombre,
            email    = response.usuario.email,
            rol      = response.usuario.rol,
            permisos = response.usuario.permisos
        )
    }

    private fun extraerError(body: String, default: String): String {
        return try {
            val msg = body.substringAfter("\"error\":\"").substringBefore("\"")
            if (msg.isNotBlank()) msg else default
        } catch (e: Exception) { default }
    }
}
