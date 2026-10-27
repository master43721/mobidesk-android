package com.mobidesk.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val MobiDeskInk = Color(0xFF101923)
private val MobiDeskPanel = Color(0xFF17232F)
private val MobiDeskElevated = Color(0xFF1D2B38)
private val MobiDeskPaper = Color(0xFFF5F7F8)
private val MobiDeskMuted = Color(0xFF9AA7B4)
private val MobiDeskTeal = Color(0xFF20BFA9)
private val MobiDeskTealDark = Color(0xFF168C7D)
private val MobiDeskWarning = Color(0xFFE4AA4F)
private val MobiDeskDanger = Color(0xFFDF7474)

private val DarkColors = darkColorScheme(
    primary = MobiDeskTeal,
    onPrimary = MobiDeskInk,
    primaryContainer = Color(0xFF163C3A),
    onPrimaryContainer = Color(0xFFB8FFF2),
    secondary = Color(0xFF9AB8C8),
    onSecondary = MobiDeskInk,
    background = MobiDeskInk,
    onBackground = MobiDeskPaper,
    surface = MobiDeskPanel,
    onSurface = MobiDeskPaper,
    surfaceVariant = MobiDeskElevated,
    onSurfaceVariant = MobiDeskMuted,
    outline = Color(0xFF405261),
    error = MobiDeskDanger,
    onError = MobiDeskInk
)

private val LightColors = lightColorScheme(
    primary = MobiDeskTealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1F5EF),
    onPrimaryContainer = Color(0xFF0C3A34),
    secondary = Color(0xFF48616D),
    onSecondary = Color.White,
    background = MobiDeskPaper,
    onBackground = MobiDeskInk,
    surface = Color.White,
    onSurface = MobiDeskInk,
    surfaceVariant = Color(0xFFE9EEF1),
    onSurfaceVariant = Color(0xFF52616B),
    outline = Color(0xFF9AA8B0),
    error = Color(0xFFAE3038),
    onError = Color.White
)

@Composable
fun MobiDeskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(
            headlineMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.4).sp
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.2.sp
            )
        ),
        content = content
    )
}
