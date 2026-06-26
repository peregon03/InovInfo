package com.Electroinova.inovinfo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class UrgenciaStyle(
    val icon:     ImageVector,
    val iconTint: Color,
    val badgeBg:  Color,
    val badgeFg:  Color
)

private fun urgenciaStyle(urgencia: String) = when (urgencia) {
    "vencido" -> UrgenciaStyle(
        icon     = Icons.Default.Warning,
        iconTint = Color(0xFFE24B4A),
        badgeBg  = Color(0xFFE24B4A),
        badgeFg  = Color.White
    )
    "hoy" -> UrgenciaStyle(
        icon     = Icons.Default.Notifications,
        iconTint = Color(0xFFF9A825),
        badgeBg  = Color(0xFFF9A825),
        badgeFg  = Color(0xFF1A1A1A)
    )
    else -> UrgenciaStyle(
        icon     = Icons.Default.Schedule,
        iconTint = Color(0xFF757575),
        badgeBg  = Color(0xFFBDBDBD),
        badgeFg  = Color(0xFF1A1A1A)
    )
}

@Composable
fun PendingCard(
    unidad:     String,
    sede:       String,
    urgencia:   String,
    fechaLabel: String,
    nota:       String,
    onResolver: () -> Unit = {},
    modifier:   Modifier = Modifier
) {
    val style = urgenciaStyle(urgencia)

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = style.icon,
                    contentDescription = urgencia,
                    tint               = style.iconTint,
                    modifier           = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "Unidad $unidad · $sede",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text     = nota,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label   = { Text(fechaLabel, style = MaterialTheme.typography.labelSmall) },
                    colors  = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = style.badgeBg,
                        labelColor     = style.badgeFg
                    ),
                    border  = null
                )
                Button(
                    onClick = onResolver,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Resolver", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
