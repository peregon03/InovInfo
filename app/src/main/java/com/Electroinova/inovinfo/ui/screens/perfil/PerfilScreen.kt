package com.Electroinova.inovinfo.ui.screens.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.Electroinova.inovinfo.data.remote.UsuarioAdminDto
import com.Electroinova.inovinfo.ui.navigation.Screen
import com.Electroinova.inovinfo.ui.theme.*

@Composable
fun PerfilScreen(
    navController: NavController,
    viewModel: PerfilViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCrearTecnicoDialog by remember { mutableStateOf(false) }
    var showCrearUnidadDialog  by remember { mutableStateOf(false) }

    // Colectar evento de cerrar sesión
    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is PerfilNavEvent.CerrarSesion -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── TopBar ────────────────────────────────────────────────────────────
        Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Mi Perfil",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        Column(
            modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Tarjeta de perfil ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = ElectroNavyBlue)
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier         = Modifier.size(72.dp).clip(CircleShape).background(ElectroGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = uiState.iniciales,
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color      = ElectroNavyBlueDark
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(uiState.nombre, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text(uiState.email, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.esCoordinadora) ElectroGold else Color(0xFF4CAF50)
                    ) {
                        Text(
                            text     = if (uiState.esCoordinadora) "Coordinadora" else "Técnico",
                            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color    = if (uiState.esCoordinadora) ElectroNavyBlueDark else Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Mensajes de éxito/error
            if (uiState.mensajeExito != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.mensajeExito!!, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                    }
                }
                LaunchedEffect(uiState.mensajeExito) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.limpiarMensaje()
                }
            }
            if (uiState.error != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(12.dp))
                }
            }

            // ── Gestión de técnicos (solo coordinadora) ───────────────────────
            if (uiState.esCoordinadora) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, null, tint = ElectroNavyBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Equipo técnico", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                            if (uiState.usuariosLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { viewModel.cargarUsuarios() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (uiState.usuarios.isEmpty() && !uiState.usuariosLoading) {
                            Text("Sin usuarios registrados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            uiState.usuarios.forEach { usuario ->
                                UsuarioRow(
                                    usuario       = usuario,
                                    onToggleActivo = { viewModel.toggleEstadoUsuario(usuario.id, usuario.activo) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick  = { showCrearTecnicoDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
                        ) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Agregar técnico")
                        }
                    }
                }
            }

            // ── Gestión de catálogos — unidades (solo coordinadora) ──────────
            if (uiState.esCoordinadora) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, null, tint = ElectroNavyBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Flota de unidades",
                                style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.catalogosLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { viewModel.cargarCatalogos() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (uiState.unidades.isEmpty() && !uiState.catalogosLoading) {
                            Text(
                                "Sin unidades registradas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.unidades.take(5).forEach { u ->
                                Row(
                                    modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Unidad ${u.numero}",
                                        style    = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        u.sede_nombre,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            }
                            if (uiState.unidades.size > 5) {
                                Text(
                                    "+ ${uiState.unidades.size - 5} más",
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick  = { showCrearUnidadDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Agregar unidad")
                        }
                    }
                }
            }

            // ── Cerrar sesión ─────────────────────────────────────────────────
            OutlinedButton(
                onClick  = { viewModel.cerrarSesion() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Dialog: Crear unidad ─────────────────────────────────────────────────
    if (showCrearUnidadDialog) {
        CrearUnidadDialog(
            sedes     = uiState.sedes,
            isLoading = uiState.crearUnidadLoading,
            onDismiss = { showCrearUnidadDialog = false },
            onCrear   = { numero, sedeId ->
                viewModel.crearUnidad(numero, sedeId)
                showCrearUnidadDialog = false
            }
        )
    }

    // ── Dialog: Crear técnico ─────────────────────────────────────────────────
    if (showCrearTecnicoDialog) {
        CrearTecnicoDialog(
            isLoading = uiState.crearTecnicoLoading,
            onDismiss = { showCrearTecnicoDialog = false },
            onCrear   = { nombre, email, password ->
                viewModel.crearTecnico(nombre, email, password)
                showCrearTecnicoDialog = false
            }
        )
    }
}

@Composable
private fun UsuarioRow(
    usuario: UsuarioAdminDto,
    onToggleActivo: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(36.dp).clip(CircleShape)
                .background(if (usuario.activo) ElectroNavyBlue.copy(alpha = 0.15f) else Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = usuario.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (usuario.activo) ElectroNavyBlue else Color.Gray
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(usuario.nombre, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(usuario.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (usuario.rol == "coordinadora") ElectroNavyBlue else Color(0xFF4CAF50)
            ) {
                Text(
                    text     = if (usuario.rol == "coordinadora") "Coord." else "Técnico",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (usuario.rol != "coordinadora") {
                Spacer(Modifier.height(4.dp))
                Switch(
                    checked         = usuario.activo,
                    onCheckedChange = { onToggleActivo() },
                    modifier        = Modifier.height(20.dp).padding(0.dp),
                    colors          = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF4CAF50)
                    )
                )
            }
        }
    }
}

@Composable
private fun CrearTecnicoDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCrear:   (nombre: String, email: String, password: String) -> Unit
) {
    var nombre   by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo técnico", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = nombre,
                    onValueChange = { nombre = it },
                    label         = { Text("Nombre completo") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it },
                    label         = { Text("Correo electrónico") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value                = password,
                    onValueChange        = { password = it },
                    label                = { Text("Contraseña temporal") },
                    singleLine           = true,
                    modifier             = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onCrear(nombre, email, password) },
                enabled  = !isLoading && nombre.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Crear técnico")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearUnidadDialog(
    sedes:     List<com.Electroinova.inovinfo.data.remote.SedeDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCrear:   (numero: String, sedeId: Int) -> Unit
) {
    var numero       by remember { mutableStateOf("") }
    var sedeExpanded by remember { mutableStateOf(false) }
    var sedeSelected by remember { mutableStateOf(sedes.firstOrNull()) }

    // Si las sedes se cargan después de abrir el dialog
    LaunchedEffect(sedes) { if (sedeSelected == null) sedeSelected = sedes.firstOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva unidad", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = numero,
                    onValueChange = { numero = it },
                    label         = { Text("Número de unidad") },
                    placeholder   = { Text("Ej: 001") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded         = sedeExpanded,
                    onExpandedChange = { sedeExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = sedeSelected?.nombre ?: "Sin sedes",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Sede") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sedeExpanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded         = sedeExpanded,
                        onDismissRequest = { sedeExpanded = false }
                    ) {
                        sedes.forEach { sede ->
                            DropdownMenuItem(
                                text    = { Text(sede.nombre) },
                                onClick = { sedeSelected = sede; sedeExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { sedeSelected?.let { onCrear(numero, it.id) } },
                enabled  = !isLoading && numero.isNotBlank() && sedeSelected != null,
                colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Agregar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
