package com.smartcalc.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = NeutralSurface,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = AccentTeal,
    onSecondary = NeutralSurface,
    secondaryContainer = AccentTealLight,
    onSecondaryContainer = Color0A3D39,
    background = NeutralBg,
    onBackground = Color111318,
    surface = NeutralSurface,
    onSurface = Color111318,
    surfaceVariant = NeutralBg,
    onSurfaceVariant = Color4A4F5C,
    outline = NeutralOutline,
    error = ErrorRed,
    onError = NeutralSurface,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = BrandBlue,
    onPrimary = NeutralSurface,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = BrandBlueLight,
    secondary = AccentTeal,
    onSecondary = Color111318,
    secondaryContainer = Color0A3D39,
    onSecondaryContainer = AccentTealLight,
    background = DarkBg,
    onBackground = ColorE6E8EF,
    surface = DarkSurface,
    onSurface = ColorE6E8EF,
    surfaceVariant = DarkOutline,
    onSurfaceVariant = ColorA8AEBF,
    outline = DarkOutline,
    error = ErrorRed,
    onError = NeutralSurface,
    errorContainer = Color5A1A14,
    onErrorContainer = ErrorRedLight
)

@Composable
fun SmartCalcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SmartCalcTypography,
        content = content
    )
}
