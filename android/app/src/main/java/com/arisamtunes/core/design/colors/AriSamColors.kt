package com.arisamtunes.core.design.colors

import androidx.compose.ui.graphics.Color

internal val LightPrimary = Color(0xFF0369A1)
internal val LightPrimaryVariant = Color(0xFF075985)
internal val LightSecondary = Color(0xFF0369A1)
internal val LightSecondaryContainer = Color(0xFFD7F0FE)
internal val LightOnSecondaryContainer = Color(0xFF082F49)
internal val LightTertiary = Color(0xFF6D28D9)
internal val LightTertiaryContainer = Color(0xFFEDE9FE)
internal val LightOnTertiaryContainer = Color(0xFF3B0764)
internal val LightBackground = Color(0xFFF0F9FF)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightTextPrimary = Color(0xFF0C4A6E)
internal val LightTextSecondary = Color(0xFF0369A1)
internal val LightBorder = Color(0xFFBAE6FD)
internal val LightSurfaceVariant = Color(0xFFE0F2FE)
internal val LightSurfaceContainerHighest = Color(0xFFD4EAF5)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)

internal val DarkPrimary = Color(0xFF38BDF8)
internal val DarkOnPrimary = Color(0xFF082F49)
internal val DarkPrimaryVariant = Color(0xFF0369A1)
internal val DarkSecondary = Color(0xFF7DD3FC)
internal val DarkSecondaryContainer = Color(0xFF0C4A6E)
internal val DarkOnSecondaryContainer = Color(0xFFE0F2FE)
internal val DarkTertiary = Color(0xFFC4B5FD)
internal val DarkTertiaryContainer = Color(0xFF4C1D95)
internal val DarkOnTertiaryContainer = Color(0xFFEDE9FE)
internal val DarkBackground = Color(0xFF0C1821)
internal val DarkSurface = Color(0xFF1E2A3A)
internal val DarkTextPrimary = Color(0xFFF0F9FF)
internal val DarkTextSecondary = Color(0xFFBAE6FD)
internal val DarkBorder = Color(0xFF0369A1)
internal val DarkSurfaceVariant = Color(0xFF26364A)
internal val DarkSurfaceContainerHighest = Color(0xFF304454)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnError = Color(0xFF690005)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val TehranAmber = Color(0xFFFFC857)
internal val TehranAmberInk = Color(0xFF332300)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val ErrorLight = Color(0xFFBA1A1A)

/**
 * Central palette for feature artwork, gradients and state accents.
 *
 * Material surfaces and content colors should still come from
 * [androidx.compose.material3.MaterialTheme.colorScheme]. These tokens exist
 * for deliberate artwork colors that are not represented by Material roles.
 * Keeping them here prevents feature composables from owning raw ARGB values.
 */
object AriSamPalette {
    val transparent = Color.Transparent
    val black = Color.Black
    val sky50 = LightBackground
    val sky100 = LightSurfaceVariant
    val violet50 = DarkOnTertiaryContainer
    val errorContainerLight = LightErrorContainer
    val red600 = ErrorLight
    val white = Color(0xFFFFFFFF)
    val yellow300 = Color(0xFFFFE24B)
    val amberAccent = TehranAmber
    val red200 = ErrorDark
    val pink300 = Color(0xFFFF7AAA)
    val roseBright = Color(0xFFFF657A)
    val pink500 = Color(0xFFFF3E9D)
    val amber500 = Color(0xFFFBBF24)
    val rose400 = Color(0xFFFB7185)
    val rose500 = Color(0xFFF43F5E)
    val skyTint = Color(0xFFD9F2FF)
    val purple200 = Color(0xFFD8B4FE)
    val skyContainer = LightSecondaryContainer
    val skySurfaceHigh = LightSurfaceContainerHighest
    val red700 = Color(0xFFD00000)
    val violet300 = DarkTertiary
    val sky200 = LightBorder
    val cyanTint = Color(0xFFB9E8FF)
    val violet400 = Color(0xFFA78BFA)
    val violetGame = Color(0xFF9A7BFF)
    val red900 = DarkErrorContainer
    val skyGlow = Color(0xFF8ED8FF)
    val roseHero = Color(0xFF8E2D57)
    val violet500 = Color(0xFF8B5CF6)
    val fuchsia500 = Color(0xFFA855F7)
    val pink600 = Color(0xFFEC4899)
    val violetAccent = Color(0xFFB57BFF)
    val brandBlue = Color(0xFF0797DB)
    val violetSoft = Color(0xFF8A79FF)
    val sky300 = DarkSecondary
    val indigoSoft = Color(0xFF7C6CF2)
    val purple700 = Color(0xFF7B2CBF)
    val indigoAccent = Color(0xFF765CFF)
    val violet700 = LightTertiary
    val red950 = DarkOnError
    val blue400 = Color(0xFF60A5FA)
    val neutral600 = Color(0xFF595959)
    val cyan400 = Color(0xFF58D3FF)
    val emerald300 = Color(0xFF55E6B5)
    val slateBlue = Color(0xFF537895)
    val indigo700 = Color(0xFF4C36D9)
    val violet900 = DarkTertiaryContainer
    val redInk = LightOnErrorContainer
    val cyan500 = Color(0xFF40D7FF)
    val teal400 = Color(0xFF40D6C9)
    val green600 = Color(0xFF40916C)
    val wine900 = Color(0xFF3D0C11)
    val purpleHero = Color(0xFF3B1E65)
    val violet950 = LightOnTertiaryContainer
    val cyanAccent = Color(0xFF38C6F4)
    val sky400 = DarkPrimary
    val emerald400 = Color(0xFF34D399)
    val amberInk = TehranAmberInk
    val plum800 = Color(0xFF321B42)
    val purple900 = Color(0xFF321450)
    val slate700 = DarkSurfaceContainerHighest
    val violetInk = Color(0xFF2E1065)
    val slateBlue700 = Color(0xFF2C394B)
    val teal700 = Color(0xFF2A6F5A)
    val plum900 = Color(0xFF2A1246)
    val slate800 = DarkSurfaceVariant
    val mintAccent = Color(0xFF24F2C9)
    val purpleBackdrop = Color(0xFF24123A)
    val darkSurface = DarkSurface
    val green900 = Color(0xFF1B4332)
    val navyViolet = Color(0xFF171E46)
    val splashBlue = Color(0xFF16435A)
    val darkRed = Color(0xFF160305)
    val blueGray = Color(0xFF123246)
    val navy700 = Color(0xFF123047)
    val navyVioletDark = Color(0xFF111E42)
    val teal900 = Color(0xFF102C3A)
    val navy900 = Color(0xFF101F32)
    val purpleBlack = Color(0xFF10051F)
    val sky500 = Color(0xFF0EA5E9)
    val sky900 = LightTextPrimary
    val darkBackground = DarkBackground
    val ocean700 = Color(0xFF0A6FA5)
    val cyan900 = Color(0xFF0A3345)
    val splashBlueDark = Color(0xFF0A2838)
    val midnightBlue = Color(0xFF09203F)
    val sky950 = DarkOnPrimary
    val deepBlue = Color(0xFF082032)
    val green950 = Color(0xFF081C15)
    val authInk = Color(0xFF081721)
    val sky800 = LightPrimaryVariant
    val splashInk = Color(0xFF07151E)
    val ink950 = Color(0xFF07141C)
    val suggestionInk = Color(0xFF06141D)
    val profileInk = Color(0xFF06131D)
    val playerBlack = Color(0xFF06060F)
    val tealInk = Color(0xFF062B2A)
    val midnight = Color(0xFF050B18)
    val blackBlue = Color(0xFF05060D)
    val chatInk = Color(0xFF04151D)
    val sky700 = LightPrimary
    val almostBlack = Color(0xFF000814)
    val scrimStrong = Color(0xE804040C)
}
