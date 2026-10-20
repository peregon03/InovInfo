package com.Electroinova.inovinfo.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private const val MAX_FOTOS = 8

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoGrid(
    fotos:           List<Uri>,
    onAgregarFoto:   () -> Unit,
    onEliminarFoto:  (Int) -> Unit,
    modifier:        Modifier = Modifier
) {
    FlowRow(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        fotos.take(MAX_FOTOS).forEachIndexed { index, uri ->
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // Miniatura de la foto
                AsyncImage(
                    model             = uri,
                    contentDescription = "Foto ${index + 1}",
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier.fillMaxSize()
                )

                // Botón eliminar en la esquina superior derecha
                Box(
                    modifier         = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onEliminarFoto(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = "Eliminar foto",
                        tint               = Color.White,
                        modifier           = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Botón agregar (visible mientras no se alcance el máximo)
        if (fotos.size < MAX_FOTOS) {
            Box(
                modifier         = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onAgregarFoto() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Agregar foto",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(28.dp)
                )
            }
        }
    }
}
