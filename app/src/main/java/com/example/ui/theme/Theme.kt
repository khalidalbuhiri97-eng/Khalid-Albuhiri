package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SouqPrimaryDark,
    secondary = SouqSecondaryDark,
    tertiary = SouqTertiaryDark,
    background = SouqBackgroundDark,
    surface = SouqSurfaceDark,
    onPrimary = SouqOnPrimaryDark,
    onSecondary = SouqOnSecondaryDark,
    onBackground = SouqOnBackgroundDark,
    onSurface = SouqOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = SouqPrimary,
    secondary = SouqSecondary,
    tertiary = SouqTertiary,
    background = SouqBackground,
    surface = SouqSurface,
    onPrimary = SouqOnPrimary,
    onSecondary = SouqOnSecondary,
    onBackground = SouqOnBackground,
    onSurface = SouqOnSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep brand-specific theme by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
