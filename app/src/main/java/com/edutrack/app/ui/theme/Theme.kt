package com.edutrack.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val EduBlue = Color(0xFF3B82F6)
val EduBlueDark = Color(0xFF2563EB)
val DeepCharcoal = Color(0xFF101827)
val DarkSlate = Color(0xFF1E293B)
val CardDark = Color(0xFF162032)
val BorderDark = Color(0xFF293548)
val MutedTextDark = Color(0xFF94A3B8)
val SuccessGreen = Color(0xFF22C55E)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

private val EduTrackDarkColorScheme = darkColorScheme(
    primary = EduBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D3557),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF60A5FA),
    onSecondary = DeepCharcoal,
    background = DeepCharcoal,
    onBackground = Color(0xFFF8FAFC),
    surface = CardDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkSlate,
    onSurfaceVariant = MutedTextDark,
    outline = BorderDark,
    error = ErrorRed,
    onError = Color.White
)

private val EduTrackLightColorScheme = lightColorScheme(
    primary = EduBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = EduBlue,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = ErrorRed,
    onError = Color.White
)

val EduTrackTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun EduTrackTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EduTrackDarkColorScheme else EduTrackLightColorScheme,
        typography = EduTrackTypography,
        content = content
    )
}
