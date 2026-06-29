package com.Electroinova.inovinfo.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("inovinfo_session", Context.MODE_PRIVATE)

    fun guardarSesion(token: String, id: Int, nombre: String, email: String, rol: String, permisos: List<String>) {
        prefs.edit()
            .putString("token",    token)
            .putInt("id",          id)
            .putString("nombre",   nombre)
            .putString("email",    email)
            .putString("rol",      rol)
            .putString("permisos", permisos.joinToString(","))
            .apply()
    }

    fun getToken():    String?  = prefs.getString("token",  null)
    fun getNombre():   String   = prefs.getString("nombre", "") ?: ""
    fun getEmail():    String   = prefs.getString("email",  "") ?: ""
    fun getRol():      String   = prefs.getString("rol",    "") ?: ""
    fun getId():       Int      = prefs.getInt("id", -1)
    fun getPermisos(): List<String> {
        val raw = prefs.getString("permisos", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    fun isLoggedIn():        Boolean = getToken() != null
    fun esCoordinadora():    Boolean = getRol() == "coordinadora"
    fun tienePermiso(p: String): Boolean = esCoordinadora() || getPermisos().contains(p)

    fun bearerToken(): String = "Bearer ${getToken()}"

    fun cerrarSesion() = prefs.edit().clear().apply()
}
