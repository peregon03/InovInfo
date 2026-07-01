package com.Electroinova.inovinfo.data.remote

import retrofit2.http.*

interface InovInfoApiService {

    // ── Catálogos ─────────────────────────────────────────────────────────────
    @GET("api/catalogos/unidades")
    suspend fun getUnidades(@Header("Authorization") token: String): List<UnidadDto>

    @GET("api/catalogos/suministros")
    suspend fun getSuministros(@Header("Authorization") token: String): List<SuministroDto>

    @GET("api/catalogos/estados")
    suspend fun getEstados(@Header("Authorization") token: String): List<EstadoDto>

    // ── Visitas ───────────────────────────────────────────────────────────────
    @GET("api/visitas/hoy")
    suspend fun getVisitasHoy(@Header("Authorization") token: String): List<VisitaHoyDto>

    @GET("api/visitas/pendientes")
    suspend fun getPendientes(@Header("Authorization") token: String): List<PendienteDto>

    @POST("api/visitas")
    suspend fun crearVisita(
        @Header("Authorization") token: String,
        @Body request: CrearVisitaRequest
    ): Map<String, Any>

    @GET("api/catalogos/sedes")
    suspend fun getSedes(@Header("Authorization") token: String): List<SedeDto>

    @POST("api/catalogos/unidades")
    suspend fun crearUnidad(
        @Header("Authorization") token: String,
        @Body request: CrearUnidadRequest
    ): UnidadDto

    // ── Usuarios (coordinadora) ───────────────────────────────────────────────
    @GET("api/usuarios")
    suspend fun getUsuarios(@Header("Authorization") token: String): List<UsuarioAdminDto>

    @PUT("api/usuarios/{id}/estado")
    suspend fun actualizarEstadoUsuario(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ActualizarEstadoRequest
    ): Map<String, String>
}
