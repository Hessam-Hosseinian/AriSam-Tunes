package com.arisamtunes.core.design.spacing

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class AriSamSpacing(
    val none: androidx.compose.ui.unit.Dp = 0.dp,
    val xxs: androidx.compose.ui.unit.Dp = 2.dp,
    val xs: androidx.compose.ui.unit.Dp = 4.dp,
    val sm: androidx.compose.ui.unit.Dp = 8.dp,
    val md: androidx.compose.ui.unit.Dp = 12.dp,
    val lg: androidx.compose.ui.unit.Dp = 16.dp,
    val xl: androidx.compose.ui.unit.Dp = 24.dp,
    val xxl: androidx.compose.ui.unit.Dp = 32.dp,
    val huge: androidx.compose.ui.unit.Dp = 48.dp,
)

val LocalAriSamSpacing = staticCompositionLocalOf { AriSamSpacing() }

/**
 * Exact geometry tokens for bespoke artwork and motion layouts.
 *
 * Feature code should prefer [AriSamSpacing] for ordinary padding and gaps.
 * These named dimensions preserve the carefully tuned player, chat and game
 * geometry without leaving raw `dp` values scattered through composables.
 */
object AriSamDimensions {
    val negative88 = -88.dp
    val negative78 = -78.dp
    val negative72 = -72.dp
    val negative64 = -64.dp
    val negative58 = -58.dp
    val negative48 = -48.dp
    val negative22 = -22.dp
    val negative2 = -2.dp
    val dp0 = 0.dp
    val dp1 = 1.dp
    val dp1_1 = 1.1.dp
    val dp1_5 = 1.5.dp
    val dp2 = 2.dp
    val dp2_6 = 2.6.dp
    val dp3 = 3.dp
    val dp4 = 4.dp
    val dp5 = 5.dp
    val dp6 = 6.dp
    val dp7 = 7.dp
    val dp8 = 8.dp
    val dp9 = 9.dp
    val dp10 = 10.dp
    val dp11 = 11.dp
    val dp12 = 12.dp
    val dp13 = 13.dp
    val dp14 = 14.dp
    val dp15 = 15.dp
    val dp16 = 16.dp
    val dp17 = 17.dp
    val dp18 = 18.dp
    val dp19 = 19.dp
    val dp20 = 20.dp
    val dp21 = 21.dp
    val dp22 = 22.dp
    val dp23 = 23.dp
    val dp24 = 24.dp
    val dp25 = 25.dp
    val dp26 = 26.dp
    val dp27 = 27.dp
    val dp28 = 28.dp
    val dp30 = 30.dp
    val dp32 = 32.dp
    val dp34 = 34.dp
    val dp36 = 36.dp
    val dp38 = 38.dp
    val dp40 = 40.dp
    val dp42 = 42.dp
    val dp44 = 44.dp
    val dp46 = 46.dp
    val dp48 = 48.dp
    val dp50 = 50.dp
    val dp51 = 51.dp
    val dp52 = 52.dp
    val dp54 = 54.dp
    val dp55 = 55.dp
    val dp56 = 56.dp
    val dp57 = 57.dp
    val dp58 = 58.dp
    val dp60 = 60.dp
    val dp61 = 61.dp
    val dp62 = 62.dp
    val dp64 = 64.dp
    val dp66 = 66.dp
    val dp68 = 68.dp
    val dp69 = 69.dp
    val dp70 = 70.dp
    val dp72 = 72.dp
    val dp76 = 76.dp
    val dp78 = 78.dp
    val dp80 = 80.dp
    val dp82 = 82.dp
    val dp86 = 86.dp
    val dp88 = 88.dp
    val dp92 = 92.dp
    val dp94 = 94.dp
    val dp96 = 96.dp
    val dp104 = 104.dp
    val dp105 = 105.dp
    val dp112 = 112.dp
    val dp116 = 116.dp
    val dp122 = 122.dp
    val dp126 = 126.dp
    val dp132 = 132.dp
    val dp138 = 138.dp
    val dp140 = 140.dp
    val dp148 = 148.dp
    val dp154 = 154.dp
    val dp156 = 156.dp
    val dp160 = 160.dp
    val dp164 = 164.dp
    val dp170 = 170.dp
    val dp176 = 176.dp
    val dp178 = 178.dp
    val dp180 = 180.dp
    val dp190 = 190.dp
    val dp210 = 210.dp
    val dp220 = 220.dp
    val dp228 = 228.dp
    val dp230 = 230.dp
    val dp238 = 238.dp
    val dp240 = 240.dp
    val dp245 = 245.dp
    val dp248 = 248.dp
    val dp250 = 250.dp
    val dp252 = 252.dp
    val dp274 = 274.dp
    val dp286 = 286.dp
    val dp290 = 290.dp
    val dp296 = 296.dp
    val dp300 = 300.dp
    val dp320 = 320.dp
    val dp330 = 330.dp
    val dp336 = 336.dp
    val dp360 = 360.dp
    val dp362 = 362.dp
    val dp448 = 448.dp
}
