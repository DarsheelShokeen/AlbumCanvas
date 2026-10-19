package com.example.albumcanvas.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.graphics.Color as AndroidColor
import com.example.albumcanvas.utils.ImageProcessor

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A), // Muted green primary
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFF2E7D32)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFF1B5E20)
)

private fun createArtworkScheme(
    colors: ImageProcessor.PaletteColors,
    darkTheme: Boolean
): ColorScheme {
    val primaryColor = Color(colors.accent)
    val primaryBg = Color(colors.primary)
    
    return if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = if (isColorDark(colors.accent)) Color.White else Color.Black,
            primaryContainer = Color(colors.darkMuted),
            onPrimaryContainer = Color.White,
            secondary = Color(colors.vibrant),
            onSecondary = Color.Black,
            background = Color(0xFF121212),
            surface = Color(0xFF1A1A1A),
            surfaceVariant = Color(colors.primary).copy(alpha = 0.15f),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.LightGray
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = if (isColorDark(colors.accent)) Color.White else Color.Black,
            primaryContainer = Color(colors.lightVibrant),
            onPrimaryContainer = Color(colors.darkMuted),
            secondary = Color(colors.vibrant),
            onSecondary = Color.White,
            background = Color(0xFFF7F7F7),
            surface = Color.White,
            surfaceVariant = Color(colors.primary).copy(alpha = 0.1f),
            onBackground = Color.Black,
            onSurface = Color.Black,
            onSurfaceVariant = Color.DarkGray
        )
    }
}

private fun isColorDark(color: Int): Boolean {
    val darkness = 1 - (0.299 * AndroidColor.red(color) + 0.587 * AndroidColor.green(color) + 0.114 * AndroidColor.blue(color)) / 255
    return darkness >= 0.5
}

@Composable
fun AlbumCanvasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    artworkColors: ImageProcessor.PaletteColors? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        artworkColors != null -> {
            createArtworkScheme(artworkColors, darkTheme)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
