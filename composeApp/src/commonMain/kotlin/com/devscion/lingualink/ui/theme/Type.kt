package com.devscion.lingualink.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Stand-ins for the design's Space Grotesk + JetBrains Mono families.
// FontFamily.Default maps to a clean geometric sans on every platform; swap in
// real Space Grotesk later by registering the font in composeResources.
val SansFamily = FontFamily.Default
val MonoFamily = FontFamily.Monospace

val TranscriptFontFamily = MonoFamily

val LinguaLinkTypography = Typography(
    displaySmall   = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, letterSpacing = (-0.6).sp),
    headlineLarge  = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    headlineSmall  = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-0.2).sp),
    titleLarge     = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.1).sp),
    titleMedium    = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    titleSmall     = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Medium,   fontSize = 13.sp),
    bodyLarge      = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium     = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall      = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Medium,   fontSize = 13.sp),
    labelMedium    = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 1.4.sp),
    labelSmall     = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium,   fontSize = 10.sp, letterSpacing = 1.6.sp),
)
