package com.twinhearts.app.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R

/** Inter (body/UI) is one variable font file; each weight picks its own variation (R-51). */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter_var,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val InterFamily = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
    inter(FontWeight.ExtraBold),
)

/** DM Serif Display: titles, big numbers and the italic accent word (R-51). */
val SerifFamily = FontFamily(
    Font(R.font.dm_serif_display_regular, FontWeight.Normal),
    Font(R.font.dm_serif_display_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Serif titles + sans body, as in the web app. Features use only these styles. */
val TwinTypography = Typography(
    displayLarge = TextStyle(fontFamily = SerifFamily, fontSize = 58.sp, lineHeight = 58.sp),
    displayMedium = TextStyle(fontFamily = SerifFamily, fontSize = 46.sp, lineHeight = 46.sp),
    displaySmall = TextStyle(fontFamily = SerifFamily, fontSize = 34.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = SerifFamily, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = SerifFamily, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = SerifFamily, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = SerifFamily, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.14.em),
    labelMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.3.em),
)
