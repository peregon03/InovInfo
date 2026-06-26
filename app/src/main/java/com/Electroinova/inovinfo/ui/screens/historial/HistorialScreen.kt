package com.Electroinova.inovinfo.ui.screens.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Electroinova.inovinfo.ui.components.VisitaListItem

private data class VisitaItem(
    val unidad:  String,
    val sede:    String,
    val estado:  String,
    val resumen: String,
    val fotos:   Int
)

private val VISITAS_HOY = listOf(
    VisitaItem("3041",  "Buga",          "OK",          "Sistema DVR funcionando correctamente.", 2),
    VisitaItem("8013",  "Tuluá",         "Con novedad", "Cámara frontal reventada.", 3),
    VisitaItem("8000",  "Tuluá",         "Inoperativa", "Unidad sin encender, sin respuesta.", 1),
    VisitaItem("42004", "Buga",          "OK",          "Todas las cámaras operativas.", 2),
    VisitaItem("8014",  "Sur Occidente", "Inoperativa", "Unidad apagada, sin batería.", 0),
)

@Composable
fun HistorialScreen() {
    val conServicio  = VISITAS_HOY.count { it.estado == "OK" }
    val inoperativas = VISITAS_HOY.count { it.estado == "Inoperativa" }

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
                Icon(
                    imageVector        = Icons.Default.List,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "Historial",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text  = "22 jun 2026 · ${VISITAS_HOY.size} registros",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector        = Icons.Default.FileDownload,
                        contentDescription = "Exportar",
                        tint               = Color.White
                    )
                }
            }
        }

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Resumen cards ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResumenCard(
                        label     = "Con servicio",
                        count     = conServicio,
                        bgColor   = Color(0xFFE8F5E9),
                        textColor = Color(0xFF2E7D32),
                        modifier  = Modifier.weight(1f)
                    )
                    ResumenCard(
                        label     = "Inoperativas",
                        count     = inoperativas,
                        bgColor   = Color(0xFFFFEBEE),
                        textColor = Color(0xFFC62828),
                        modifier  = Modifier.weight(1f)
                    )
                }
            }

            // ── Lista de visitas ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Registros del día",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(VISITAS_HOY) { visita ->
                VisitaListItem(
                    unidad  = visita.unidad,
                    sede    = visita.sede,
                    estado  = visita.estado,
                    resumen = visita.resumen,
                    fotos   = visita.fotos
                )
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
            modifier          = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}
