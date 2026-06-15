package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = DarkGreen,
    secondary = DeepPurple,
    tertiary = YellowGold,
    background = AppBackground,
    surface = AppSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFE8EFEA),
    onSurfaceVariant = DarkGreen
  )

private val DarkColorScheme = LightColorScheme // Force bright/pleasant branding since "tema cerah" is specifically requested

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Force bright mode as explicitly requested
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce the specific brand colors requested (dark green, deep purple, yellow)
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
