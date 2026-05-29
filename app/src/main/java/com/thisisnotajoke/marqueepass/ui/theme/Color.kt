package com.thisisnotajoke.marqueepass.ui.theme

import androidx.compose.ui.graphics.Color

val Obsidian = Color(0xFF09090B) // Even deeper black
val PlaybillYellow = Color(0xFFFFCC00) // Classic Broadway yellow
val NeonPink = Color(0xFFFF0055) // Bright pink for accents
val NeonCyan = Color(0xFF00E5FF) // Electric blue
val NeonPurple = Color(0xFF8A2BE2) // Deep marquee purple

// Dark Scheme
val MarqueePrimaryDark = PlaybillYellow
val MarqueeOnPrimaryDark = Color.Black
val MarqueeSecondaryDark = NeonPink
val MarqueeOnSecondaryDark = Color.White
val MarqueeTertiaryDark = NeonCyan
val MarqueeOnTertiaryDark = Color.Black
val MarqueeBackgroundDark = Obsidian
val MarqueeOnBackgroundDark = Color.White
val MarqueeSurfaceDark = Color(0xFF16161A) // Slightly elevated dark
val MarqueeOnSurfaceDark = Color.White

// Light Scheme (Fallback/Alternative - forced dark mode usually used)
val MarqueePrimaryLight = PlaybillYellow
val MarqueeOnPrimaryLight = Color.Black
val MarqueeSecondaryLight = NeonPink
val MarqueeOnSecondaryLight = Color.White
val MarqueeTertiaryLight = NeonCyan
val MarqueeOnTertiaryLight = Color.Black
val MarqueeBackgroundLight = Color(0xFFFFFBFE)
val MarqueeOnBackgroundLight = Obsidian
val MarqueeSurfaceLight = Color(0xFFFFFBFE)
val MarqueeOnSurfaceLight = Obsidian

val AmbientDeepViolet = Color(0xFF1C0A33) // Deeper violet for marquee glow
val AmbientSlateBlue = Color(0xFF0D1B2A)
val TicketCardTop = Color(0xFF22222A) // More pronounced ticket card contrast
val TicketCardBottom = Color(0xFF141419)
