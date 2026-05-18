package com.example.toycity.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = Indigo40,
    onPrimary            = Color.White,
    primaryContainer     = Indigo90,
    onPrimaryContainer   = Indigo10,

    secondary            = Violet40,
    onSecondary          = Color.White,
    secondaryContainer   = Violet90,
    onSecondaryContainer = Violet10,

    tertiary             = Emerald40,
    onTertiary           = Color.White,
    tertiaryContainer    = Emerald90,
    onTertiaryContainer  = Emerald10,

    error                = Crimson40,
    onError              = Color.White,
    errorContainer       = Crimson90,
    onErrorContainer     = Crimson10,

    background           = NeutralBackground,
    onBackground         = Color(0xFF1C1B2E),

    surface              = NeutralSurface,
    onSurface            = Color(0xFF1C1B2E),
    surfaceVariant       = NeutralVariant95,
    onSurfaceVariant     = NeutralVariant40,

    outline              = Color(0xFF7A7591),
    outlineVariant       = NeutralVariant90,

    inverseSurface       = Color(0xFF312F45),
    inverseOnSurface     = Color(0xFFF4F0FF),
    inversePrimary       = Indigo80,

    scrim                = Color(0xFF000000),
    surfaceTint          = Indigo40,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Indigo80,
    onPrimary            = Indigo10,
    primaryContainer     = Indigo30,
    onPrimaryContainer   = Indigo90,

    secondary            = Violet70,
    onSecondary          = Violet10,
    secondaryContainer   = Violet30,
    onSecondaryContainer = Violet90,

    tertiary             = Emerald70,
    onTertiary           = Emerald10,
    tertiaryContainer    = Emerald20,
    onTertiaryContainer  = Emerald90,

    error                = Crimson70,
    onError              = Crimson10,
    errorContainer       = Crimson20,
    onErrorContainer     = Crimson90,

    background           = DarkBackground,
    onBackground         = Color(0xFFE8E4FF),

    surface              = DarkSurface,
    onSurface            = Color(0xFFE8E4FF),
    surfaceVariant       = DarkSurfaceVar,
    onSurfaceVariant     = NeutralVariant80,

    outline              = Color(0xFF948FB0),
    outlineVariant       = DarkOutlineVar,

    inverseSurface       = Color(0xFFE8E4FF),
    inverseOnSurface     = Color(0xFF312F45),
    inversePrimary       = Indigo40,

    scrim                = Color(0xFF000000),
    surfaceTint          = Indigo80,
)

@Composable
fun ToyCityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
