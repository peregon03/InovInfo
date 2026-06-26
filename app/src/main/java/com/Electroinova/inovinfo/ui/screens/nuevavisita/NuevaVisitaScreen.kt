package com.Electroinova.inovinfo.ui.screens.nuevavisita

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.Electroinova.inovinfo.ui.components.PhotoGrid
import com.Electroinova.inovinfo.ui.components.estadoColor

private val ESTADOS = listOf("OK", "Con novedad", "Inoperativa", "Sin llaves", "Bat. desconectada")

private val SUMINISTROS = listOf(
    "Sum. Accesorios general",
    "Suministro e Instalación Visor 7\"",
    "Cable de cámara 2.5m",
    "Cable de cámara 1.5m",
    "Conectores tipo avión 4 pines",
    "Fusibles"
)

private fun observacionTexto(estado: String, novedad: String): String = when {
    estado == "OK" ->
        "Unidad en óptimas condiciones de operación. Sistema DVR Móvil funcionando correctamente. " +
        "Cámaras operativas, GPS activo, batería en rango normal. Sin novedades."
    novedad.isNotBlank() ->
        "Se evidencia novedad en la unidad. ${novedad.take(80).trimEnd()}. " +
        "Se realizó inspección general del sistema DVR Móvil. " +
        "Se recomienda revisión técnica especializada."
    else ->
        "Unidad presenta condición anormal. Se requiere inspección adicional por parte del equipo técnico."
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NuevaVisitaScreen() {
    var numeroUnidad          by remember { mutableStateOf("") }
    var estadoSeleccionado    by remember { mutableStateOf("OK") }
    var novedadTecnico        by remember { mutableStateOf("") }
    var suministrosExpanded   by remember { mutableStateOf(false) }
    var selectedSuministros   by remember { mutableStateOf(emptySet<String>()) }
    var requiereSeguimiento   by remember { mutableStateOf(false) }
    var fechaSeguimiento      by remember { mutableStateOf("") }
    var notaSeguimiento       by remember { mutableStateOf("") }
    var photoCount            by remember { mutableStateOf(2) }

    val mostrarNovedad = estadoSeleccionado != "OK" && estadoSeleccionado != "Inoperativa"
    val observacion    = observacionTexto(estadoSeleccionado, novedadTecnico)
    val badgePatron    = when {
        estadoSeleccionado == "OK"   -> "Patrón estándar"
        novedadTecnico.isNotBlank()  -> "Redactado por IA ✦"
        else                          -> null
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── TopBar ───────────────────────────────────────────────────────────
        Surface(
            color     = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "Nueva Visita",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text  = "22 jun 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Box(
                    modifier         = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "JA",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }

        // ── Formulario ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. N.º Unidad
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = numeroUnidad,
                    onValueChange = { numeroUnidad = it },
                    label         = { Text("N.º Unidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    modifier      = Modifier.weight(1f)
                )
                if (numeroUnidad.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color  = MaterialTheme.colorScheme.primary,
                        shape  = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text  = "Buga",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. Estado de la unidad
            Column {
                Text(
                    text  = "Estado de la unidad",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ESTADOS.forEach { estado ->
                        val (bgSel, fgSel) = estadoColor(estado)
                        FilterChip(
                            selected = estadoSeleccionado == estado,
                            onClick  = { estadoSeleccionado = estado },
                            label    = { Text(estado, style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = bgSel,
                                selectedLabelColor     = fgSel
                            )
                        )
                    }
                }
            }

            // 3. Novedad del técnico (condicional)
            if (mostrarNovedad) {
                OutlinedTextField(
                    value         = novedadTecnico,
                    onValueChange = { novedadTecnico = it },
                    label         = { Text("Novedad del técnico") },
                    placeholder   = { Text("Describe la novedad en tus palabras...") },
                    minLines      = 3,
                    maxLines      = 5,
                    trailingIcon  = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Mic, contentDescription = "Voz")
                        }
                    },
                    modifier      = Modifier.fillMaxWidth()
                )
            }

            // 4. Observación estructurada
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "Observación estructurada",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (badgePatron != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF1565C0),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text     = badgePatron,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape  = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text  = observacion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick  = {},
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Edit,
                                contentDescription = null,
                                modifier           = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text("Editar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 5. Suministros / Accesorios
            Column {
                Text(
                    text  = "Suministros / Accesorios",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded        = suministrosExpanded,
                    onExpandedChange = { suministrosExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = if (selectedSuministros.isEmpty()) "Ninguno"
                                        else "${selectedSuministros.size} seleccionado(s)",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Agregar suministro") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = suministrosExpanded) },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded        = suministrosExpanded,
                        onDismissRequest = { suministrosExpanded = false }
                    ) {
                        SUMINISTROS.forEach { item ->
                            DropdownMenuItem(
                                text    = { Text(item) },
                                onClick = {
                                    selectedSuministros = if (item in selectedSuministros)
                                        selectedSuministros - item
                                    else
                                        selectedSuministros + item
                                }
                            )
                        }
                    }
                }
                if (selectedSuministros.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedSuministros.forEach { item ->
                            InputChip(
                                selected        = true,
                                onClick         = { selectedSuministros = selectedSuministros - item },
                                label           = { Text(item, style = MaterialTheme.typography.labelSmall) },
                                colors          = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // 6. Requiere seguimiento
            Column {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.Notifications,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = "Requiere seguimiento",
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked         = requiereSeguimiento,
                        onCheckedChange = { requiereSeguimiento = it },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor  = MaterialTheme.colorScheme.primary,
                            checkedTrackColor  = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                if (requiereSeguimiento) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier            = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value         = fechaSeguimiento,
                                onValueChange = { fechaSeguimiento = it },
                                label         = { Text("Fecha de seguimiento") },
                                placeholder   = { Text("dd/mm/aaaa") },
                                trailingIcon  = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                                },
                                singleLine    = true,
                                modifier      = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value         = notaSeguimiento,
                                onValueChange = { notaSeguimiento = it },
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
                Text(
                    text  = "Fotos",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                PhotoGrid(
                    photoCount = photoCount,
                    onAddPhoto = { if (photoCount < 8) photoCount++ }
                )
            }

            // 8. Botón guardar
            Button(
                onClick  = {},
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector        = Icons.Default.Save,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = if (requiereSeguimiento) "Guardar + crear recordatorio"
                            else "Guardar registro",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
