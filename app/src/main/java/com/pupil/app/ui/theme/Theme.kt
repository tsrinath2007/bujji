package com.pupil.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PupilPrimaryLight,
    onPrimary = PupilOnPrimaryLight,
    primaryContainer = PupilPrimaryContainerLight,
    onPrimaryContainer = PupilOnPrimaryContainerLight,
    secondary = PupilAccentLight,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = PupilWarmBgLight,
    surface = PupilSurfaceLight,
    surfaceVariant = PupilSurfaceVariantLight,
    onBackground = PupilOnSurfaceLight,
    onSurface = PupilOnSurfaceLight,
    onSurfaceVariant = PupilOnSurfaceVariantLight,
    outline = PupilOutlineLight
)

@Composable
fun PupilTheme(
    darkTheme: Boolean = false, // Forced Light theme per spec
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use Clean Notebook LightColorScheme
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
