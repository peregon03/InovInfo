package com.Electroinova.inovinfo.ui.screens.nuevavisita

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Electroinova.inovinfo.data.remote.EstadoDto
import com.Electroinova.inovinfo.ui.components.PhotoGrid
import com.Electroinova.inovinfo.ui.components.estadoColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NuevaVisitaScreen(
    onNavigateToPerfil: () -> Unit = {},
    viewModel: NuevaVisitaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var unidadExpanded     by remember { mutableStateOf(false) }
    var suministrosExpanded by remember { mutableStateOf(false) }

    val hoy = LocalDate.now().format(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "CO"))
    )

    val mostrarNovedad = uiState.estadoSeleccionado != null &&
        uiState.estadoSeleccionado!!.nombre != "OK" &&
        uiState.estadoSeleccionado!!.nombre != "Inoperativa"

    val badgePatron = when {
        uiState.estadoSeleccionado?.nombre == "OK" -> "Patrón estándar"
        uiState.geminiState is GeminiUiState.Success -> "Redactado por IA ✦"
        uiState.novedadTecnico.isNotBlank()           -> "Redactado por IA ✦"
        else                                           -> null
    }

    // Snackbar de éxito
    if (uiState.guardadoExitoso) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2500)
            viewModel.limpiarExito()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            // ── TopBar ────────────────────────────────────────────────────────
            Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddCircle, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nueva Visita",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(hoy, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                    Box(
                        modifier         = Modifier.size(38.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            uiState.usuarioIniciales,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }

            // ── Cargando catálogos ────────────────────────────────────────────
            if (uiState.catalogosLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando datos...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                return@Column
            }

            if (uiState.errorCatalogos != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(uiState.errorCatalogos!!, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.cargarCatalogos() }) { Text("Reintentar") }
                    }
                }
                return@Column
            }

            // ── Sin unidades registradas ──────────────────────────────────────
            if (uiState.unidades.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Sin unidades registradas",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (uiState.esCoordinadora)
                                "Agrega las unidades de flota desde el Perfil para poder registrar visitas."
                            else
                                "No hay unidades disponibles aún. Contacta a la coordinadora para que las registre.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.esCoordinadora) {
                            Spacer(Modifier.height(20.dp))
                            Button(onClick = onNavigateToPerfil) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ir al Perfil")
                            }
                        }
                    }
                }
                return@Column
            }

            // ── Formulario ────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Banner éxito
                if (uiState.guardadoExitoso) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Visita guardada correctamente", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // 1. Seleccionar unidad
                ExposedDropdownMenuBox(
                    expanded         = unidadExpanded,
                    onExpandedChange = { unidadExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = uiState.unidadSeleccionada?.let { "Unidad ${it.numero} · ${it.sede_nombre}" } ?: "",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Unidad") },
                        placeholder   = { Text("Selecciona una unidad") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unidadExpanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded         = unidadExpanded,
                        onDismissRequest = { unidadExpanded = false }
                    ) {
                        if (uiState.unidades.isEmpty()) {
                            DropdownMenuItem(
                                text    = { Text("Sin unidades registradas", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {}
                            )
                        } else {
                            uiState.unidades.forEach { unidad ->
                                DropdownMenuItem(
                                    text    = { Text("${unidad.numero} · ${unidad.sede_nombre}") },
                                    onClick = { viewModel.onUnidadSelect(unidad); unidadExpanded = false }
                                )
                            }
                        }
                    }
                }

                // 2. Estado
                Column {
                    Text("Estado de la unidad", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.estados.forEach { estado ->
                            val (bgSel, fgSel) = estadoColor(estado.nombre)
                            FilterChip(
                                selected = uiState.estadoSeleccionado?.id == estado.id,
                                onClick  = { viewModel.onEstadoSelect(estado) },
                                label    = { Text(estado.nombre, style = MaterialTheme.typography.labelSmall) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = bgSel,
                                    selectedLabelColor     = fgSel
                                )
                            )
                        }
                    }
                }

                // 3. Novedad (condicional)
                if (mostrarNovedad) {
                    OutlinedTextField(
                        value         = uiState.novedadTecnico,
                        onValueChange = viewModel::onNovedadChange,
                        label         = { Text("Novedad del técnico") },
                        placeholder   = { Text("Describe la novedad en tus palabras...") },
                        minLines      = 3,
                        maxLines      = 5,
                        trailingIcon  = {
                            IconButton(onClick = {}) { Icon(Icons.Default.Mic, contentDescription = "Voz") }
                        },
                        modifier      = Modifier.fillMaxWidth()
                    )

                    if (uiState.novedadTecnico.isNotBlank()) {
                        Button(
                            onClick  = { viewModel.estructurarObservacion() },
                            enabled  = uiState.geminiState !is GeminiUiState.Loading,
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            if (uiState.geminiState is GeminiUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Estructurando...", style = MaterialTheme.typography.labelLarge)
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Estructurar con IA", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (uiState.geminiState is GeminiUiState.Error) {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Error: ${(uiState.geminiState as GeminiUiState.Error).mensaje}",
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Observación estructurada
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Observación estructurada", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (badgePatron != null) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(4.dp)) {
                                Text(badgePatron, style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape  = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Text(uiState.observacion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 60.dp))
                            TextButton(onClick = {}, modifier = Modifier.align(Alignment.TopEnd)) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Editar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 5. Suministros
                Column {
                    Text("Suministros / Accesorios", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded         = suministrosExpanded,
                        onExpandedChange = { suministrosExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = if (uiState.suministrosSeleccionados.isEmpty()) "Ninguno"
                                            else "${uiState.suministrosSeleccionados.size} seleccionado(s)",
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Agregar suministro") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = suministrosExpanded) },
                            modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded         = suministrosExpanded,
                            onDismissRequest = { suministrosExpanded = false }
                        ) {
                            uiState.suministrosCatalogo.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (item.id in uiState.suministrosSeleccionados) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(item.nombre)
                                        }
                                    },
                                    onClick = { viewModel.onSuministroToggle(item.id) }
                                )
                            }
                        }
                    }
                    if (uiState.suministrosSeleccionados.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            uiState.suministrosCatalogo
                                .filter { it.id in uiState.suministrosSeleccionados }
                                .forEach { item ->
                                    InputChip(
                                        selected = true,
                                        onClick  = { viewModel.onSuministroToggle(item.id) },
                                        label    = { Text(item.nombre, style = MaterialTheme.typography.labelSmall) },
                                        colors   = InputChipDefaults.inputChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                                    )
                                }
                        }
                    }
                }

                // 6. Seguimiento
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Requiere seguimiento", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(
                            checked         = uiState.requiereSeguimiento,
                            onCheckedChange = viewModel::onSeguimientoToggle,
                            colors          = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    if (uiState.requiereSeguimiento) {
                        Spacer(Modifier.height(8.dp))
                        Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value         = uiState.fechaSeguimiento,
                                    onValueChange = viewModel::onFechaSeguimientoChange,
                                    label         = { Text("Fecha de seguimiento") },
                                    placeholder   = { Text("AAAA-MM-DD") },
                                    trailingIcon  = { Icon(Icons.Default.CalendarToday, null) },
                                    singleLine    = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier      = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value         = uiState.notaSeguimiento,
                                    onValueChange = viewModel::onNotaSeguimientoChange,
                                    label         = { Text("Nota de seguimiento") },
                                    minLines      = 2,
                                    maxLines      = 3,
                                    modifier      = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 7. Fotos
                Column {
                    Text("Fotos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    PhotoGrid(photoCount = uiState.photoCount, onAddPhoto = viewModel::onAddPhoto)
                }

                // Error guardar
                if (uiState.errorGuardar != null) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            uiState.errorGuardar!!,
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                }

                // 8. Botón guardar
                Button(
                    onClick  = { viewModel.guardarVisita() },
                    enabled  = !uiState.guardando,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (uiState.requiereSeguimiento) "Guardar + crear recordatorio" else "Guardar registro",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
