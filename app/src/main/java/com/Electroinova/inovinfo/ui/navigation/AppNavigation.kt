package com.Electroinova.inovinfo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.Electroinova.inovinfo.ui.screens.auth.AuthViewModel
import com.Electroinova.inovinfo.ui.screens.auth.LoginScreen
import com.Electroinova.inovinfo.ui.screens.auth.RecuperarPasswordScreen
import com.Electroinova.inovinfo.ui.screens.auth.RegisterScreen
import com.Electroinova.inovinfo.ui.screens.auth.VerificacionScreen
import com.Electroinova.inovinfo.ui.screens.historial.HistorialScreen
import com.Electroinova.inovinfo.ui.screens.nuevavisita.NuevaVisitaScreen
import com.Electroinova.inovinfo.ui.screens.pendientes.PendientesScreen
import com.Electroinova.inovinfo.ui.screens.perfil.PerfilScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Login.route,
        modifier         = modifier
    ) {
        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(authViewModel, navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(authViewModel, navController)
        }
        composable(
            route = "${Screen.Verificacion.route}/{modo}",
            arguments = listOf(navArgument("modo") { type = NavType.StringType })
        ) { backStackEntry ->
            val modo = backStackEntry.arguments?.getString("modo") ?: "registro"
            VerificacionScreen(authViewModel, navController, modo)
        }
        composable(Screen.RecuperarPassword.route) {
            RecuperarPasswordScreen(authViewModel, navController)
        }

        // ── Principal ─────────────────────────────────────────────────────────
        composable(Screen.NuevaVisita.route) {
            NuevaVisitaScreen(onNavigateToPerfil = {
                navController.navigate(Screen.Perfil.route) {
                    launchSingleTop = true
                    restoreState    = true
                }
            })
        }
        composable(Screen.Historial.route)   { HistorialScreen() }
        composable(Screen.Pendientes.route)  { PendientesScreen() }
        composable(Screen.Perfil.route)      { PerfilScreen(navController) }
    }
}
