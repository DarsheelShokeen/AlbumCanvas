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
    primary = Color(0xFFBA68C8), // Radiant Violet
    secondary = Color(0xFF29B6F6), // Electric Cyan Blue
    tertiary = Color(0xFFAB47BC),
    background = Color(0xFF080A0F), // True OLED black
    surface = Color(0xFF10131C), // Deep glass surface
    surfaceVariant = Color(0xFF181D2A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8E24AA),
    secondary = Color(0xFF1E88E5),
    tertiary = Color(0xFF6A1B9A),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0)
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
