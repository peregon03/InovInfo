package com.Electroinova.inovinfo.ui.screens.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import com.Electroinova.inovinfo.data.remote.VisitaHoyDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistorialUiState(
    val visitas:   List<VisitaHoyDto> = emptyList(),
    val isLoading: Boolean            = true,
    val error:     String?            = null
)

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val api:            InovInfoApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init { cargarVisitas() }

    fun cargarVisitas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val visitas = api.getVisitasHoy(sessionManager.bearerToken())
                _uiState.update { it.copy(visitas = visitas, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión al servidor", isLoading = false) }
            }
        }
    }
}
