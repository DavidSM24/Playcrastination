package com.playcrastination.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PlayColors {
    val Background = Color(0xFF0B0F19)
    val Navigation = Color(0xFF111827)
    val Surface = Color(0xFF1B2435)
    val Panel = Color(0xFF141B2A)
    val Violet = Color(0xFF8B5CF6)
    val LightViolet = Color(0xFFC4B0FF)
    val Cyan = Color(0xFF22D3EE)
    val Text = Color(0xFFF1F5F9)
    val Muted = Color(0xFF94A3B8)
    val Line = Color(0xFF303B52)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFF87171)
}

object PlaySpacing {
    val Inset = 20.dp
    val Gap = 12.dp
    val Section = 28.dp
    val CardRadius = 20.dp
    val ContentWidth = 820.dp
}

const val ScreenTransitionMillis = 220
const val ProgressAnimationMillis = 540

private val playScheme = darkColorScheme(
    primary = PlayColors.LightViolet,
    onPrimary = Color(0xFF211246),
    primaryContainer = Color(0xFF38275F),
    onPrimaryContainer = PlayColors.Text,
    secondary = PlayColors.Cyan,
    background = PlayColors.Background,
    onBackground = PlayColors.Text,
    surface = PlayColors.Navigation,
    onSurface = PlayColors.Text,
    surfaceVariant = PlayColors.Surface,
    onSurfaceVariant = PlayColors.Muted,
    outline = PlayColors.Line,
    error = PlayColors.Error
)

private val playTypography = Typography(
    displaySmall = TextStyle(fontSize = 40.sp, lineHeight = 43.sp, letterSpacing = (-1.8).sp,
        fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif),
    headlineMedium = TextStyle(fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.9).sp,
        fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 27.sp, letterSpacing = (-0.35).sp,
        fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
)

@Composable
fun PlaycrastinationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = playScheme,
        typography = playTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(PlaySpacing.CardRadius)
        ),
        content = content
    )
}
