package com.cui.vocab.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B45B0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEFF),
    onPrimaryContainer = Color(0xFF1B0B50),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7F1EA),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF1F6F43),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7F5E3),
    onTertiaryContainer = Color(0xFF0B5132),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFEDE7F6),
    onSurfaceVariant = Color(0xFF5A5764),
    outline = Color(0xFF8A8494),
    outlineVariant = Color(0xFFD5CFDD),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCBBEFF),
    onPrimary = Color(0xFF2F1E6B),
    primaryContainer = Color(0xFF42308A),
    onPrimaryContainer = Color(0xFFE8DEFF),
    secondary = Color(0xFF7FDDD0),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFC7F1EA),
    tertiary = Color(0xFF8ED8AC),
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF11472C),
    onTertiaryContainer = Color(0xFFD7F5E3),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF2C2933),
    onSurfaceVariant = Color(0xFFC9C4D0),
    outline = Color(0xFF948F9C),
    outlineVariant = Color(0xFF46424E),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun VocabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
