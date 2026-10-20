package com.Electroinova.inovinfo.data.remote

import okhttp3.MultipartBody
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

    @Multipart
    @POST("api/visitas/{id}/fotos")
    suspend fun subirFoto(
        @Header("Authorization") token: String,
        @Path("id") visitaId: Int,
        @Part foto: MultipartBody.Part
    ): FotoResponse

    @DELETE("api/visitas/{id}/fotos/{fotoId}")
    suspend fun eliminarFoto(
        @Header("Authorization") token: String,
        @Path("id") visitaId: Int,
        @Path("fotoId") fotoId: Int
    ): Map<String, String>

    @GET("api/catalogos/sedes")
    suspend fun getSedes(@Header("Authorization") token: String): List<SedeDto>

    @POST("api/catalogos/unidades")
    suspend fun crearUnidad(
        @Header("Authorization") token: String,
        @Body request: CrearUnidadRequest
    ): UnidadDto

    @PUT("api/catalogos/unidades/{id}")
    suspend fun actualizarUnidad(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ActualizarUnidadRequest
    ): UnidadDto

    @DELETE("api/catalogos/unidades/{id}")
    suspend fun eliminarUnidad(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Map<String, String>

    @POST("api/catalogos/suministros")
    suspend fun crearSuministro(
        @Header("Authorization") token: String,
        @Body request: CrearSuministroRequest
    ): SuministroDto

    @PUT("api/catalogos/suministros/{id}")
    suspend fun actualizarSuministro(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ActualizarSuministroRequest
    ): SuministroDto

    @POST("api/catalogos/sedes")
    suspend fun crearSede(
        @Header("Authorization") token: String,
        @Body request: CrearSedeRequest
    ): SedeDto

    @PUT("api/catalogos/sedes/{id}")
    suspend fun actualizarSede(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ActualizarSedeRequest
    ): SedeDto

    // ── Usuarios (coordinadora) ───────────────────────────────────────────────
    @GET("api/usuarios")
    suspend fun getUsuarios(@Header("Authorization") token: String): List<UsuarioAdminDto>

    @PUT("api/usuarios/{id}/estado")
    suspend fun actualizarEstadoUsuario(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ActualizarEstadoRequest
    ): Map<String, String>

    // ── Informes ────────────────────────────────────────────────────────────
    @POST("api/informes/generar")
    suspend fun generarInforme(
        @Header("Authorization") token: String,
        @Body request: GenerarInformeRequest
    ): InformeResponse

    // ── Importación masiva desde Excel ───────────────────────────────────────
    @Multipart
    @POST("api/importar/excel")
    suspend fun importarExcel(
        @Header("Authorization") token: String,
        @Part archivo: MultipartBody.Part
    ): ImportarExcelResponse
}
