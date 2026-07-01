package com.Electroinova.inovinfo.ui.screens.pendientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import com.Electroinova.inovinfo.data.remote.PendienteDto
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
    private val api:            InovInfoApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendientesUiState())
    val uiState: StateFlow<PendientesUiState> = _uiState.asStateFlow()

    init { cargarPendientes() }

    fun cargarPendientes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val pendientes = api.getPendientes(sessionManager.bearerToken())
                _uiState.update { it.copy(pendientes = pendientes, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión al servidor", isLoading = false) }
            }
        }
    }
}
