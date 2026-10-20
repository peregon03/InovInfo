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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Electroinova.inovinfo.ui.components.SectionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Date picker states
    var showDesdeDialog by remember { mutableStateOf(false) }
    var showHastaDialog by remember { mutableStateOf(false) }

    // Dropdown sede
    var sedeExpanded by remember { mutableStateOf(false) }

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
                                modifier = Modifier.size(18.dp),
                                color    = Color.White,
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

                    // Error
                    AnimatedVisibility(visible = state.error != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
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
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint     = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text  = "Informe generado",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Text(
                                    text  = "Pestaña: ${state.pestana}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text  = "${state.totalVisitas} visitas exportadas",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                // Botón abrir en Sheets
                                OutlinedButton(
                                    onClick = {
                                        state.sheetUrl?.let { url ->
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        modifier           = Modifier.size(16.dp)
                                    )
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

    // ── DatePicker Dialogs ───────────────────────────────────────────────────

    if (showDesdeDialog) {
        DatePickerModal(
            onDateSelected = { millis ->
                millis?.let {
                    viewModel.onFechaDesdeChanged(millisToApiDate(it))
                }
                showDesdeDialog = false
            },
            onDismiss = { showDesdeDialog = false }
        )
    }

    if (showHastaDialog) {
        DatePickerModal(
            onDateSelected = { millis ->
                millis?.let {
                    viewModel.onFechaHastaChanged(millisToApiDate(it))
                }
                showHastaDialog = false
            },
            onDismiss = { showHastaDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onDateSelected(datePickerState.selectedDateMillis) }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

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
