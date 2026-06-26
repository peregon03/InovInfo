package com.Electroinova.inovinfo.ui.screens.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.Electroinova.inovinfo.ui.components.SectionCard

private data class Unidad(val numero: String, val sede: String)

private val UNIDADES = listOf(
    Unidad("3041",  "Buga"),
    Unidad("8013",  "Tuluá"),
    Unidad("8000",  "Tuluá"),
    Unidad("42004", "Buga"),
    Unidad("8014",  "Sur Occidente"),
    Unidad("16016", "Buga"),
)

private data class Tecnico(val nombre: String, val rol: String)

private val TECNICOS = listOf(
    Tecnico("Juan Arango",    "Técnico"),
    Tecnico("Carlos Muñoz",   "Técnico"),
    Tecnico("María González", "Coordinadora"),
)

@Composable
fun AdminScreen() {
    var fechaDesde by remember { mutableStateOf("") }
    var fechaHasta by remember { mutableStateOf("") }

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
                    imageVector        = Icons.Default.Settings,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text  = "Administración",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text  = "Solo coordinadora",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ── Contenido ────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Maestro de Unidades
            SectionCard(title = "Maestro de Unidades", icon = Icons.Default.Storage) {
                Column {
                    // Encabezado tabla
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text     = "# Unidad",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text     = "Sede",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text  = "Acciones",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    UNIDADES.forEachIndexed { idx, unidad ->
                        UnidadRow(unidad = unidad)
                        if (idx < UNIDADES.lastIndex) {
                            Divider(
                                color     = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick  = {},
                        modifier = Modifier.align(Alignment.End),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nueva unidad", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 2. Exportar informe
            SectionCard(title = "Exportar informe", icon = Icons.Default.TableChart) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value         = fechaDesde,
                            onValueChange = { fechaDesde = it },
                            label         = { Text("Desde") },
                            placeholder   = { Text("dd/mm/aaaa") },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value         = fechaHasta,
                            onValueChange = { fechaHasta = it },
                            label         = { Text("Hasta") },
                            placeholder   = { Text("dd/mm/aaaa") },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick  = {},
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector        = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier           = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar a Google Sheets")
                    }
                    OutlinedButton(
                        onClick  = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector        = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier           = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Ver Drive")
                    }
                }
            }

            // 3. Técnicos
            SectionCard(title = "Técnicos", icon = Icons.Default.Person) {
                Column {
                    TECNICOS.forEach { tecnico ->
                        TecnicoRow(tecnico = tecnico)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick  = {},
                        modifier = Modifier.align(Alignment.End),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar técnico", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UnidadRow(unidad: Unidad) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = unidad.numero,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text     = unidad.sede,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Row {
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector        = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint               = MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TecnicoRow(tecnico: Tecnico) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Default.Person,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = tecnico.nombre,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        val (chipBg, chipFg) = if (tecnico.rol == "Coordinadora")
            Color(0xFF1565C0) to Color.White
        else
            Color(0xFF4CAF50) to Color.White
        Surface(
            color = chipBg,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text     = tecnico.rol,
                style    = MaterialTheme.typography.labelSmall,
                color    = chipFg,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
