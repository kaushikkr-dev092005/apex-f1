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

private val DarkColorScheme =
  darkColorScheme(
    primary = F1Red,
    onPrimary = Color(0xFF23080D),
    secondary = F1Green,
    onSecondary = Color.Black,
    tertiary = F1Yellow,
    background = F1CarbonDark,
    onBackground = Color.White,
    surface = F1GridSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF251F28),
    onSurfaceVariant = Color(0xFFA9A3AD),
    outline = Color(0xFF6E6772),
    outlineVariant = Color(0xFF302A33)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFFC62036),
    onPrimary = Color.White,
    secondary = F1Red,
    onSecondary = Color.White,
    tertiary = F1Green,
    background = Color(0xFFF5F1F2),
    onBackground = F1TextPrimaryLight,
    surface = Color.White,
    onSurface = F1TextPrimaryLight,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = F1TextSecondaryLight,
    outline = Color(0xFF726974),
    outlineVariant = Color(0xFFDFD7DD)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to maintain official team brand colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
