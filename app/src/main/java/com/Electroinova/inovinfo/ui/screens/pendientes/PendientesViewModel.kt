package com.Electroinova.inovinfo.ui.screens.pendientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.PendienteDto
import com.Electroinova.inovinfo.data.repository.VisitaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendientesUiState(
    val pendientes: List<PendienteDto> = emptyList(),
    val isLoading:  Boolean            = true,
    val error:      String?            = null
)

@HiltViewModel
class PendientesViewModel @Inject constructor(
    private val visitaRepo:     VisitaRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendientesUiState())
    val uiState: StateFlow<PendientesUiState> = _uiState.asStateFlow()

    init {
        // Colectar pendientes desde Room (actualización reactiva)
        viewModelScope.launch {
            visitaRepo.getPendientes().collect { pendientes ->
                _uiState.update { it.copy(pendientes = pendientes, isLoading = false) }
            }
        }
        // Refresh inicial desde el servidor
        cargarPendientes()
    }

    fun cargarPendientes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                visitaRepo.refreshPendientes()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("PendientesVM", "Error: ${e.javaClass.simpleName} - ${e.message}", e)
                val msg = when (e) {
                    is java.net.UnknownHostException,
                    is java.net.ConnectException -> "Sin conexión al servidor"
                    is retrofit2.HttpException -> "Error del servidor: ${e.code()}"
                    else -> "Error: ${e.message}"
                }
                _uiState.update { it.copy(error = msg, isLoading = false) }
            }
        }
    }
}
