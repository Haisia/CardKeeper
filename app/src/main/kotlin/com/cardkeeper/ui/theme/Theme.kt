package com.cardkeeper.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color(0xFF0F1B2D),
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Color(0xFFB4CCE8),
    onSecondary = Color(0xFF1B2F42),
    secondaryContainer = Color(0xFF324658),
    onSecondaryContainer = Color(0xFFD0E4F8),
    tertiary = Color(0xFFD7BEE8),
    onTertiary = Color(0xFF3A1D4B),
    tertiaryContainer = Color(0xFF523462),
    onTertiaryContainer = Color(0xFFF3DAFF),
    error = Error,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

@Composable
fun CardKeeperTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Surface.toArgb()
            window.navigationBarColor = Surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
