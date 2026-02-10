package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.example.myapplication.ui.UiColors

private val AppDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = UiColors.AppBg,
    surface = UiColors.SurfaceBg,
    surfaceVariant = UiColors.SurfaceBg,
    onPrimary = UiColors.TextPrimary,
    onSecondary = UiColors.TextPrimary,
    onTertiary = UiColors.TextPrimary,
    onBackground = UiColors.TextPrimary,
    onSurface = UiColors.TextPrimary,
    onSurfaceVariant = UiColors.TextPrimary
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = Typography,
        content = content
    )
}
