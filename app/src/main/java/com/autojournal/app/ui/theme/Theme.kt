package com.autojournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class AppStyle(val title: String, val description: String) {
    GARAGE("Гараж", "Графит и оранжевый, как приборная панель"),
    PREMIUM("Премиум", "Тёплый белый, чёрные акценты"),
    NEON("Неон", "Ночной город, светящиеся линии"),
    CLASSIC("Классика", "Синяя, светлая или тёмная по системе"),
}

/** Цвета и эффекты, которых нет в стандартной схеме Material. */
data class AppExtras(
    val style: AppStyle,
    val isDark: Boolean,
    val heroColors: List<Color>,
    val heroText: Color,
    val heroAccent: Color,
    /** Фон приложения: градиент для «Неона», иначе null (обычный цвет фона). */
    val backgroundBrush: Brush?,
    val ok: Color,
    val soon: Color,
    val overdue: Color,
    val off: Color,
    val gaugeTrack: Color,
    val glow: Boolean,
    val cardBorder: Color?,
)

val LocalExtras = staticCompositionLocalOf { extrasFor(AppStyle.CLASSIC, false) }

/** Совместимость со старым кодом. */
object StatusColors {
    val overdue = Color(0xFFD93025)
    val soon = Color(0xFFE08A00)
    val ok = Color(0xFF2E9D46)
    val off = Color(0xFF8A94A3)
}

private val ClassicLight = lightColorScheme(
    primary = Color(0xFF1558B0), onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF), onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFFD9730D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB8), onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFF2E7D32), onTertiary = Color.White,
    background = Color(0xFFF7F9FC), surface = Color(0xFFF7F9FC),
    surfaceVariant = Color(0xFFE1E6EF), surfaceContainer = Color(0xFFEDF1F7),
    surfaceContainerHigh = Color(0xFFE7ECF3), surfaceContainerLow = Color(0xFFF2F5FA),
)

private val ClassicDark = darkColorScheme(
    primary = Color(0xFFA9C7FF), onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF00468C), onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFFFB86E), onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3B00), onSecondaryContainer = Color(0xFFFFDDB8),
    tertiary = Color(0xFF81C784), background = Color(0xFF111318), surface = Color(0xFF111318),
)

private val Garage = darkColorScheme(
    primary = Color(0xFFFF7A1A), onPrimary = Color(0xFF1A0D00),
    primaryContainer = Color(0xFF3A2414), onPrimaryContainer = Color(0xFFFFB98A),
    secondary = Color(0xFFFFB547), onSecondary = Color(0xFF241500),
    secondaryContainer = Color(0xFF3A2414), onSecondaryContainer = Color(0xFFFFB98A),
    tertiary = Color(0xFF4CC38A), onTertiary = Color(0xFF00210F),
    background = Color(0xFF14161A), onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF14161A), onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF2A2E36), onSurfaceVariant = Color(0xFFA3AAB5),
    surfaceContainerLowest = Color(0xFF0F1114), surfaceContainerLow = Color(0xFF1A1C21),
    surfaceContainer = Color(0xFF1F2228), surfaceContainerHigh = Color(0xFF262A31),
    surfaceContainerHighest = Color(0xFF2E323A),
    outline = Color(0xFF4A505A), outlineVariant = Color(0xFF30353D),
    error = Color(0xFFFF5A4E),
)

private val Premium = lightColorScheme(
    primary = Color(0xFF16181C), onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE6DD), onPrimaryContainer = Color(0xFF16181C),
    secondary = Color(0xFFD9730D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E1D6), onSecondaryContainer = Color(0xFF16181C),
    tertiary = Color(0xFF2E8A4A), onTertiary = Color.White,
    background = Color(0xFFF6F4EF), onBackground = Color(0xFF16181C),
    surface = Color(0xFFF6F4EF), onSurface = Color(0xFF16181C),
    surfaceVariant = Color(0xFFECE8DF), onSurfaceVariant = Color(0xFF6B6A66),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFBFAF7),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFF1EEE7),
    surfaceContainerHighest = Color(0xFFEAE6DD),
    outline = Color(0xFFB9B4A9), outlineVariant = Color(0xFFE3DED3),
    error = Color(0xFFC2352A),
)

private val Neon = darkColorScheme(
    primary = Color(0xFF3CF0FF), onPrimary = Color(0xFF001F24),
    primaryContainer = Color(0xFF12324A), onPrimaryContainer = Color(0xFF9FF8FF),
    secondary = Color(0xFFFF3FA4), onSecondary = Color(0xFF2B0016),
    secondaryContainer = Color(0xFF12324A), onSecondaryContainer = Color(0xFF9FF8FF),
    tertiary = Color(0xFFFFC23C), onTertiary = Color(0xFF261A00),
    background = Color(0xFF0A0C1E), onBackground = Color(0xFFEAF2FF),
    surface = Color(0xFF0A0C1E), onSurface = Color(0xFFEAF2FF),
    surfaceVariant = Color(0xFF1F2456), onSurfaceVariant = Color(0xFF9AA6D1),
    surfaceContainerLowest = Color(0xFF070918), surfaceContainerLow = Color(0xFF10132E),
    surfaceContainer = Color(0xFF14173A), surfaceContainerHigh = Color(0xFF1B1F4A),
    surfaceContainerHighest = Color(0xFF232858),
    outline = Color(0xFF3B4380), outlineVariant = Color(0xFF262B5C),
    error = Color(0xFFFF3FA4),
)

fun extrasFor(style: AppStyle, systemDark: Boolean): AppExtras = when (style) {
    AppStyle.GARAGE -> AppExtras(
        style, true,
        heroColors = listOf(Color(0xFF2C3038), Color(0xFF16181C)),
        heroText = Color.White, heroAccent = Color(0xFFFF7A1A), backgroundBrush = null,
        ok = Color(0xFF4CC38A), soon = Color(0xFFFF9F1A), overdue = Color(0xFFFF5A4E), off = Color(0xFF6B7280),
        gaugeTrack = Color(0xFF2C3038), glow = false, cardBorder = null,
    )
    AppStyle.PREMIUM -> AppExtras(
        style, false,
        heroColors = listOf(Color(0xFF1E2126), Color(0xFF101114)),
        heroText = Color.White, heroAccent = Color(0xFFE9E3D6), backgroundBrush = null,
        ok = Color(0xFF2E8A4A), soon = Color(0xFFD9730D), overdue = Color(0xFFC2352A), off = Color(0xFF9A968D),
        gaugeTrack = Color(0xFFEDE9E0), glow = false, cardBorder = null,
    )
    AppStyle.NEON -> AppExtras(
        style, true,
        heroColors = listOf(Color(0xFF232A70), Color(0xFF11143A)),
        heroText = Color(0xFFEAF2FF), heroAccent = Color(0xFF3CF0FF),
        backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF1A1F55), Color(0xFF0A0C1E), Color(0xFF0A0C1E))),
        ok = Color(0xFF3CF0FF), soon = Color(0xFFFFC23C), overdue = Color(0xFFFF3FA4), off = Color(0xFF5A6290),
        gaugeTrack = Color(0x1AFFFFFF), glow = true, cardBorder = Color(0x26FFFFFF),
    )
    AppStyle.CLASSIC -> AppExtras(
        style, systemDark,
        heroColors = listOf(Color(0xFF2A7DE1), Color(0xFF0D3F86)),
        heroText = Color.White, heroAccent = Color(0xFFFFC531), backgroundBrush = null,
        ok = StatusColors.ok, soon = StatusColors.soon, overdue = StatusColors.overdue, off = StatusColors.off,
        gaugeTrack = if (systemDark) Color(0xFF2A2F38) else Color(0xFFE1E6EF), glow = false, cardBorder = null,
    )
}

private fun schemeFor(style: AppStyle, systemDark: Boolean): ColorScheme = when (style) {
    AppStyle.GARAGE -> Garage
    AppStyle.PREMIUM -> Premium
    AppStyle.NEON -> Neon
    AppStyle.CLASSIC -> if (systemDark) ClassicDark else ClassicLight
}

private fun typography(): Typography {
    val base = Typography()
    return base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.2.sp),
    )
}

/** Крупные цифры (пробег, суммы). */
val BigNumber = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp)

@Composable
fun AutoJournalTheme(style: AppStyle = AppStyle.CLASSIC, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalExtras provides extrasFor(style, dark)) {
        MaterialTheme(
            colorScheme = schemeFor(style, dark),
            typography = typography(),
            content = content,
        )
    }
}
