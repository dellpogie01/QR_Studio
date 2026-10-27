package com.juanmediahubph.qrstudio.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.juanmediahubph.qrstudio.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6B57),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA6F2DA),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4B635B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEE9DE),
    onSecondaryContainer = Color(0xFF072019),
    tertiary = Color(0xFF3F6375),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC2E8FD),
    onTertiaryContainer = Color(0xFF001E2B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF7FBF8),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBEC9C3),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2C322F),
    inverseOnSurface = Color(0xFFEDF1EE),
    inversePrimary = Color(0xFF89D6BE),
    surfaceDim = Color(0xFFD7DBD8),
    surfaceBright = Color(0xFFF7FBF8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F2),
    surfaceContainer = Color(0xFFEBEFEC),
    surfaceContainerHigh = Color(0xFFE5E9E6),
    surfaceContainerHighest = Color(0xFFDFE4E1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D6BE),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF00513F),
    onPrimaryContainer = Color(0xFFA6F2DA),
    secondary = Color(0xFFB2CCC2),
    onSecondary = Color(0xFF1D352D),
    secondaryContainer = Color(0xFF334B43),
    onSecondaryContainer = Color(0xFFCEE9DE),
    tertiary = Color(0xFFA6CCE1),
    onTertiary = Color(0xFF073544),
    tertiaryContainer = Color(0xFF254C5C),
    onTertiaryContainer = Color(0xFFC2E8FD),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBEC9C3),
    outline = Color(0xFF88938E),
    outlineVariant = Color(0xFF3F4945),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFDEE4E1),
    inverseOnSurface = Color(0xFF2C322F),
    inversePrimary = Color(0xFF0B6B57),
    surfaceDim = Color(0xFF0E1513),
    surfaceBright = Color(0xFF343A38),
    surfaceContainerLowest = Color(0xFF090F0D),
    surfaceContainerLow = Color(0xFF171D1B),
    surfaceContainer = Color(0xFF1B211F),
    surfaceContainerHigh = Color(0xFF252B29),
    surfaceContainerHighest = Color(0xFF303634),
)

private val QrStudioTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    )
}

@Composable
fun QrStudioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QrStudioTypography,
        content = content,
    )
}
