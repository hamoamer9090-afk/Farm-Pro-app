package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    secondary = ColorSecondary,
    error = ColorError,
    background = ColorBackground,
    surface = ColorSurface,
    onPrimary = ColorOnPrimary,
    onSurface = ColorOnSurface,
    onBackground = ColorOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = ColorPrimary,
    secondary = ColorSecondary,
    error = ColorError,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = ColorOnPrimary,
    onSurface = Color.White,
    onBackground = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val colorScheme = if (primaryColor != null) {
        baseColorScheme.copy(
            primary = primaryColor,
            // If primary is custom, we can also customize other matching semantic tones if desired
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
