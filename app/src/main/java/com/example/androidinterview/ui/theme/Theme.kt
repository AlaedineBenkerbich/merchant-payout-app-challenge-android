package com.example.androidinterview.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Every role a component may read is set explicitly: any role left out falls back to Material's
// default purple palette, which would show up on tonal buttons, the navigation bar and dialogs.
private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF00344A),
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = Color(0xFFD6F0FA),
    secondary = BrandBlueDark,
    onSecondary = Color(0xFF00344A),
    secondaryContainer = BrandBlueContainerDark,
    onSecondaryContainer = Color(0xFFD6F0FA),
    background = ScreenBackgroundDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = Color(0xFF2B3036),
    onSurfaceVariant = InkMutedDark,
    surfaceContainerLowest = ScreenBackgroundDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF242A31),
    surfaceContainerHighest = Color(0xFF2B3036),
    outline = Color(0xFF8B95A1),
    outlineVariant = Color(0xFF3A424B),
    error = DebitRedDark,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = BrandBlueOnContainer,
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = BrandBlueContainer,
    onSecondaryContainer = BrandBlueOnContainer,
    background = ScreenBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE5E7EB),
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE5E7EB),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFFD1D5DB),
    error = DebitRed,
)

/** Colours with a financial meaning that Material's scheme has no slot for. */
@Immutable
class AppColors(val credit: Color, val debit: Color)

private val LightAppColors = AppColors(credit = CreditGreen, debit = DebitRed)
private val DarkAppColors = AppColors(credit = CreditGreenDark, debit = DebitRedDark)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

@Composable
fun AndroidInterviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app keeps its own palette instead of following the wallpaper.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
