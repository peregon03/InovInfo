package com.Electroinova.inovinfo.ui.screens.pendientes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Electroinova.inovinfo.ui.components.PendingCard

private data class Pendiente(
    val unidad:     String,
    val sede:       String,
    val urgencia:   String,
    val fechaLabel: String,
    val nota:       String
)

private val PENDIENTES = listOf(
    Pendiente("8007",  "Buga",          "vencido", "Vencido hace 2 meses",  "Baterías desconectadas sin solución"),
    Pendiente("3023",  "Tuluá",         "vencido", "Vencido hace 4 meses",  "Unidad sin movimiento desde diciembre"),
    Pendiente("16016", "Buga",          "hoy",     "Hoy",                   "Cámara lateral + GPS pendiente de instalación"),
    Pendiente("2237",  "Buga",          "futuro",  "4 jul 2026",            "Cambio cámaras lateral y reversa"),
)

private val FILTROS = listOf("Todos", "Vencidos", "Esta semana", "Resueltos")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendientesScreen() {
    var filtroActivo by remember { mutableStateOf("Todos") }
    val vencidos      = PENDIENTES.count { it.urgencia == "vencido" }

    val pendientesFiltrados = when (filtroActivo) {
        "Vencidos"     -> PENDIENTES.filter { it.urgencia == "vencido" }
        "Esta semana"  -> PENDIENTES.filter { it.urgencia == "hoy" }
        "Resueltos"    -> emptyList()
        else           -> PENDIENTES
    }

    val hayPendienteHoy = PENDIENTES.any { it.urgencia == "hoy" }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── TopBar ───────────────────────────────────────────────────────────
        Surface(
            color           = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    Icon(
                        imageVector        = Icons.Default.Notifications,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text  = "Pendientes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (vencidos > 0) {
                        Text(
                            text  = "$vencidos vencidos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF8A80)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Banner de alerta hoy
            if (hayPendienteHoy) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape  = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Row(
                            modifier          = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Notifications,
                                contentDescription = null,
                                tint               = Color(0xFFF9A825),
                                modifier           = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text  = "Visita programada hoy",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF795548)
                                )
                                Text(
                                    text  = "Unidad 16016 · Buga",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF795548)
                                )
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

            // Lista de pendientes
            items(pendientesFiltrados) { pendiente ->
                PendingCard(
                    unidad     = pendiente.unidad,
                    sede       = pendiente.sede,
                    urgencia   = pendiente.urgencia,
                    fechaLabel = pendiente.fechaLabel,
                    nota       = pendiente.nota
                )
            }

            if (pendientesFiltrados.isEmpty()) {
                item {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text  = "Sin pendientes en esta categoría",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
