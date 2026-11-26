package ai.tambo.foundation.ngoma.ui

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Charte NGoma : rouge en couleur principale (base NGoma).
object NColors {
    val Brand = Color(0xFFE0262D)
    val BrandDark = Color(0xFFFF5A5F)
    val Brand50 = Color(0xFFFDECEC)
    val Brand100 = Color(0xFFFAD3D4)
    val Brand700 = Color(0xFFB71C22)
    val Bg = Color(0xFFF5F5F7)
    val Ink = Color(0xFF1D1D1F)
    val Secondary = Color(0xFF6E6E73)
    val Grey = Color(0xFFAAAAAA)
    val Hairline = Color(0xFFE5E5EA)
    val Muted = Color(0xFFEEEEF0)
    val Positive = Color(0xFF218039)
    val Negative = Color(0xFFD70015)
    val DarkBg = Color(0xFF121214)
    val DarkSurface = Color(0xFF1C1C1E)
}

private val light: Colors = lightColors(
    primary = NColors.Brand,
    primaryVariant = NColors.Brand700,
    secondary = NColors.Brand,
    background = NColors.Bg,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = NColors.Ink,
    onSurface = NColors.Ink,
    error = NColors.Negative,
)

private val dark: Colors = darkColors(
    primary = NColors.BrandDark,
    primaryVariant = NColors.Brand,
    secondary = NColors.BrandDark,
    background = NColors.DarkBg,
    surface = NColors.DarkSurface,
    onPrimary = Color.White,
)

@Composable
fun NGomaTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colors = if (darkTheme) dark else light, content = content)
}
