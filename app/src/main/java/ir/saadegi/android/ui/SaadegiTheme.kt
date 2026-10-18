package ir.saadegi.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Source of truth: saadegi-site's warm ivory and terracotta brand.
 * Never depend on Material defaults for selected navigation surfaces.
 */
object BrandColors {
    val Ivory = Color(0xFFFAF9F5)
    val Paper = Color(0xFFF3F0EA)
    val Ink = Color(0xFF211F1C)
    val TextSecondary = Color(0xFF776F67)
    val Terracotta = Color(0xFFB84A26)
    val WarmOrange = Color(0xFFD97757)
    val Divider = Color(0xFFEAE6DE)
    val Night = Color(0xFF262624)
    val NightSurface = Color(0xFF32312E)
    val NightAccent = Color(0xFFE8A87C)
}

@Composable
fun SaadegiTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) {
        darkColorScheme(
            primary = BrandColors.NightAccent,
            onPrimary = BrandColors.Ink,
            background = BrandColors.Night,
            onBackground = Color(0xFFF4F0E8),
            surface = BrandColors.NightSurface,
            onSurface = Color(0xFFF4F0E8),
            surfaceVariant = Color(0xFF3C3B37),
            onSurfaceVariant = Color(0xFFC8C1B8),
            surfaceContainerLow = Color(0xFF2D2D2A),
            outlineVariant = Color(0xFF54524C),
            secondaryContainer = Color(0xFF54443C),
            onSecondaryContainer = Color(0xFFFFE1D4),
        )
    } else {
        lightColorScheme(
            primary = BrandColors.Terracotta,
            onPrimary = Color.White,
            background = BrandColors.Ivory,
            onBackground = BrandColors.Ink,
            surface = Color.White,
            onSurface = BrandColors.Ink,
            surfaceVariant = BrandColors.Paper,
            onSurfaceVariant = BrandColors.TextSecondary,
            surfaceContainerLow = BrandColors.Paper,
            outlineVariant = BrandColors.Divider,
            secondaryContainer = Color(0xFFF8E7DF),
            onSecondaryContainer = BrandColors.Terracotta,
        )
    }
    MaterialTheme(
        colorScheme = palette,
        typography = Typography(defaultFontFamily = FontFamily.SansSerif),
        content = content,
    )
}
