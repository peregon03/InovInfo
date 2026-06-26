package com.Electroinova.inovinfo.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

fun estadoColor(estado: String): Pair<Color, Color> = when (estado) {
    "OK"                -> Color(0xFF2E7D32) to Color.White
    "Con novedad"       -> Color(0xFFF9A825) to Color(0xFF1A1A1A)
    "Inoperativa"       -> Color(0xFFE24B4A) to Color.White
    "Sin llaves"        -> Color(0xFF757575) to Color.White
    "Bat. desconectada" -> Color(0xFF9E9E9E) to Color(0xFF1A1A1A)
    else                -> Color(0xFF9E9E9E) to Color(0xFF1A1A1A)
}

@Composable
fun StatusChip(
    estado: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = estadoColor(estado)
    AssistChip(
        onClick  = {},
        label    = { Text(estado, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        colors   = AssistChipDefaults.assistChipColors(
            containerColor = bg,
            labelColor     = fg
        ),
        border = null
    )
}
