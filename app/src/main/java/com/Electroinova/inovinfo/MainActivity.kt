package com.Electroinova.inovinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.Electroinova.inovinfo.ui.navigation.AppNavigation
import com.Electroinova.inovinfo.ui.navigation.Screen
import com.Electroinova.inovinfo.ui.screens.auth.AuthNavEvent
import com.Electroinova.inovinfo.ui.screens.auth.AuthViewModel
import com.Electroinova.inovinfo.ui.theme.ElectroGold
import com.Electroinova.inovinfo.ui.theme.ElectroNavyBlue
import com.Electroinova.inovinfo.ui.theme.InovInfoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            InovInfoTheme {
                val navController  = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Rutas que NO muestran la barra de navegación inferior
                val authRoutes = setOf(
                    Screen.Login.route,
                    Screen.Register.route,
                    Screen.RecuperarPassword.route,
                    "${Screen.Verificacion.route}/{modo}"
                )
                val showBottomBar = currentRoute != null && currentRoute !in authRoutes

                // Colectar eventos de navegación del AuthViewModel
                LaunchedEffect(Unit) {
                    authViewModel.navEvent.collect { event ->
                        when (event) {
                            is AuthNavEvent.IrAMain -> {
                                navController.navigate(Screen.NuevaVisita.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            is AuthNavEvent.IrALogin -> {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            is AuthNavEvent.IrAVerificacion -> {
                                navController.navigate("${Screen.Verificacion.route}/${event.modo}")
                            }
                            is AuthNavEvent.IrARecuperar -> {
                                navController.navigate(Screen.RecuperarPassword.route)
                            }
                            is AuthNavEvent.RecuperacionExitosa -> {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }

                // Verificar sesión al iniciar la app
                LaunchedEffect(Unit) {
                    authViewModel.checkSesion()
                }

                val bottomNavItems = listOf(
                    BottomNavItem(Screen.NuevaVisita, Icons.Default.AddCircle,    "Nueva Visita"),
                    BottomNavItem(Screen.Historial,   Icons.Default.List,          "Historial"),
                    BottomNavItem(Screen.Pendientes,  Icons.Default.Notifications, "Pendientes"),
                    BottomNavItem(Screen.Admin,        Icons.Default.Settings,     "Admin"),
                )

                Scaffold(
                    modifier  = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(containerColor = ElectroNavyBlue) {
                                bottomNavItems.forEach { item ->
                                    val selected = currentRoute == item.screen.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick  = {
                                            navController.navigate(item.screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState    = true
                                            }
                                        },
                                        icon  = {
                                            Icon(
                                                imageVector        = item.icon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor   = ElectroGold,
                                            selectedTextColor   = ElectroGold,
                                            unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                                            unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                                            indicatorColor      = ElectroNavyBlue
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(
                        navController = navController,
                        authViewModel = authViewModel,
                        modifier      = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(
    val screen: Screen,
    val icon:   ImageVector,
    val label:  String
)
