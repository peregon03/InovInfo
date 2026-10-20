package com.Electroinova.inovinfo.ui.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Electroinova.inovinfo.data.remote.SedeDto
import com.Electroinova.inovinfo.data.remote.SuministroDto
import com.Electroinova.inovinfo.data.remote.UnidadDto
import com.Electroinova.inovinfo.ui.components.SectionCard
import com.Electroinova.inovinfo.ui.theme.ElectroNavyBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state   by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Launcher para selección de archivo Excel
    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importarExcel(it) } }

    // DatePicker states
    var showDesdeDialog by remember { mutableStateOf(false) }
    var showHastaDialog by remember { mutableStateOf(false) }

    // Diálogos de sedes
    var showCrearSedeDialog by remember { mutableStateOf(false) }
    var sedeParaEditar      by remember { mutableStateOf<SedeDto?>(null) }

    // Diálogos de unidades
    var showCrearUnidadDialog  by remember { mutableStateOf(false) }
    var unidadParaEditar       by remember { mutableStateOf<UnidadDto?>(null) }
    var unidadParaEliminar     by remember { mutableStateOf<UnidadDto?>(null) }

    // Diálogos de suministros
    var showCrearSuministroDialog by remember { mutableStateOf(false) }
    var suministroParaEditar      by remember { mutableStateOf<SuministroDto?>(null) }

    // Filtro de búsqueda de unidades
    var filtroUnidades by remember { mutableStateOf("") }

    // Dropdown sede (filtro informe)
    var sedeExpanded by remember { mutableStateOf(false) }

    // Auto-limpiar mensaje de éxito
    if (state.mensajeExito != null) {
        LaunchedEffect(state.mensajeExito) {
            kotlinx.coroutines.delay(3000)
            viewModel.limpiarMensaje()
        }
    }

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

            // ── Mensajes éxito/error de gestión ─────────────────────────────
            AnimatedVisibility(visible = state.mensajeExito != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.mensajeExito ?: "", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null && !state.generando) {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = state.error ?: "",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Gestión de sedes ─────────────────────────────────────────────
            SectionCard(title = "Bases operativas (sedes)", icon = Icons.Default.LocationOn) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Sedes registradas",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.sedesLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { viewModel.cargarSedes() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    if (state.sedes.isEmpty() && !state.sedesLoading) {
                        Text(
                            "Sin sedes registradas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.sedes.forEach { sede ->
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sede.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick  = { sedeParaEditar = sede },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar sede", modifier = Modifier.size(16.dp), tint = ElectroNavyBlue)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick  = { showCrearSedeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar sede")
                    }
                }
            }

            // ── Flota de unidades ─────────────────────────────────────────────
            SectionCard(title = "Flota de unidades", icon = Icons.Default.DirectionsCar) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Cabecera con contador y refresh
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${state.unidades.size} unidades registradas",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.unidadesLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { viewModel.cargarUnidades() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Barra de búsqueda
                    OutlinedTextField(
                        value         = filtroUnidades,
                        onValueChange = { filtroUnidades = it },
                        label         = { Text("Buscar unidad") },
                        leadingIcon   = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )

                    // Lista de unidades filtradas
                    val unidadesFiltradas = state.unidades.filter { u ->
                        filtroUnidades.isBlank() ||
                            u.numero.contains(filtroUnidades, ignoreCase = true) ||
                            u.sede_nombre.contains(filtroUnidades, ignoreCase = true)
                    }

                    if (unidadesFiltradas.isEmpty() && !state.unidadesLoading) {
                        Text(
                            if (filtroUnidades.isBlank()) "Sin unidades registradas" else "Sin resultados para \"$filtroUnidades\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        unidadesFiltradas.forEach { u ->
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Unidad ${u.numero}",
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    // Chip con nombre de sede
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ElectroNavyBlue.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            u.sede_nombre,
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = ElectroNavyBlue,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick  = { unidadParaEditar = u },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Editar unidad", modifier = Modifier.size(16.dp), tint = ElectroNavyBlue)
                                }
                                IconButton(
                                    onClick  = { unidadParaEliminar = u },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Eliminar unidad", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick  = { showCrearUnidadDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
                    ) {
                        if (state.crearUnidadLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar unidad")
                    }
                }
            }

            // ── Suministros / Accesorios ──────────────────────────────────────
            SectionCard(title = "Suministros / Accesorios", icon = Icons.Default.Inventory) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${state.suministros.size} suministros registrados",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.suministrosLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { viewModel.cargarSuministros() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (state.suministros.isEmpty() && !state.suministrosLoading) {
                        Text(
                            "Sin suministros registrados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.suministros.forEach { s ->
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        s.nombre,
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines   = 1,
                                        overflow   = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        // Badge de categoría
                                        if (!s.categoria.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    s.categoria,
                                                    style    = MaterialTheme.typography.labelSmall,
                                                    color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        // Precio de referencia
                                        if (s.precio_ref != null) {
                                            Text(
                                                "$%.0f".format(s.precio_ref),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick  = { suministroParaEditar = s },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Editar suministro", modifier = Modifier.size(16.dp), tint = ElectroNavyBlue)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick  = { showCrearSuministroDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar suministro")
                    }
                }
            }

            // ── Importar desde Excel ─────────────────────────────────────────
            SectionCard(title = "Importar desde Excel", icon = Icons.Default.UploadFile) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(
                        text  = "Sube un archivo Excel (.xlsx) con columnas: sede, unidad. La primera fila es encabezado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick  = {
                            excelLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        },
                        enabled  = !state.importando,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
                    ) {
                        if (state.importando) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Importando...")
                        } else {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Importar Excel")
                        }
                    }

                    // Resultado de la importación
                    AnimatedVisibility(visible = state.resultadoImportacion != null && !state.importando) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.resultadoImportacion ?: "",
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // ── Exportar informe a Google Sheets ────────────────────────────
            SectionCard(title = "Exportar informe", icon = Icons.Default.TableChart) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Selector de sede (opcional)
                    ExposedDropdownMenuBox(
                        expanded         = sedeExpanded,
                        onExpandedChange = { sedeExpanded = !sedeExpanded }
                    ) {
                        OutlinedTextField(
                            value         = state.sedeSeleccionada?.nombre ?: "Todas las sedes",
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Sede") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sedeExpanded) },
                            modifier      = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded         = sedeExpanded,
                            onDismissRequest = { sedeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Todas las sedes") },
                                onClick = {
                                    viewModel.onSedeSeleccionada(null)
                                    sedeExpanded = false
                                }
                            )
                            state.sedes.forEach { sede ->
                                DropdownMenuItem(
                                    text    = { Text(sede.nombre) },
                                    onClick = {
                                        viewModel.onSedeSeleccionada(sede)
                                        sedeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Fechas con DatePicker
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value         = formatDisplayDate(state.fechaDesde),
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Desde") },
                            placeholder   = { Text("Seleccionar") },
                            trailingIcon  = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Seleccionar fecha",
                                    modifier = Modifier.clickable { showDesdeDialog = true }
                                )
                            },
                            singleLine    = true,
                            modifier      = Modifier
                                .weight(1f)
                                .clickable { showDesdeDialog = true }
                        )
                        OutlinedTextField(
                            value         = formatDisplayDate(state.fechaHasta),
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Hasta") },
                            placeholder   = { Text("Seleccionar") },
                            trailingIcon  = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Seleccionar fecha",
                                    modifier = Modifier.clickable { showHastaDialog = true }
                                )
                            },
                            singleLine    = true,
                            modifier      = Modifier
                                .weight(1f)
                                .clickable { showHastaDialog = true }
                        )
                    }

                    // Botón generar
                    Button(
                        onClick  = { viewModel.generarInforme() },
                        enabled  = !state.generando && state.fechaDesde.isNotBlank() && state.fechaHasta.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.generando) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Generando informe...")
                        } else {
                            Icon(
                                imageVector        = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier           = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Exportar a Google Sheets")
                        }
                    }

                    // Error del informe
                    AnimatedVisibility(visible = state.error != null && state.generando) {
                        Card(
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text     = state.error ?: "",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Resultado exitoso
                    AnimatedVisibility(visible = state.exito && state.sheetUrl != null) {
                        Card(
                            colors   = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier            = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector        = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint               = Color(0xFF2E7D32),
                                        modifier           = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text  = "Informe generado",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Text(text = "Pestaña: ${state.pestana}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "${state.totalVisitas} visitas exportadas", style = MaterialTheme.typography.bodySmall)

                                OutlinedButton(
                                    onClick  = {
                                        state.sheetUrl?.let { url ->
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Abrir en Google Sheets")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // ── Diálogo: Crear sede ──────────────────────────────────────────────────
    if (showCrearSedeDialog) {
        CrearSedeDialog(
            isLoading = state.crearSedeLoading,
            onDismiss = { showCrearSedeDialog = false },
            onCrear   = { nombre ->
                viewModel.crearSede(nombre)
                showCrearSedeDialog = false
            }
        )
    }

    // ── Diálogo: Editar sede ─────────────────────────────────────────────────
    sedeParaEditar?.let { sede ->
        EditarSedeDialog(
            sede      = sede,
            onDismiss = { sedeParaEditar = null },
            onGuardar = { nuevoNombre ->
                viewModel.actualizarSede(sede.id, nuevoNombre)
                sedeParaEditar = null
            }
        )
    }

    // ── Diálogo: Crear unidad ────────────────────────────────────────────────
    if (showCrearUnidadDialog) {
        CrearUnidadDialog(
            sedes     = state.sedes,
            isLoading = state.crearUnidadLoading,
            onDismiss = { showCrearUnidadDialog = false },
            onCrear   = { numero, sedeId ->
                viewModel.crearUnidad(numero, sedeId)
                showCrearUnidadDialog = false
            }
        )
    }

    // ── Diálogo: Editar unidad ───────────────────────────────────────────────
    unidadParaEditar?.let { unidad ->
        EditarUnidadDialog(
            unidad    = unidad,
            sedes     = state.sedes,
            onDismiss = { unidadParaEditar = null },
            onGuardar = { numero, sedeId, activa ->
                viewModel.actualizarUnidad(unidad.id, numero, sedeId, activa)
                unidadParaEditar = null
            }
        )
    }

    // ── Diálogo: Confirmar eliminar unidad ───────────────────────────────────
    unidadParaEliminar?.let { unidad ->
        AlertDialog(
            onDismissRequest = { unidadParaEliminar = null },
            title            = { Text("Eliminar unidad", fontWeight = FontWeight.Bold) },
            text             = { Text("¿Confirmas que deseas desactivar la unidad ${unidad.numero} (${unidad.sede_nombre})?") },
            confirmButton    = {
                Button(
                    onClick = {
                        viewModel.eliminarUnidad(unidad.id)
                        unidadParaEliminar = null
                    },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton    = {
                TextButton(onClick = { unidadParaEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo: Crear suministro ────────────────────────────────────────────
    if (showCrearSuministroDialog) {
        CrearSuministroDialog(
            onDismiss = { showCrearSuministroDialog = false },
            onCrear   = { nombre, categoria, precio ->
                viewModel.crearSuministro(nombre, categoria, precio)
                showCrearSuministroDialog = false
            }
        )
    }

    // ── Diálogo: Editar suministro ───────────────────────────────────────────
    suministroParaEditar?.let { s ->
        EditarSuministroDialog(
            suministro = s,
            onDismiss  = { suministroParaEditar = null },
            onGuardar  = { nombre, categoria, precio, activo ->
                viewModel.actualizarSuministro(s.id, nombre, categoria, precio, activo)
                suministroParaEditar = null
            }
        )
    }

    // ── DatePicker Dialogs ───────────────────────────────────────────────────
    if (showDesdeDialog) {
        DatePickerModal(
            onDateSelected = { millis ->
                millis?.let { viewModel.onFechaDesdeChanged(millisToApiDate(it)) }
                showDesdeDialog = false
            },
            onDismiss = { showDesdeDialog = false }
        )
    }

    if (showHastaDialog) {
        DatePickerModal(
            onDateSelected = { millis ->
                millis?.let { viewModel.onFechaHastaChanged(millisToApiDate(it)) }
                showHastaDialog = false
            },
            onDismiss = { showHastaDialog = false }
        )
    }
}

// ── Diálogos de Sedes ────────────────────────────────────────────────────────

@Composable
private fun CrearSedeDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCrear:   (nombre: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Nueva sede", fontWeight = FontWeight.Bold) },
        text             = {
            OutlinedTextField(
                value         = nombre,
                onValueChange = { nombre = it },
                label         = { Text("Nombre de la sede") },
                placeholder   = { Text("Ej: Bogotá Norte") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton    = {
            Button(
                onClick  = { onCrear(nombre) },
                enabled  = !isLoading && nombre.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditarSedeDialog(
    sede:      SedeDto,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String) -> Unit
) {
    var nombre by remember { mutableStateOf(sede.nombre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Editar sede", fontWeight = FontWeight.Bold) },
        text             = {
            OutlinedTextField(
                value         = nombre,
                onValueChange = { nombre = it },
                label         = { Text("Nombre de la sede") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton    = {
            Button(
                onClick  = { onGuardar(nombre) },
                enabled  = nombre.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
            ) { Text("Guardar") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Diálogos de Unidades ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearUnidadDialog(
    sedes:     List<SedeDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCrear:   (numero: String, sedeId: Int) -> Unit
) {
    var numero        by remember { mutableStateOf("") }
    var sedeElegida   by remember { mutableStateOf(sedes.firstOrNull()) }
    var sedeExpanded  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Nueva unidad", fontWeight = FontWeight.Bold) },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = numero,
                    onValueChange = { numero = it },
                    label         = { Text("Número de unidad") },
                    placeholder   = { Text("Ej: A-01") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded         = sedeExpanded,
                    onExpandedChange = { sedeExpanded = !sedeExpanded }
                ) {
                    OutlinedTextField(
                        value         = sedeElegida?.nombre ?: "Seleccionar sede",
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
                                onClick = {
                                    sedeElegida  = sede
                                    sedeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton    = {
            Button(
                onClick  = { sedeElegida?.let { onCrear(numero, it.id) } },
                enabled  = !isLoading && numero.isNotBlank() && sedeElegida != null,
                colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarUnidadDialog(
    unidad:    UnidadDto,
    sedes:     List<SedeDto>,
    onDismiss: () -> Unit,
    onGuardar: (numero: String, sedeId: Int, activa: Boolean) -> Unit
) {
    var numero       by remember { mutableStateOf(unidad.numero) }
    var sedeElegida  by remember { mutableStateOf(sedes.find { it.id == unidad.sede_id } ?: sedes.firstOrNull()) }
    var activa       by remember { mutableStateOf(true) }
    var sedeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Editar unidad", fontWeight = FontWeight.Bold) },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = numero,
                    onValueChange = { numero = it },
                    label         = { Text("Número de unidad") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded         = sedeExpanded,
                    onExpandedChange = { sedeExpanded = !sedeExpanded }
                ) {
                    OutlinedTextField(
                        value         = sedeElegida?.nombre ?: "Seleccionar sede",
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
                                onClick = {
                                    sedeElegida  = sede
                                    sedeExpanded = false
                                }
                            )
                        }
                    }
                }
                // Toggle activa
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Unidad activa",
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = activa, onCheckedChange = { activa = it })
                }
            }
        },
        confirmButton    = {
            Button(
                onClick  = { sedeElegida?.let { onGuardar(numero, it.id, activa) } },
                enabled  = numero.isNotBlank() && sedeElegida != null,
                colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
            ) { Text("Guardar") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Diálogos de Suministros ──────────────────────────────────────────────────

@Composable
private fun CrearSuministroDialog(
    onDismiss: () -> Unit,
    onCrear:   (nombre: String, categoria: String?, precioRef: Double?) -> Unit
) {
    var nombre    by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var precio    by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Nuevo suministro", fontWeight = FontWeight.Bold) },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = nombre,
                    onValueChange = { nombre = it },
                    label         = { Text("Nombre *") },
                    placeholder   = { Text("Ej: Cámara domo 2MP") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = categoria,
                    onValueChange = { categoria = it },
                    label         = { Text("Categoría (opcional)") },
                    placeholder   = { Text("Ej: Cámara, Cable, Accesorio") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = precio,
                    onValueChange = { precio = it },
                    label         = { Text("Precio referencia (opcional)") },
                    placeholder   = { Text("Ej: 150000") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton    = {
            Button(
                onClick  = {
                    val precioRef = precio.toDoubleOrNull()
                    onCrear(nombre, categoria.takeIf { it.isNotBlank() }, precioRef)
                },
                enabled  = nombre.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
            ) { Text("Crear") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditarSuministroDialog(
    suministro: SuministroDto,
    onDismiss:  () -> Unit,
    onGuardar:  (nombre: String, categoria: String?, precioRef: Double?, activo: Boolean) -> Unit
) {
    var nombre    by remember { mutableStateOf(suministro.nombre) }
    var categoria by remember { mutableStateOf(suministro.categoria ?: "") }
    var precio    by remember { mutableStateOf(suministro.precio_ref?.let { "%.0f".format(it) } ?: "") }
    var activo    by remember { mutableStateOf(suministro.activo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Editar suministro", fontWeight = FontWeight.Bold) },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = nombre,
                    onValueChange = { nombre = it },
                    label         = { Text("Nombre *") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = categoria,
                    onValueChange = { categoria = it },
                    label         = { Text("Categoría (opcional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = precio,
                    onValueChange = { precio = it },
                    label         = { Text("Precio referencia (opcional)") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth()
                )
                // Toggle activo
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Suministro activo",
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = activo, onCheckedChange = { activo = it })
                }
            }
        },
        confirmButton    = {
            Button(
                onClick  = {
                    val precioRef = precio.toDoubleOrNull()
                    onGuardar(nombre, categoria.takeIf { it.isNotBlank() }, precioRef, activo)
                },
                enabled  = nombre.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
            ) { Text("Guardar") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── DatePicker ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss:      () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text("Aceptar")
            }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// ── Utilidades de fecha ──────────────────────────────────────────────────────

/**
 * Convierte millis UTC a formato "YYYY-MM-DD" para el API.
 */
private fun millisToApiDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
}

/**
 * Convierte "YYYY-MM-DD" a "dd/MM/yyyy" para mostrar al usuario.
 */
private fun formatDisplayDate(apiDate: String): String {
    if (apiDate.isBlank()) return ""
    return try {
        val parts = apiDate.split("-")
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } catch (_: Exception) {
        apiDate
    }
}
