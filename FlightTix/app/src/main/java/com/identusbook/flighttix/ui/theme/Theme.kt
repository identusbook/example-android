package com.identusbook.flighttix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// FlightTix accent — the iOS system blue.
val AccentBlue = Color(0xFF0A84FF)

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentBlue
)

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentBlue
)

@Composable
fun FlightTixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
