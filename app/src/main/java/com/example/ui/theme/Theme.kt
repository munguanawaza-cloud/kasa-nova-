package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KasaDarkColorScheme = darkColorScheme(
    primary = SovereignGold,
    onPrimary = Color(0xFF1A1403),
    primaryContainer = SovereignGoldContainer,
    onPrimaryContainer = SovereignGoldOnContainer,
    secondary = UsdEmerald,
    onSecondary = Color(0xFF032311),
    secondaryContainer = UsdEmeraldContainer,
    onSecondaryContainer = Color(0xFFBBF7D0),
    tertiary = CdfCobalt,
    onTertiary = Color(0xFF042235),
    tertiaryContainer = CdfCobaltContainer,
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = ForestBg,
    onBackground = TextPrimaryDark,
    surface = ForestSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ForestSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ForestSurface,
    surfaceContainer = ForestSurfaceHigh,
    surfaceContainerHigh = ForestSurfaceHighest,
    outline = ForestOutline,
    error = OverdueCrimson,
    onError = Color.White,
    errorContainer = OverdueCrimsonContainer,
    onErrorContainer = Color(0xFFFECACA)
)

private val KasaLightColorScheme = lightColorScheme(
    primary = SovereignGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF3E2C04),
    secondary = UsdEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF14532D),
    tertiary = CdfCobaltDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = IvoryBg,
    onBackground = TextPrimaryLight,
    surface = IvorySurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = IvorySurface,
    surfaceContainer = IvorySurfaceVariant,
    surfaceContainerHigh = Color(0xFFD6E4DD),
    outline = IvoryOutline,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val KasaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KasaNovaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KasaDarkColorScheme else KasaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = KasaShapes,
        content = content
    )
}
