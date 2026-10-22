package com.Electroinova.inovinfo.ui.screens.nuevavisita

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Electroinova.inovinfo.data.remote.EstadoDto
import com.Electroinova.inovinfo.ui.components.PhotoGrid
import com.Electroinova.inovinfo.ui.components.estadoColor
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Crea un URI temporal en caché para la foto de cámara usando FileProvider. */
private fun crearUriTemporal(context: Context): Uri {
    val archivo = File(context.cacheDir, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NuevaVisitaScreen(
    onNavigateToPerfil: () -> Unit = {},
    viewModel: NuevaVisitaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var unidadExpanded            by remember { mutableStateOf(false) }
    var suministrosExpanded       by remember { mutableStateOf(false) }
    var mostrarDialogoObservacion by remember { mutableStateOf(false) }
    var mostrarDialogoFoto        by remember { mutableStateOf(false) }

    // URI temporal para la captura con cámara (se crea antes de lanzar el intent)
    var uriCamaraTemporal by remember { mutableStateOf<Uri?>(null) }

    // Launcher: galería de imágenes
    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.onFotoAgregada(it) } }

    // Launcher: cámara (devuelve true si se guardó la foto en uriCamaraTemporal)
    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) uriCamaraTemporal?.let { viewModel.onFotoAgregada(it) }
    }

    // Helper para abrir cámara (crea URI temporal y lanza)
    val lanzarCamara = {
        val uri = crearUriTemporal(context)
        uriCamaraTemporal = uri
        camaraLauncher.launch(uri)
    }

    // Launcher: solicitar permiso de cámara en runtime
    val permisosCamaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) lanzarCamara()
    }

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

                // Progreso del día
                if (uiState.sedeDetectada != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Sede: ${uiState.sedeDetectada}",
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${uiState.unidadesRevisadas.size}/${uiState.unidadesSede.size}",
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (uiState.unidadesSede.isNotEmpty())
                                        uiState.unidadesRevisadas.size.toFloat() / uiState.unidadesSede.size
                                    else 0f
                                },
                                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color      = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(8.dp))
                            // Chips: verde si ya fue revisada, gris si pendiente
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement   = Arrangement.spacedBy(4.dp)
                            ) {
                                uiState.unidadesSede.forEach { unidad ->
                                    val revisada = unidad.numero in uiState.unidadesRevisadas
                                    Surface(
                                        shape    = RoundedCornerShape(6.dp),
                                        color    = if (revisada) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                   else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { viewModel.onUnidadSelect(unidad) }
                                    ) {
                                        Text(
                                            unidad.numero,
                                            style      = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (revisada) FontWeight.Bold else FontWeight.Normal,
                                            color      = if (revisada) Color(0xFF2E7D32)
                                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Seleccionar unidad
                val sedesUnicas = remember(uiState.unidades) {
                    uiState.unidades.map { it.sede_id to it.sede_nombre }.distinctBy { it.first }
                }
                val unidadesFiltradas = remember(uiState.busquedaUnidad, uiState.filtroSedeId, uiState.unidades) {
                    viewModel.unidadesFiltradas()
                }

                // Filtro por sede
                if (sedesUnicas.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.filtroSedeId == null,
                                onClick  = { viewModel.onFiltroSedeChange(null) },
                                label    = { Text("Todas", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        items(sedesUnicas) { (id, nombre) ->
                            FilterChip(
                                selected = uiState.filtroSedeId == id,
                                onClick  = { viewModel.onFiltroSedeChange(id) },
                                label    = { Text(nombre, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded         = unidadExpanded,
                    onExpandedChange = { unidadExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = if (unidadExpanded) uiState.busquedaUnidad
                                        else uiState.unidadSeleccionada?.let { "Unidad ${it.numero} · ${it.sede_nombre}" } ?: "",
                        onValueChange = { viewModel.onBusquedaUnidadChange(it) },
                        label         = { Text("Unidad") },
                        placeholder   = { Text("Buscar por número o sede...") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unidadExpanded) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded         = unidadExpanded,
                        onDismissRequest = { unidadExpanded = false }
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
                                    onClick = { viewModel.onUnidadSelect(unidad); unidadExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Recordatorio de seguimiento pendiente
                if (uiState.seguimientoPendiente != null) {
                    val pend = uiState.seguimientoPendiente!!
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint     = Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Seguimiento pendiente",
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color(0xFFE65100)
                                )
                                Text(
                                    "Desde: ${pend.fecha_seguimiento}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFBF360C)
                                )
                                if (!pend.nota_seguimiento.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        pend.nota_seguimiento,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
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
                            TextButton(onClick = { mostrarDialogoObservacion = true }, modifier = Modifier.align(Alignment.TopEnd)) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Fotos (${uiState.fotos.size}/8)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.subiendoFotos) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                uiState.progresoFotos,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    PhotoGrid(
                        fotos          = uiState.fotos,
                        onAgregarFoto  = { mostrarDialogoFoto = true },
                        onEliminarFoto = viewModel::onFotoEliminada
                    )
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
                        when {
                            uiState.progresoFotos.isNotBlank() -> uiState.progresoFotos
                            uiState.guardando                  -> "Guardando..."
                            uiState.requiereSeguimiento        -> "Guardar + crear recordatorio"
                            else                               -> "Guardar registro"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Diálogo: Origen de foto (cámara o galería) ───────────────────────────
    if (mostrarDialogoFoto) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoFoto = false },
            title   = { Text("Agregar foto", fontWeight = FontWeight.SemiBold) },
            text    = { Text("Elige de dónde tomar la foto") },
            confirmButton = {
                Button(onClick = {
                    mostrarDialogoFoto = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        lanzarCamara()
                    } else {
                        permisosCamaraLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    mostrarDialogoFoto = false
                    galeriaLauncher.launch("image/*")
                }) {
                    Icon(Icons.Default.Photo, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galería")
                }
            }
        )
    }

    // ── Diálogo: Editar observación ──────────────────────────────────────────
    if (mostrarDialogoObservacion) {
        EditarObservacionDialog(
            textoInicial = uiState.observacion,
            onDismiss    = { mostrarDialogoObservacion = false },
            onGuardar    = { nuevoTexto ->
                viewModel.onObservacionChange(nuevoTexto)
                mostrarDialogoObservacion = false
            }
        )
    }
}

@Composable
private fun EditarObservacionDialog(
    textoInicial: String,
    onDismiss:    () -> Unit,
    onGuardar:    (String) -> Unit
) {
    var texto by remember { mutableStateOf(textoInicial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar observación", fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value         = texto,
                onValueChange = { texto = it },
                label         = { Text("Observación estructurada") },
                minLines      = 5,
                maxLines      = 10,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick  = { onGuardar(texto) },
                enabled  = texto.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
