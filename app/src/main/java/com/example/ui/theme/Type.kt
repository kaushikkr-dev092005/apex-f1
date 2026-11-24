package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/** One system sans family; no font download, bitmap atlas or additional runtime. */
private val base=Typography()
private val sans=FontFamily.SansSerif
val Typography=Typography(
    displayLarge=base.displayLarge.copy(fontFamily=sans),
    displayMedium=base.displayMedium.copy(fontFamily=sans),
    displaySmall=base.displaySmall.copy(fontFamily=sans),
    headlineLarge=base.headlineLarge.copy(fontFamily=sans),
    headlineMedium=base.headlineMedium.copy(fontFamily=sans),
    headlineSmall=base.headlineSmall.copy(fontFamily=sans),
    titleLarge=base.titleLarge.copy(fontFamily=sans),
    titleMedium=base.titleMedium.copy(fontFamily=sans),
    titleSmall=base.titleSmall.copy(fontFamily=sans),
    bodyLarge=base.bodyLarge.copy(fontFamily=sans,fontFeatureSettings="tnum"),
    bodyMedium=base.bodyMedium.copy(fontFamily=sans),
    bodySmall=base.bodySmall.copy(fontFamily=sans),
    labelLarge=base.labelLarge.copy(fontFamily=sans),
    labelMedium=base.labelMedium.copy(fontFamily=sans),
    labelSmall=base.labelSmall.copy(fontFamily=sans)
)
