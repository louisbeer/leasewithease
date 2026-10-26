package com.beer.leasewithease.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import androidx.glance.material3.ColorProviders
import com.beer.leasewithease.theme.TechBlue200
import com.beer.leasewithease.theme.TechBlue500
import com.beer.leasewithease.theme.TechBlue700

private val WidgetLightColorScheme = lightColorScheme(
    primary = TechBlue500,
    onPrimary = Color.White,
    secondary = TechBlue700,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val WidgetDarkColorScheme = darkColorScheme(
    primary = TechBlue200,
    onPrimary = Color.Black,
    secondary = TechBlue700,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)

val LeaseWithEaseWidgetColors = ColorProviders(
    light = WidgetLightColorScheme,
    dark = WidgetDarkColorScheme
)

// Semantic diff colors — same in light and dark
val WidgetGreenColor = ColorProvider(Color(0xFF00C853))
val WidgetRedColor = ColorProvider(Color(0xFFD50000))
