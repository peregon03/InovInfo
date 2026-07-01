package com.Electroinova.inovinfo.ui.screens.pendientes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Electroinova.inovinfo.data.remote.PendienteDto
import com.Electroinova.inovinfo.ui.components.PendingCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FILTROS = listOf("Todos", "Vencidos", "Esta semana", "Futuros")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendientesScreen(
    viewModel: PendientesViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    var filtroActivo  by remember { mutableStateOf("Todos") }

    val vencidos = uiState.pendientes.count { it.urgencia == "vencido" }
    val hayHoy   = uiState.pendientes.any { it.urgencia == "hoy" }

    val pendientesFiltrados = when (filtroActivo) {
        "Vencidos"    -> uiState.pendientes.filter { it.urgencia == "vencido" }
        "Esta semana" -> uiState.pendientes.filter { it.urgencia == "hoy" }
        "Futuros"     -> uiState.pendientes.filter { it.urgencia == "futuro" }
        else          -> uiState.pendientes
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── TopBar ────────────────────────────────────────────────────────────
        Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgedBox(
                    badge = {
                        if (vencidos > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text(vencidos.toString(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pendientes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    if (vencidos > 0) {
                        Text("$vencidos vencidos", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF8A80))
                    }
                }
                IconButton(onClick = { viewModel.cargarPendientes() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                }
            }
        }

        // Cargando
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // Error
        if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.cargarPendientes() }) { Text("Reintentar") }
                }
            }
            return@Column
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Banner hoy
            if (hayHoy) {
                item {
                    Spacer(Modifier.height(12.dp))
                    val hoy = uiState.pendientes.filter { it.urgencia == "hoy" }
                    Card(
                        shape  = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, null, tint = Color(0xFFF9A825), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("${hoy.size} visita(s) programada(s) para hoy", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF795548))
                                hoy.take(2).forEach {
                                    Text("Unidad ${it.unidad_numero} · ${it.sede_nombre}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF795548))
                                }
                            }
                        }
                    }
                }
            } else {
                item { Spacer(Modifier.height(12.dp)) }
            }

            // Filtros
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FILTROS.forEach { filtro ->
                        FilterChip(
                            selected = filtroActivo == filtro,
                            onClick  = { filtroActivo = filtro },
                            label    = { Text(filtro, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Lista
            if (pendientesFiltrados.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text("Sin pendientes en esta categoría", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(pendientesFiltrados) { pendiente ->
                    PendingCard(
                        unidad     = pendiente.unidad_numero,
                        sede       = pendiente.sede_nombre,
                        urgencia   = pendiente.urgencia,
                        fechaLabel = formatearFecha(pendiente),
                        nota       = pendiente.nota_seguimiento ?: "Sin nota"
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private fun formatearFecha(pendiente: PendienteDto): String {
    return try {
        val fecha = LocalDate.parse(pendiente.fecha_seguimiento.take(10))
        val hoy   = LocalDate.now()
        when {
            fecha.isBefore(hoy) -> {
                val dias = java.time.temporal.ChronoUnit.DAYS.between(fecha, hoy)
                "Vencido hace $dias día(s)"
            }
            fecha.isEqual(hoy) -> "Hoy"
            else -> fecha.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "CO")))
        }
    } catch (e: Exception) {
        pendiente.fecha_seguimiento
    }
}
