package com.ipdial.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipdial.data.model.ThemeMode

// Google Dialer Inspired Palette
val GoogleBlue = Color(0xFF1A73E8)
val GoogleBlueDark = Color(0xFF8AB4F8)
val GoogleRed = Color(0xFFEA4335)
val GoogleGreen = Color(0xFF34A853)

val DarkBg = Color(0xFF000000) // Pure Black
val DarkSurface = Color(0xFF121212)
val DarkSurfaceVariant = Color(0xFF1E1E1E)

val LightBg = Color(0xFFFFFFFF) // Pure White
val LightSurface = Color(0xFFF8F9FA)
val LightSurfaceVariant = Color(0xFFF1F3F4)

val EndRed = GoogleRed
val ForestGreen = GoogleGreen

fun Modifier.glass(
    shape: Shape = RoundedCornerShape(16.dp),
    alpha: Float = 0.2f,
    borderWidth: Dp = 1.dp
): Modifier = this

private val DarkColorScheme = darkColorScheme(
    primary = GoogleBlueDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = GoogleBlueDark,
    secondary = Color(0xFF9AA0A6),
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = Color(0xFFE8EAED),
    onSurfaceVariant = Color(0xFF9AA0A6),
    error = GoogleRed
)

private val LightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = GoogleBlue,
    secondary = Color(0xFF5F6368),
    background = LightBg,
    surface = LightBg,
    surfaceVariant = LightSurfaceVariant,
    onSurface = Color(0xFF202124),
    onSurfaceVariant = Color(0xFF5F6368),
    error = GoogleRed
)

enum class GlassMode { None }
val LocalGlassMode = staticCompositionLocalOf { GlassMode.None }

@Composable
fun IPDialTheme(
    themeMode: ThemeMode = ThemeMode.System,
    fontMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Dark, ThemeMode.Obsidian -> true
        ThemeMode.Light, ThemeMode.Quartz -> false
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IPDialTypography, 
        shapes = IPDialShapes,         
        content = {
            CompositionLocalProvider(LocalGlassMode provides GlassMode.None) {
                content()
            }
        }
    )
}
