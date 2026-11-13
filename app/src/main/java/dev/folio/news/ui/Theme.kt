package dev.folio.news.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFFD92B1F), onPrimary = Color.White,
    background = Color(0xFFF6F3EE), onBackground = Color(0xFF161616),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF161616),
    surfaceVariant = Color(0xFFEAE5DC), onSurfaceVariant = Color(0xFF5E5A52),
    outline = Color(0xFFCFC9BE),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFFF5A4D), onPrimary = Color(0xFF1A0503),
    background = Color(0xFF0D0D0E), onBackground = Color(0xFFEDEBE6),
    surface = Color(0xFF1A1A1C), onSurface = Color(0xFFEDEBE6),
    surfaceVariant = Color(0xFF26262A), onSurfaceVariant = Color(0xFFA9A59D),
    outline = Color(0xFF3A3A3F),
)

@Composable
fun FolioTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}

@Composable
fun isDark(mode: dev.folio.news.data.ThemeMode): Boolean = when (mode) {
    dev.folio.news.data.ThemeMode.System -> isSystemInDarkTheme()
    dev.folio.news.data.ThemeMode.Light -> false
    dev.folio.news.data.ThemeMode.Dark -> true
}
