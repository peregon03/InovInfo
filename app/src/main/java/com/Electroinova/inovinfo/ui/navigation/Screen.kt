package com.Electroinova.inovinfo.ui.navigation

sealed class Screen(val route: String) {
    object NuevaVisita : Screen("nueva_visita")
    object Historial   : Screen("historial")
    object Pendientes  : Screen("pendientes")
    object Admin       : Screen("admin")
}
