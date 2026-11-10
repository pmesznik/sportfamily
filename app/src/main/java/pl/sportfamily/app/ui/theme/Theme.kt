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
    val cardShadow: Boolean,
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
    neutral = Tone(Color(0xFF5A6478), Color(0xFFEEF1F6), Color(0xFF1B2230)),
    cardShadow = true,
)

private val DarkSf = SfColors(
    sport = Tone(Color(0xFF7FB2EC), Color(0xFF2C3E5C), Color(0xFFA9CCF4)),
    school = Tone(Color(0xFF5CC79B), Color(0xFF27443C), Color(0xFF8EDDBB)),
    important = Tone(Color(0xFFF2B66E), Color(0xFF4A3C2B), Color(0xFFF6CC95)),
    extra = Tone(Color(0xFFB6A2E8), Color(0xFF3B3657), Color(0xFFCFC2F1)),
    family = Tone(Color(0xFFEE9DB6), Color(0xFF4A3040), Color(0xFFF5C0D0)),
    error = Tone(Color(0xFFF08A82), Color(0xFF4C3036), Color(0xFFF6AFA9)),
    neutral = Tone(Color(0xFFAEB7C9), Color(0xFF343C50), Color(0xFFEEF1F7)),
    cardShadow = false,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3479C7), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EDF9), onPrimaryContainer = Color(0xFF245A97),
    secondary = Color(0xFF29966B), onSecondary = Color.White,
    background = Color(0xFFF5F7FA), onBackground = Color(0xFF1B2230),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1B2230),
    surfaceVariant = Color(0xFFEEF1F6), onSurfaceVariant = Color(0xFF5A6478),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F7FA), surfaceContainerHighest = Color(0xFFEEF1F6),
    outline = Color(0xFFC3CAD6), outlineVariant = Color(0xFFDCE1EA),
    error = Color(0xFFC9433B), errorContainer = Color(0xFFFBE6E4), onErrorContainer = Color(0xFF9E2E27),
    secondaryContainer = Color(0xFFE3EDF9), onSecondaryContainer = Color(0xFF245A97),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7FB2EC), onPrimary = Color(0xFF10223A),
    primaryContainer = Color(0xFF2C3E5C), onPrimaryContainer = Color(0xFFA9CCF4),
    secondary = Color(0xFF5CC79B), onSecondary = Color(0xFF0E2A1F),
    background = Color(0xFF202633), onBackground = Color(0xFFEEF1F7),
    surface = Color(0xFF2A3142), onSurface = Color(0xFFEEF1F7),
    surfaceVariant = Color(0xFF343C50), onSurfaceVariant = Color(0xFFAEB7C9),
    surfaceContainer = Color(0xFF2A3142), surfaceContainerHigh = Color(0xFF2F3649),
    surfaceContainerLow = Color(0xFF242A38), surfaceContainerHighest = Color(0xFF343C50),
    outline = Color(0xFF55607A), outlineVariant = Color(0xFF414A60),
    error = Color(0xFFF08A82), errorContainer = Color(0xFF4C3036), onErrorContainer = Color(0xFFF6AFA9),
    secondaryContainer = Color(0xFF2C3E5C), onSecondaryContainer = Color(0xFFA9CCF4),
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
}
