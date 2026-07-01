package com.Electroinova.inovinfo.ui.navigation

sealed class Screen(val route: String) {
    // ── Auth ──────────────────────────────────────────────────────────────────
    object Login             : Screen("login")
    object Register          : Screen("register")
    object Verificacion      : Screen("verificacion")
    object RecuperarPassword : Screen("recuperar_password")

    // ── Principal ─────────────────────────────────────────────────────────────
    object NuevaVisita : Screen("nueva_visita")
    object Historial   : Screen("historial")
    object Pendientes  : Screen("pendientes")
    object Perfil      : Screen("perfil")
}
