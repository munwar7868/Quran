package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.ReadingThemeMode

private val PureLightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = Gray100,
    onPrimaryContainer = PureBlack,
    secondary = Gray800,
    onSecondary = PureWhite,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray800,
    outline = Gray300,
    outlineVariant = Gray200
)

private val PureDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = Gray900,
    onPrimaryContainer = PureWhite,
    secondary = Gray300,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = Gray950,
    onSurface = PureWhite,
    surfaceVariant = Gray900,
    onSurfaceVariant = Gray300,
    outline = Gray700,
    outlineVariant = Gray800
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaText,
    onPrimary = SepiaBackground,
    primaryContainer = SepiaSurfaceVariant,
    onPrimaryContainer = SepiaText,
    secondary = SepiaTextSecondary,
    onSecondary = SepiaBackground,
    background = SepiaBackground,
    onBackground = SepiaText,
    surface = SepiaSurface,
    onSurface = SepiaText,
    surfaceVariant = SepiaSurfaceVariant,
    onSurfaceVariant = SepiaTextSecondary,
    outline = SepiaBorder,
    outlineVariant = SepiaBorder
)

private val NightDimColorScheme = darkColorScheme(
    primary = NightDimText,
    onPrimary = NightDimBackground,
    primaryContainer = NightDimSurface,
    onPrimaryContainer = NightDimText,
    secondary = NightDimTextSecondary,
    onSecondary = NightDimBackground,
    background = NightDimBackground,
    onBackground = NightDimText,
    surface = NightDimSurface,
    onSurface = NightDimText,
    surfaceVariant = Color(0xFF24242A),
    onSurfaceVariant = NightDimTextSecondary,
    outline = NightDimBorder,
    outlineVariant = NightDimBorder
)

@Composable
fun QuranAppTheme(
    readingTheme: ReadingThemeMode = ReadingThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (readingTheme) {
        ReadingThemeMode.LIGHT -> PureLightColorScheme
        ReadingThemeMode.DARK -> PureDarkColorScheme
        ReadingThemeMode.SEPIA -> SepiaColorScheme
        ReadingThemeMode.NIGHT_DIM -> NightDimColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuranTypography,
        content = content
    )
}
