package com.gauthier.affut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    // Distincts du fond (Sand) : sinon une puce sélectionnée devient invisible en thème clair.
    primaryContainer = SageLight,
    onPrimaryContainer = ForestGreenDark,
    secondary = Bark,
    onSecondary = Color.White,
    secondaryContainer = TanLight,
    onSecondaryContainer = Bark,
    background = Sand,
    onBackground = ForestGreenDark,
    surface = Sand,
    onSurface = ForestGreenDark,
    error = AlertOrange,
    onError = Color.White,
    errorContainer = AlertOrangeContainer,
    onErrorContainer = Sand,
)

// Rôles explicitement définis (pas seulement primary/secondary/background/error) : sinon les rôles
// non précisés (onPrimary, primaryContainer, secondaryContainer...) retombent sur le violet par défaut
// de Material3, incohérent avec la palette forêt et parfois peu lisible sur fond sombre.
private val DarkColors = darkColorScheme(
    primary = ForestGreenLight,
    onPrimary = ForestGreenDark,
    primaryContainer = ForestGreen,
    onPrimaryContainer = ForestGreenLight,
    secondary = BarkLight,
    onSecondary = ForestGreenDark,
    secondaryContainer = Bark,
    onSecondaryContainer = BarkLight,
    background = ForestGreenDark,
    onBackground = OnDarkSurface,
    surface = ForestGreenDark,
    onSurface = OnDarkSurface,
    error = AlertOrange,
    onError = ForestGreenDark,
    errorContainer = AlertOrangeContainer,
    onErrorContainer = Sand,
)

@Composable
fun AffutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AffutTypography,
        content = content,
    )
}
