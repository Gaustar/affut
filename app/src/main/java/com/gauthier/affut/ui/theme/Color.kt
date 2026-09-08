package com.gauthier.affut.ui.theme

import androidx.compose.ui.graphics.Color

// Palette sobre inspirée nature/forêt — à affiner plus tard si besoin.
val ForestGreen = Color(0xFF2E4B2E)
val ForestGreenDark = Color(0xFF16290F)
// Vert clair utilisé comme "primary" en thème sombre : ForestGreen seul est trop foncé pour
// servir de couleur de texte/icône lisible sur fond sombre (contraste ~1.6:1, très insuffisant).
val ForestGreenLight = Color(0xFF8FCE86)
val Bark = Color(0xFF5A4632)
// Ton clair de Bark, même rôle que ForestGreenLight mais pour les accents secondaires (chips sélectionnées...).
val BarkLight = Color(0xFFD9BFA2)
val Sand = Color(0xFFE8E0D0)
// Teintes de conteneurs pour le thème clair, volontairement plus soutenues que Sand.
val SageLight = Color(0xFFC7D8BC)
val TanLight = Color(0xFFDCC3A0)
val AlertOrange = Color(0xFFD98E32)
val AlertOrangeContainer = Color(0xFF5C3B14)
// Texte/icônes clairs sur fond sombre, teinté vert plutôt qu'un blanc pur pour rester dans la palette.
val OnDarkSurface = Color(0xFFE8F0E4)
