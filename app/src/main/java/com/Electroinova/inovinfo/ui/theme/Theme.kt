package com.Electroinova.inovinfo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val InovInfoColorScheme = lightColorScheme(
    primary            = ElectroNavyBlue,
    onPrimary          = ElectroOnPrimary,
    primaryContainer   = ElectroNavyBlueLight,
    onPrimaryContainer = ElectroOnPrimary,

    secondary            = ElectroGold,
    onSecondary          = ElectroOnSecondary,
    secondaryContainer   = ElectroGoldLight,
    onSecondaryContainer = ElectroOnSecondary,

    background  = ElectroBackground,
    onBackground = ElectroOnBackground,

    surface         = ElectroSurface,
    onSurface       = ElectroOnSurface,
    surfaceVariant  = ElectroSurface,
    onSurfaceVariant = ElectroOnSurfaceVar,

    error   = ElectroError,
    onError = ElectroOnError,
)

@Composable
fun InovInfoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InovInfoColorScheme,
        typography  = Typography,
        content     = content
    )
}
