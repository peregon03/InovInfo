package com.Electroinova.inovinfo.ui.screens.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Electroinova.inovinfo.data.remote.UnidadDto
import com.Electroinova.inovinfo.data.remote.VisitaCompletaDto
import com.Electroinova.inovinfo.ui.components.estadoColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        if (uiState.unidadSeleccionada == null) {
            // ── Estado A: modo búsqueda ────────────────────────────────────────
            BusquedaTopBar()
            BusquedaContent(
                uiState   = uiState,
                viewModel = viewModel
            )
        } else {
            // ── Estado B: historial de la unidad ──────────────────────────────
            val onRefresh = { viewModel.cargarHistorial(uiState.unidadSeleccionada!!.id) }
            HistorialTopBar(
                unidad    = uiState.unidadSeleccionada!!,
                onBack    = { viewModel.limpiarSeleccion() },
                onRefresh = onRefresh
            )
            HistorialContent(
                uiState   = uiState,
                viewModel = viewModel,
                onRefresh = onRefresh
            )
        }
    }
}

// ── TopBar: modo búsqueda ──────────────────────────────────────────────────────

@Composable
private fun BusquedaTopBar() {
    Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Consulta de Unidad",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

// ── TopBar: modo historial ─────────────────────────────────────────────────────

@Composable
private fun HistorialTopBar(unidad: UnidadDto, onBack: () -> Unit, onRefresh: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Unidad ${unidad.numero} · ${unidad.sede_nombre}",
                    style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color    = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Historial completo",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
            }
        }
    }
}

// ── Contenido: modo búsqueda ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusquedaContent(
    uiState:   HistorialUiState,
    viewModel: HistorialViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val unidadesFiltradas = viewModel.unidadesFiltradas()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cargando catálogo
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // Error al cargar catálogo
        if (uiState.error != null && uiState.unidades.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(uiState.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { /* retry handled in VM init; no-op here */ }) { Text("Reintentar") }
            }
            return@Column
        }

        // Buscador de unidades
        ExposedDropdownMenuBox(
            expanded         = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value         = if (expanded) uiState.busquedaUnidad
                                else uiState.unidadSeleccionada?.let { "Unidad ${it.numero} · ${it.sede_nombre}" } ?: "",
                onValueChange = { viewModel.onBusquedaChange(it) },
                label         = { Text("Unidad") },
                placeholder   = { Text("Buscar por número o sede...") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
            )
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (unidadesFiltradas.isEmpty()) {
                    DropdownMenuItem(
                        text    = { Text("Sin resultados", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = {}
                    )
                } else {
                    unidadesFiltradas.forEach { unidad ->
                        DropdownMenuItem(
                            text    = { Text("${unidad.numero} · ${unidad.sede_nombre}") },
                            onClick = {
                                viewModel.onUnidadSeleccionada(unidad)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Indicación de acción
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Selecciona una unidad para ver su historial",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Contenido: historial de visitas ───────────────────────────────────────────

@Composable
private fun HistorialContent(
    uiState:   HistorialUiState,
    viewModel: HistorialViewModel,
    onRefresh: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Box
        }

        if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(uiState.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRefresh) { Text("Reintentar") }
                }
            }
            return@Box
        }

        val totalSuministros = uiState.visitas.sumOf { v -> v.suministros.sumOf { it.cantidad } }
        val ultimaVisita = uiState.visitas.maxByOrNull { it.created_at }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Tarjeta de resumen ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                ResumenUnidadCard(
                    totalVisitas     = uiState.visitas.size,
                    ultimaVisita     = ultimaVisita,
                    totalSuministros = totalSuministros
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = if (uiState.visitas.isEmpty()) "Sin visitas registradas" else "Visitas registradas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(uiState.visitas) { visita ->
                VisitaHistorialCard(
                    visita          = visita,
                    esCoordinadora  = uiState.esCoordinadora,
                    onEliminar      = { viewModel.eliminarVisita(it) }
                )
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ── Tarjeta resumen de la unidad ──────────────────────────────────────────────

@Composable
private fun ResumenUnidadCard(
    totalVisitas:     Int,
    ultimaVisita:     VisitaCompletaDto?,
    totalSuministros: Int
) {
    val fechaUltima = ultimaVisita?.let { formatearFecha(it.created_at) } ?: "–"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ResumenItem(valor = totalVisitas.toString(),     etiqueta = "Visitas")
            VerticalDivider(modifier = Modifier.height(40.dp))
            ResumenItem(valor = fechaUltima,                etiqueta = "Última visita")
            VerticalDivider(modifier = Modifier.height(40.dp))
            ResumenItem(valor = totalSuministros.toString(), etiqueta = "Suministros")
        }
    }
}

@Composable
private fun ResumenItem(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor,    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Tarjeta de una visita ──────────────────────────────────────────────────────

@Composable
private fun VisitaHistorialCard(
    visita:         VisitaCompletaDto,
    esCoordinadora: Boolean = false,
    onEliminar:     (Int) -> Unit = {}
) {
    var expandido        by remember { mutableStateOf(false) }
    var mostrarDialogo   by remember { mutableStateOf(false) }
    val (bgEstado, fgEstado) = estadoColor(visita.estado)

    // Diálogo de confirmación de eliminación
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title            = { Text("Eliminar visita") },
            text             = { Text("¿Eliminar esta visita? Esta acción no se puede deshacer.") },
            confirmButton    = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    onEliminar(visita.id)
                }) { Text("Confirmar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton    = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { expandido = !expandido },
        shape     = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // Fila superior: fecha + chip de estado + botón eliminar (solo coordinadora)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    formatearFecha(visita.created_at),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = bgEstado
                    ) {
                        Text(
                            visita.estado,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = fgEstado,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (esCoordinadora) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick  = { mostrarDialogo = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar visita",
                                tint               = Color(0xFFD32F2F),
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Técnico
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier           = Modifier.size(14.dp),
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    visita.tecnico_nombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Observación (truncada o expandida)
            val observacion = visita.observacion_estructurada ?: visita.novedad_texto
            if (!observacion.isNullOrBlank()) {
                Text(
                    text     = observacion,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = if (expandido) Int.MAX_VALUE else 2,
                    overflow = if (expandido) TextOverflow.Visible else TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurface
                )
            }

            // Suministros
            if (visita.suministros.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier           = Modifier.size(14.dp),
                        tint               = MaterialTheme.colorScheme.primary
                    )
                    visita.suministros.forEach { sum ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "Sum ${sum.nombre} ×${sum.cantidad}",
                                style    = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Fotos
            if (visita.fotos.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier           = Modifier.size(14.dp),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${visita.fotos.size} foto${if (visita.fotos.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Seguimiento
            if (visita.requiere_seguimiento && visita.fecha_seguimiento != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier           = Modifier.size(12.dp),
                                tint               = Color(0xFFE65100)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Seguimiento: ${formatearFecha(visita.fecha_seguimiento)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
                if (!visita.nota_seguimiento.isNullOrBlank()) {
                    Text(
                        visita.nota_seguimiento,
                        style  = MaterialTheme.typography.bodySmall,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Formatea un timestamp ISO a "dd/MM/yyyy". */
private fun formatearFecha(iso: String): String = try {
    val dt = LocalDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME)
    dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "CO")))
} catch (_: Exception) { iso }
