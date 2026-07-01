package com.Electroinova.inovinfo.data.remote

// ── Requests ──────────────────────────────────────────────────────────────────

data class LoginRequest(val email: String, val password: String)

data class SolicitarRegistroRequest(val nombre: String, val email: String, val password: String)

data class VerificarCodigoRequest(val email: String, val codigo: String)

data class RecuperarPasswordRequest(
    val email:          String,
    val codigo:         String,
    val nuevaPassword:  String
)

data class CrearTecnicoRequest(val nombre: String, val email: String, val password: String)

// ── Responses ─────────────────────────────────────────────────────────────────

data class AuthResponse(
    val token:   String,
    val usuario: UsuarioResponse
)

data class UsuarioResponse(
    val id:       Int,
    val nombre:   String,
    val email:    String,
    val rol:      String,
    val permisos: List<String>? = null
)

data class MensajeResponse(val mensaje: String)

data class PrimerUsuarioResponse(val primerUsuario: Boolean)
