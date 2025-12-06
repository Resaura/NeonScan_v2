package com.neonscan.app.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFF00D8FF),
    onPrimary = Color(0xFF003948),
    background = Color(0xFF00213B),
    surface = Color(0xFF0A2F46),
    onSurface = Color(0xFFE6F2FF),
    secondary = Color(0xFF5B9AC8),
    onSecondary = Color(0xFF00213B),
    outline = Color(0xFF0F3B57)
)

private val LightColorPalette = lightColorScheme(
    primary = Color(0xFF00D8FF),
    onPrimary = Color(0xFF003948),
    background = Color(0xFF00213B),
    surface = Color(0xFF0A2F46),
    onSurface = Color(0xFFE6F2FF),
    secondary = Color(0xFF5B9AC8),
    onSecondary = Color(0xFF00213B),
    outline = Color(0xFF0F3B57)
)

@Composable
fun NeonScanTheme(
    darkTheme: Boolean = true,
    colorScheme: ColorScheme = if (darkTheme || isSystemInDarkTheme()) DarkColorPalette else LightColorPalette,
    content: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(color = colorScheme.background, darkIcons = false)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = Shapes(),
        content = content
    )
}
