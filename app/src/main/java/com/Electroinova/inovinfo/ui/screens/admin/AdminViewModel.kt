package com.Electroinova.inovinfo.ui.screens.admin

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Electroinova.inovinfo.data.local.SessionManager
import com.Electroinova.inovinfo.data.local.entity.toDto
import com.Electroinova.inovinfo.data.remote.ActualizarSedeRequest
import com.Electroinova.inovinfo.data.remote.ActualizarSuministroRequest
import com.Electroinova.inovinfo.data.remote.ActualizarUnidadRequest
import com.Electroinova.inovinfo.data.remote.CrearSedeRequest
import com.Electroinova.inovinfo.data.remote.CrearSuministroRequest
import com.Electroinova.inovinfo.data.remote.CrearUnidadRequest
import com.Electroinova.inovinfo.data.remote.GenerarInformeRequest
import com.Electroinova.inovinfo.data.remote.InovInfoApiService
import com.Electroinova.inovinfo.data.remote.SedeDto
import com.Electroinova.inovinfo.data.remote.SuministroDto
import com.Electroinova.inovinfo.data.remote.UnidadDto
import com.Electroinova.inovinfo.data.repository.CatalogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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
    val crearUnidadLoading: Boolean = false,

    // Suministros
    val suministros: List<SuministroDto> = emptyList(),
    val suministrosLoading: Boolean = false,

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
    val mensajeExito: String? = null,

    // Importación masiva desde Excel
    val importando:            Boolean = false,
    val resultadoImportacion:  String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiService:     InovInfoApiService,
    private val sessionManager: SessionManager,
    private val catalogoRepo:   CatalogoRepository,
    private val app:            Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        // Colectar sedes, unidades y suministros desde Room (actualización reactiva)
        viewModelScope.launch {
            catalogoRepo.getSedes().collect { entities ->
                _uiState.update { it.copy(sedes = entities.map { e -> e.toDto() }) }
            }
        }
        viewModelScope.launch {
            catalogoRepo.getUnidades().collect { entities ->
                _uiState.update { it.copy(unidades = entities.map { e -> e.toDto() }) }
            }
        }
        viewModelScope.launch {
            catalogoRepo.getSuministros().collect { entities ->
                _uiState.update { it.copy(suministros = entities.map { e -> e.toDto() }) }
            }
        }
        // Refresh inicial desde el servidor
        cargarSedes()
        cargarUnidades()
        cargarSuministros()
    }

    fun cargarSedes() {
        viewModelScope.launch {
            _uiState.update { it.copy(sedesLoading = true) }
            try {
                catalogoRepo.refreshCatalogos()
                _uiState.update { it.copy(sedesLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(sedesLoading = false) }
            }
        }
    }

    fun cargarUnidades() {
        viewModelScope.launch {
            _uiState.update { it.copy(unidadesLoading = true) }
            try {
                catalogoRepo.refreshCatalogos()
                _uiState.update { it.copy(unidadesLoading = false) }
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
                // Actualizar caché en Room tras la operación
                catalogoRepo.refreshCatalogos()
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
                // Actualizar caché en Room tras la operación
                catalogoRepo.refreshCatalogos()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar la sede") }
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
                val token = sessionManager.bearerToken()
                apiService.crearUnidad(token, CrearUnidadRequest(numero.trim(), sedeId))
                _uiState.update { it.copy(crearUnidadLoading = false, mensajeExito = "Unidad '${numero.trim()}' creada correctamente") }
                // Actualizar caché en Room tras la operación
                catalogoRepo.refreshCatalogos()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 409) "Ya existe una unidad con ese número en esa sede" else "Error al crear la unidad"
                _uiState.update { it.copy(crearUnidadLoading = false, error = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(crearUnidadLoading = false, error = "Sin conexión al servidor") }
            }
        }
    }

    fun actualizarUnidad(id: Int, numero: String, sedeId: Int, activa: Boolean) {
        if (numero.isBlank()) {
            _uiState.update { it.copy(error = "El número no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val token = sessionManager.bearerToken()
                apiService.actualizarUnidad(token, id, ActualizarUnidadRequest(numero.trim(), sedeId, activa))
                _uiState.update { it.copy(mensajeExito = "Unidad actualizada correctamente") }
                // Actualizar caché en Room tras la operación
                catalogoRepo.refreshCatalogos()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar la unidad") }
            }
        }
    }

    fun eliminarUnidad(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val token = sessionManager.bearerToken()
                apiService.eliminarUnidad(token, id)
                _uiState.update { it.copy(mensajeExito = "Unidad eliminada correctamente") }
                // Actualizar caché en Room tras la operación
                catalogoRepo.refreshCatalogos()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo eliminar la unidad") }
            }
        }
    }

    fun cargarSuministros() {
        viewModelScope.launch {
            _uiState.update { it.copy(suministrosLoading = true) }
            try {
                catalogoRepo.refreshCatalogos()
                _uiState.update { it.copy(suministrosLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(suministrosLoading = false) }
            }
        }
    }

    fun crearSuministro(nombre: String, categoria: String?, precioRef: Double?) {
        if (nombre.isBlank()) {
            _uiState.update { it.copy(error = "El nombre del suministro es requerido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val token = sessionManager.bearerToken()
                apiService.crearSuministro(
                    token,
                    CrearSuministroRequest(
                        nombre     = nombre.trim(),
                        categoria  = categoria?.takeIf { it.isNotBlank() },
                        precio_ref = precioRef
                    )
                )
                _uiState.update { it.copy(mensajeExito = "Suministro '${nombre.trim()}' creado correctamente") }
                // Actualizar caché en Room tras la operación
                catalogoRepo.refreshCatalogos()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 409) "Ya existe un suministro con ese nombre" else "Error al crear el suministro"
                _uiState.update { it.copy(error = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión al servidor") }
            }
        }
    }

    fun actualizarSuministro(id: Int, nombre: String, categoria: String?, precioRef: Double?, activo: Boolean) {
        if (nombre.isBlank()) {
            _uiState.update { it.copy(error = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val token = sessionManager.bearerToken()
                apiService.actualizarSuministro(
                    token, id,
                    ActualizarSuministroRequest(
                        nombre     = nombre.trim(),
                        categoria  = categoria?.takeIf { it.isNotBlank() },
                        precio_ref = precioRef,
                        activo     = activo
                    )
                )
                _uiState.update { it.copy(mensajeExito = "Suministro actualizado correctamente") }
                // Actualizar caché en Room tras la operación
                catalogoRepo.refreshCatalogos()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar el suministro") }
            }
        }
    }

    fun importarExcel(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(importando = true, error = null) }
            try {
                val contentResolver = app.contentResolver
                val inputStream     = contentResolver.openInputStream(uri)!!
                val bytes           = inputStream.readBytes()
                inputStream.close()

                val requestBody = bytes.toRequestBody(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaType()
                )
                val part = MultipartBody.Part.createFormData("archivo", "import.xlsx", requestBody)

                val response = apiService.importarExcel(sessionManager.bearerToken(), part)

                val msg = buildString {
                    append("${response.unidadesCreadas} unidades creadas")
                    if (response.unidadesActualizadas > 0) append(", ${response.unidadesActualizadas} actualizadas")
                    if (response.sedesCreadas > 0) append(", ${response.sedesCreadas} sedes nuevas")
                    if (response.sinPlaca > 0) append("\n${response.sinPlaca} unidades sin placa")
                    if (response.advertencias.isNotEmpty()) append("\n${response.advertencias.size} advertencias")
                    if (response.errores.isNotEmpty()) append("\n${response.errores.size} errores")
                }
                _uiState.update {
                    it.copy(
                        importando           = false,
                        resultadoImportacion = msg,
                        mensajeExito         = msg
                    )
                }
                // Actualizar caché en Room con los datos importados
                catalogoRepo.refreshCatalogos()
            } catch (e: Exception) {
                _uiState.update { it.copy(importando = false, error = "Error al importar: ${e.message}") }
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
