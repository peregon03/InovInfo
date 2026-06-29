package com.Electroinova.inovinfo.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @GET("api/auth/check-primer-usuario")
    suspend fun checkPrimerUsuario(): PrimerUsuarioResponse

    @POST("api/auth/solicitar-registro")
    suspend fun solicitarRegistro(@Body request: SolicitarRegistroRequest): MensajeResponse

    @POST("api/auth/verificar-registro")
    suspend fun verificarRegistro(@Body request: VerificarCodigoRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/solicitar-recuperacion")
    suspend fun solicitarRecuperacion(@Body request: Map<String, String>): MensajeResponse

    @POST("api/auth/verificar-recuperacion")
    suspend fun verificarRecuperacion(@Body request: RecuperarPasswordRequest): MensajeResponse

    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") token: String): UsuarioResponse

    @POST("api/auth/crear-tecnico")
    suspend fun crearTecnico(
        @Header("Authorization") token: String,
        @Body request: CrearTecnicoRequest
    ): Map<String, UsuarioResponse>
}
