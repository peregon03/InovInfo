package com.Electroinova.inovinfo.ui.screens.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Electroinova.inovinfo.ui.components.VisitaListItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val hoy = LocalDate.now().format(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "CO"))
    )

    val conServicio  = uiState.visitas.count { it.estado == "OK" }
    val inoperativas = uiState.visitas.count { it.estado == "Inoperativa" }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── TopBar ────────────────────────────────────────────────────────────
        Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.List, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Historial",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        "$hoy · ${uiState.visitas.size} registros",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { viewModel.cargarVisitas() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                }
            }
        }

        // ── Cargando ──────────────────────────────────────────────────────────
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // ── Error ─────────────────────────────────────────────────────────────
        if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(uiState.error!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.cargarVisitas() }) { Text("Reintentar") }
                }
            }
            return@Column
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Resumen del día
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResumenCard("Con servicio", conServicio, Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                    ResumenCard("Inoperativas", inoperativas, Color(0xFFFFEBEE), Color(0xFFC62828), Modifier.weight(1f))
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Registros del día",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.visitas.isEmpty()) {
                item {
                    Box(
                        modifier            = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment    = Alignment.Center
                    ) {
                        Text(
                            "Sin registros hoy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.visitas) { visita ->
                    VisitaListItem(
                        unidad  = visita.unidad_numero,
                        sede    = visita.sede_nombre,
                        estado  = visita.estado,
                        resumen = visita.observacion_estructurada ?: "Sin observación",
                        fotos   = visita.fotos_count
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ResumenCard(
    label:     String,
    count:     Int,
    bgColor:   Color,
    textColor: Color,
    modifier:  Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
            Text(label, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f))
        }
    }
}
