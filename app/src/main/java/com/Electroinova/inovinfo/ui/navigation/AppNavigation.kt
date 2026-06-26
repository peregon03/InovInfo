package com.Electroinova.inovinfo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.Electroinova.inovinfo.ui.screens.admin.AdminScreen
import com.Electroinova.inovinfo.ui.screens.historial.HistorialScreen
import com.Electroinova.inovinfo.ui.screens.nuevavisita.NuevaVisitaScreen
import com.Electroinova.inovinfo.ui.screens.pendientes.PendientesScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.NuevaVisita.route,
        modifier         = modifier
    ) {
        composable(Screen.NuevaVisita.route) { NuevaVisitaScreen() }
        composable(Screen.Historial.route)   { HistorialScreen() }
        composable(Screen.Pendientes.route)  { PendientesScreen() }
        composable(Screen.Admin.route)       { AdminScreen() }
    }
}
