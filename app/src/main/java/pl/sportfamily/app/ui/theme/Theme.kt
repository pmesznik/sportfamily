package pl.sportfamily.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.sportfamily.app.data.Category

/** Kolor kategorii w trzech odcieniach: akcent, tło (container) i tekst na tle. */
@Immutable
data class Tone(val main: Color, val container: Color, val onContainer: Color)

@Immutable
data class SfColors(
    val sport: Tone,
    val school: Tone,
    val important: Tone,
    val extra: Tone,
    val family: Tone,
    val error: Tone,
    val neutral: Tone,
    /** Kolor marki (granat w jasnym, pomarańcz w ciemnym): puste stany, aktualizacje, osoby. */
    val brand: Tone,
    /** Pomarańcz z logo dla najważniejszej akcji (przycisk Dodaj) i treść na nim. */
    val accent: Color,
    val onAccent: Color,
    val cardShadow: Boolean,
    val dark: Boolean = !cardShadow,
) {
    fun of(c: Category): Tone = when (c) {
        Category.SPORT -> sport
        Category.SCHOOL -> school
        Category.IMPORTANT -> important
        Category.EXTRA -> extra
        Category.FAMILY -> family
    }
}

private val LightSf = SfColors(
    sport = Tone(Color(0xFF3479C7), Color(0xFFE3EDF9), Color(0xFF245A97)),
    school = Tone(Color(0xFF29966B), Color(0xFFE0F2EA), Color(0xFF1D7151)),
    important = Tone(Color(0xFFE99A3C), Color(0xFFFCEEDC), Color(0xFF9A5B12)),
    extra = Tone(Color(0xFF8A70C7), Color(0xFFEEE9F8), Color(0xFF634BA0)),
    family = Tone(Color(0xFFD16B8A), Color(0xFFFBE8EE), Color(0xFF9C3F5C)),
    error = Tone(Color(0xFFC9433B), Color(0xFFFBE6E4), Color(0xFF9E2E27)),
    neutral = Tone(Color(0xFF5B6075), Color(0xFFEFEDE8), Color(0xFF141B3D)),
    brand = Tone(Color(0xFF141B3D), Color(0xFFE6E8F2), Color(0xFF141B3D)),
    accent = Color(0xFFFF6A1F),
    onAccent = Color(0xFF141B3D),
    cardShadow = true,
)

private val DarkSf = SfColors(
    sport = Tone(Color(0xFF7FB2EC), Color(0xFF2C3E5C), Color(0xFFA9CCF4)),
    school = Tone(Color(0xFF5CC79B), Color(0xFF27443C), Color(0xFF8EDDBB)),
    important = Tone(Color(0xFFF2B66E), Color(0xFF4A3C2B), Color(0xFFF6CC95)),
    extra = Tone(Color(0xFFB6A2E8), Color(0xFF3B3657), Color(0xFFCFC2F1)),
    family = Tone(Color(0xFFEE9DB6), Color(0xFF4A3040), Color(0xFFF5C0D0)),
    error = Tone(Color(0xFFF08A82), Color(0xFF4C3036), Color(0xFFF6AFA9)),
    neutral = Tone(Color(0xFFAEB3C9), Color(0xFF232B4E), Color(0xFFF3F1EC)),
    brand = Tone(Color(0xFFFF7A35), Color(0xFF4A2C1E), Color(0xFFFFC2A0)),
    accent = Color(0xFFFF7A35),
    onAccent = Color(0xFF141B3D),
    cardShadow = false,
)

// Paleta marki z logo: granat #141B3D, pomarańcz #FF6A1F, krem #FFF5E8.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF141B3D), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E8F2), onPrimaryContainer = Color(0xFF141B3D),
    secondary = Color(0xFFFF6A1F), onSecondary = Color(0xFF141B3D),
    tertiary = Color(0xFFFF6A1F), onTertiary = Color(0xFF141B3D),
    background = Color(0xFFF7F4EF), onBackground = Color(0xFF141B3D),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF141B3D),
    surfaceVariant = Color(0xFFEFEDE8), onSurfaceVariant = Color(0xFF5B6075),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F4EF), surfaceContainerHighest = Color(0xFFEFEDE8),
    outline = Color(0xFFC9C5BC), outlineVariant = Color(0xFFE4E0D8),
    error = Color(0xFFC9433B), errorContainer = Color(0xFFFBE6E4), onErrorContainer = Color(0xFF9E2E27),
    secondaryContainer = Color(0xFFE6E8F2), onSecondaryContainer = Color(0xFF141B3D),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF7A35), onPrimary = Color(0xFF141B3D),
    primaryContainer = Color(0xFF4A2C1E), onPrimaryContainer = Color(0xFFFFC2A0),
    secondary = Color(0xFFFF7A35), onSecondary = Color(0xFF141B3D),
    tertiary = Color(0xFFFF7A35), onTertiary = Color(0xFF141B3D),
    background = Color(0xFF0E1330), onBackground = Color(0xFFF3F1EC),
    surface = Color(0xFF182040), onSurface = Color(0xFFF3F1EC),
    surfaceVariant = Color(0xFF232B4E), onSurfaceVariant = Color(0xFFAEB3C9),
    surfaceContainer = Color(0xFF182040), surfaceContainerHigh = Color(0xFF1D2547),
    surfaceContainerLow = Color(0xFF131A38), surfaceContainerHighest = Color(0xFF232B4E),
    outline = Color(0xFF4A5274), outlineVariant = Color(0xFF2E3659),
    error = Color(0xFFF08A82), errorContainer = Color(0xFF4C3036), onErrorContainer = Color(0xFFF6AFA9),
    secondaryContainer = Color(0xFF2B3460), onSecondaryContainer = Color(0xFFE6E8F2),
)

private val tnum = "tnum"

val SfTypography = Typography(
    displayMedium = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, fontFeatureSettings = tnum),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = tnum),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
)

val SfShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalSf = staticCompositionLocalOf { LightSf }

/** Kolory dzieci do wyboru w profilu. */
val ChildPalette = listOf(0xFF1F9EA8, 0xFFE4572E, 0xFFD9A21B, 0xFFC2417A, 0xFF6B8F2B, 0xFF3D4F9F, 0xFF8A70C7, 0xFF2E86DE)
    .map { it.toInt() }

@Composable
fun SportFamilyTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSf provides if (dark) DarkSf else LightSf) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = SfTypography,
            shapes = SfShapes,
            content = content,
        )
    }
}

object Sf {
    val colors: SfColors @Composable get() = LocalSf.current

    /** Logo dopasowane do aktywnego motywu aplikacji (nie systemu). */
    val logoHorizontal: Int @Composable get() = if (LocalSf.current.dark) pl.sportfamily.app.R.drawable.logo_horizontal_dark else pl.sportfamily.app.R.drawable.logo_horizontal
    val logoSymbol: Int @Composable get() = if (LocalSf.current.dark) pl.sportfamily.app.R.drawable.logo_symbol_dark else pl.sportfamily.app.R.drawable.logo_symbol
}
