package com.example.androidpowerinfer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightPrimary = Color(0xFF128C7E)
private val LightSecondary = Color(0xFF25D366)
private val LightBackground = Color(0xFFECE5DD)
private val LightSurface = Color.White
private val LightOnPrimary = Color.White
private val LightOnSecondary = Color.White
private val LightOnBackground = Color.Black
private val LightOnSurface = Color.Black

private val DarkPrimary = Color(0xFF075E54)
private val DarkSecondary = Color(0xFF128C7E)
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1F1F1F)
private val DarkOnPrimary = Color.White
private val DarkOnSecondary = Color.White
private val DarkOnBackground = Color.White
private val DarkOnSurface = Color.White

private val WhatsAppLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
)

private val WhatsAppDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
)

@Composable
fun LlamaAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WhatsAppDarkColorScheme else WhatsAppLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
